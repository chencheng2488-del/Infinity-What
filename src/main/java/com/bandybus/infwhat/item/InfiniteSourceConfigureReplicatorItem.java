package com.bandybus.infwhat.item;

import com.bandybus.infwhat.block.RedstoneMode;
import com.bandybus.infwhat.block.entity.InfItemGeneratorBlockEntity;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Sneak-use copies a generator configuration; regular use pastes it onto a matching generator.
 *
 * <p>1.14.3 ships only the item generator, so only item cards can be saved or pasted there.</p>
 */
public class InfiniteSourceConfigureReplicatorItem extends Item {
    private static final String TAG_CONFIGURATION = "Configuration";
    private static final String KEY_MODE = "PasteMode";
    private static final String KEY_TYPE = "Type";
    private static final String KEY_OUTPUT_ENABLED = "OutputEnabled";
    private static final String KEY_OUTPUT_SIDES = "OutputSides";
    private static final String KEY_REDSTONE_MODE = "RedstoneMode";
    private static final String KEY_PULSE_ON = "PulseOnTicks";
    private static final String KEY_PULSE_OFF = "PulseOffTicks";
    private static final String KEY_BATCH_SIZE = "BatchSize";
    /** Legacy single-rate key, only read from cards created before 2.2. */
    private static final String KEY_OUTPUT_RATE = "OutputRate";
    private static final String KEY_INFINITE_MODE = "InfiniteMode";
    private static final String[] SIDE_RATE_KEYS = createSideRateKeys();

    public InfiniteSourceConfigureReplicatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public ActionResultType onItemUse(ItemUseContext context) {
        World world = context.getWorld();
        TileEntity target = world.getTileEntity(context.getPos());
        boolean itemTarget = target instanceof InfItemGeneratorBlockEntity;
        if (!itemTarget) {
            // Sneak-use on a non-generator block must not fall through to onItemRightClick,
            // which is reserved for clearing the card in air.
            return context.getPlayer() != null && context.getPlayer().isSneaking()
                    ? ActionResultType.SUCCESS : ActionResultType.PASS;
        }

        if (world.isRemote) {
            return ActionResultType.SUCCESS;
        }
        if (context.getPlayer() == null) {
            return ActionResultType.PASS;
        }
        ItemStack card = context.getItem();
        if (context.getPlayer().isSneaking()) {
            copyConfiguration(card, target);
            show(context, "message.infwhat.replicator.copied", TextFormatting.GREEN);
            return ActionResultType.SUCCESS;
        }

        CompoundNBT root = card.getTag();
        if (root == null || !root.contains(TAG_CONFIGURATION, Constants.NBT.TAG_COMPOUND)) {
            show(context, "message.infwhat.replicator.empty", TextFormatting.RED);
            return ActionResultType.SUCCESS;
        }
        CompoundNBT config = root.getCompound(TAG_CONFIGURATION);
        if (!config.getString(KEY_TYPE).equals("item")) {
            show(context, "message.infwhat.replicator.type_mismatch", TextFormatting.RED);
            return ActionResultType.SUCCESS;
        }

        boolean outputEnabled = config.getBoolean(KEY_OUTPUT_ENABLED);
        int sideMask = config.getInt(KEY_OUTPUT_SIDES);
        ReplicatorPasteMode pasteMode = getPasteMode(card);
        InfItemGeneratorBlockEntity generator = (InfItemGeneratorBlockEntity) target;
        boolean applied;
        switch (pasteMode) {
            case ALL:
                applied = generator.applyFullConfiguration(outputEnabled, sideMask,
                        readItemRates(config), config.getBoolean(KEY_INFINITE_MODE))
                        && generator.setBatchSize(readBatch(config));
                break;
            case OUTPUT:
                applied = generator.applyOutputState(outputEnabled, sideMask);
                break;
            case RATE:
                applied = generator.applyRates(readItemRates(config));
                break;
            case REDSTONE:
                applied = generator.applyRedstoneConfiguration(
                        RedstoneMode.byId(config.getInt(KEY_REDSTONE_MODE)),
                        readPulse(config, KEY_PULSE_ON), readPulse(config, KEY_PULSE_OFF));
                break;
            case BATCH:
            default:
                applied = generator.setBatchSize(readBatch(config));
                break;
        }
        if (!applied) {
            show(context, "message.infwhat.replicator.rejected", TextFormatting.RED);
            return ActionResultType.SUCCESS;
        }
        show(context, "message.infwhat.replicator.pasted_mode", TextFormatting.GREEN,
                modeName(pasteMode));
        return ActionResultType.SUCCESS;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        ItemStack card = player.getHeldItem(hand);
        if (!player.isSneaking()) {
            // Regular air use cycles the paste mode; sneak-air keeps its 2.2 clear action.
            // Ignore the fall-through use that follows a normal block interaction.
            // A miss still returns a BlockRayTraceResult, so the hit type must be checked instead.
            RayTraceResult hit = player.pick(5.0D, 0.0F, false);
            if (hit != null && hit.getType() != RayTraceResult.Type.MISS) {
                return new ActionResult<>(ActionResultType.PASS, card);
            }
            if (world.isRemote) {
                return new ActionResult<>(ActionResultType.SUCCESS, card);
            }
            ReplicatorPasteMode mode = getPasteMode(card).next();
            card.getOrCreateTag().putInt(KEY_MODE, mode.ordinal());
            show(player, "message.infwhat.replicator.mode", TextFormatting.AQUA, modeName(mode));
            return new ActionResult<>(ActionResultType.SUCCESS, card);
        }
        if (world.isRemote) {
            return new ActionResult<>(ActionResultType.SUCCESS, card);
        }
        CompoundNBT root = card.getTag();
        if (root == null || !root.contains(TAG_CONFIGURATION, Constants.NBT.TAG_COMPOUND)) {
            show(player, "message.infwhat.replicator.empty", TextFormatting.RED);
            return new ActionResult<>(ActionResultType.SUCCESS, card);
        }
        root.remove(TAG_CONFIGURATION);
        card.setTag(root);
        show(player, "message.infwhat.replicator.cleared", TextFormatting.GREEN);
        return new ActionResult<>(ActionResultType.SUCCESS, card);
    }

    private static ReplicatorPasteMode getPasteMode(ItemStack card) {
        CompoundNBT root = card.getTag();
        return root == null ? ReplicatorPasteMode.ALL
                : ReplicatorPasteMode.byId(root.getInt(KEY_MODE));
    }

    private static ITextComponent modeName(ReplicatorPasteMode mode) {
        return new TranslationTextComponent(
                "item.infwhat.infinite_source_config_replicator.mode." + mode.key());
    }

    private static int readPulse(CompoundNBT config, String key) {
        if (!config.contains(key, Constants.NBT.TAG_INT)) {
            return InfItemGeneratorBlockEntity.DEFAULT_PULSE_TICKS;
        }
        return Math.max(InfItemGeneratorBlockEntity.MIN_PULSE_TICKS,
                Math.min(InfItemGeneratorBlockEntity.MAX_PULSE_TICKS, config.getInt(key)));
    }

    private static int readBatch(CompoundNBT config) {
        int size = config.contains(KEY_BATCH_SIZE, Constants.NBT.TAG_INT)
                ? config.getInt(KEY_BATCH_SIZE) : InfItemGeneratorBlockEntity.DEFAULT_BATCH_SIZE;
        return Math.max(InfItemGeneratorBlockEntity.MIN_BATCH_SIZE,
                Math.min(InfItemGeneratorBlockEntity.MAX_BATCH_SIZE, size));
    }

    private static int[] readItemRates(CompoundNBT config) {
        int[] rates = new int[Direction.values().length];
        boolean hasSideRates = config.contains(SIDE_RATE_KEYS[0], Constants.NBT.TAG_INT);
        int legacyRate = Math.max(1, config.contains(KEY_OUTPUT_RATE, Constants.NBT.TAG_INT)
                ? config.getInt(KEY_OUTPUT_RATE) : 1);
        for (Direction direction : Direction.values()) {
            int index = direction.getIndex();
            rates[index] = hasSideRates
                    ? (config.contains(SIDE_RATE_KEYS[index], Constants.NBT.TAG_INT)
                            ? config.getInt(SIDE_RATE_KEYS[index]) : legacyRate)
                    : legacyRate;
        }
        return rates;
    }

    private static void copyConfiguration(ItemStack card, TileEntity target) {
        CompoundNBT config = new CompoundNBT();
        InfItemGeneratorBlockEntity generator = (InfItemGeneratorBlockEntity) target;
        config.putString(KEY_TYPE, "item");
        config.putBoolean(KEY_OUTPUT_ENABLED, generator.isOutputEnabled());
        config.putInt(KEY_OUTPUT_SIDES, generator.getOutputSideMask());
        config.putBoolean(KEY_INFINITE_MODE, generator.isInfiniteModeEnabled());
        config.putInt(KEY_REDSTONE_MODE, generator.getRedstoneMode().ordinal());
        config.putInt(KEY_PULSE_ON, generator.getPulseOnTicks());
        config.putInt(KEY_PULSE_OFF, generator.getPulseOffTicks());
        config.putInt(KEY_BATCH_SIZE, generator.getBatchSize());
        for (Direction direction : Direction.values()) {
            config.putInt(SIDE_RATE_KEYS[direction.getIndex()],
                    generator.getSideRate(direction));
        }
        card.getOrCreateTag().put(TAG_CONFIGURATION, config);
    }

    private static String[] createSideRateKeys() {
        Direction[] directions = Direction.values();
        String[] keys = new String[directions.length];
        for (Direction direction : directions) {
            String name = direction.getName2();
            keys[direction.getIndex()] = "SideRate"
                    + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
        return keys;
    }

    private static void show(ItemUseContext context, String translationKey, TextFormatting color) {
        context.getPlayer().sendStatusMessage(
                new TranslationTextComponent(translationKey).applyTextStyle(color), true);
    }

    private static void show(ItemUseContext context, String translationKey, TextFormatting color,
                             ITextComponent argument) {
        context.getPlayer().sendStatusMessage(
                new TranslationTextComponent(translationKey, argument).applyTextStyle(color), true);
    }

    private static void show(PlayerEntity player, String translationKey, TextFormatting color) {
        player.sendStatusMessage(
                new TranslationTextComponent(translationKey).applyTextStyle(color), true);
    }

    private static void show(PlayerEntity player, String translationKey, TextFormatting color,
                             ITextComponent argument) {
        player.sendStatusMessage(
                new TranslationTextComponent(translationKey, argument).applyTextStyle(color), true);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
                               List<ITextComponent> tooltip, ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);
        CompoundNBT root = stack.getTag();
        if (root != null && root.contains(TAG_CONFIGURATION, Constants.NBT.TAG_COMPOUND)) {
            String type = root.getCompound(TAG_CONFIGURATION).getString(KEY_TYPE);
            tooltip.add(new TranslationTextComponent("item.infwhat.infinite_source_config_replicator.saved",
                    new TranslationTextComponent("item.infwhat.infinite_source_config_replicator.type." + type))
                    .applyTextStyle(TextFormatting.GRAY));
        } else {
            tooltip.add(new TranslationTextComponent("item.infwhat.infinite_source_config_replicator.empty")
                    .applyTextStyle(TextFormatting.GRAY));
        }
        ReplicatorPasteMode mode = getPasteMode(stack);
        tooltip.add(new TranslationTextComponent("item.infwhat.infinite_source_config_replicator.mode",
                        modeName(mode))
                .applyTextStyle(TextFormatting.AQUA));
        tooltip.add(new TranslationTextComponent("item.infwhat.infinite_source_config_replicator.fields",
                        new TranslationTextComponent("item.infwhat.infinite_source_config_replicator.fields."
                                + mode.key()))
                .applyTextStyle(TextFormatting.DARK_GRAY));
        tooltip.add(new TranslationTextComponent("item.infwhat.infinite_source_config_replicator.instructions")
                .applyTextStyle(TextFormatting.DARK_GRAY));
    }
}
