package com.bandybus.infwhat.item;

import com.bandybus.infwhat.block.RedstoneMode;
import com.bandybus.infwhat.block.entity.InfFluidGeneratorBlockEntity;
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
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.List;

/** Sneak-use copies a generator configuration; regular use pastes it onto a matching generator. */
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
    public ActionResultType useOn(ItemUseContext context) {
        World level = context.getLevel();
        TileEntity target = level.getBlockEntity(context.getClickedPos());
        boolean itemTarget = target instanceof InfItemGeneratorBlockEntity;
        boolean fluidTarget = target instanceof InfFluidGeneratorBlockEntity;
        if (!itemTarget && !fluidTarget) {
            // Sneak-use on a non-generator block must not fall through to use(World, Player, Hand),
            // which is reserved for clearing the card in air.
            return context.getPlayer() != null && context.getPlayer().isShiftKeyDown()
                    ? ActionResultType.CONSUME : ActionResultType.PASS;
        }

        if (level.isClientSide) {
            return ActionResultType.SUCCESS;
        }
        if (context.getPlayer() == null) {
            return ActionResultType.PASS;
        }
        ItemStack card = context.getItemInHand();
        if (context.getPlayer().isShiftKeyDown()) {
            copyConfiguration(card, target, itemTarget);
            show(context, "message.infwhat.replicator.copied", TextFormatting.GREEN);
            return ActionResultType.CONSUME;
        }

        CompoundNBT root = card.getTag();
        if (root == null || !root.contains(TAG_CONFIGURATION, Constants.NBT.TAG_COMPOUND)) {
            show(context, "message.infwhat.replicator.empty", TextFormatting.RED);
            return ActionResultType.CONSUME;
        }
        CompoundNBT config = root.getCompound(TAG_CONFIGURATION);
        String expectedType = itemTarget ? "item" : "fluid";
        if (!config.getString(KEY_TYPE).equals(expectedType)) {
            show(context, "message.infwhat.replicator.type_mismatch", TextFormatting.RED);
            return ActionResultType.CONSUME;
        }

        boolean outputEnabled = config.getBoolean(KEY_OUTPUT_ENABLED);
        int sideMask = config.getInt(KEY_OUTPUT_SIDES);
        ReplicatorPasteMode pasteMode = getPasteMode(card);
        boolean applied;
        if (itemTarget) {
            InfItemGeneratorBlockEntity generator = (InfItemGeneratorBlockEntity) target;
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
        } else {
            InfFluidGeneratorBlockEntity generator = (InfFluidGeneratorBlockEntity) target;
            switch (pasteMode) {
                case ALL:
                    applied = generator.applyFullConfiguration(outputEnabled, sideMask,
                            readFluidRates(config))
                            && generator.setBatchSize(readBatch(config));
                    break;
                case OUTPUT:
                    applied = generator.applyOutputState(outputEnabled, sideMask);
                    break;
                case RATE:
                    applied = generator.applyRates(readFluidRates(config));
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
        }
        if (!applied) {
            show(context, "message.infwhat.replicator.rejected", TextFormatting.RED);
            return ActionResultType.CONSUME;
        }
        show(context, "message.infwhat.replicator.pasted_mode", TextFormatting.GREEN,
                modeName(pasteMode));
        return ActionResultType.CONSUME;
    }

    @Override
    public ActionResult<ItemStack> use(World level, PlayerEntity player, Hand hand) {
        ItemStack card = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            // Regular air use cycles the paste mode; sneak-air keeps its 2.2 clear action.
            // Ignore the fall-through use() that follows a normal block interaction.
            // A miss still returns a BlockRayTraceResult, so the hit type must be checked instead.
            RayTraceResult hit = player.pick(5.0D, 0.0F, false);
            if (hit != null && hit.getType() != RayTraceResult.Type.MISS) {
                return ActionResult.pass(card);
            }
            if (level.isClientSide) {
                return ActionResult.success(card);
            }
            ReplicatorPasteMode mode = getPasteMode(card).next();
            card.getOrCreateTag().putInt(KEY_MODE, mode.ordinal());
            show(player, "message.infwhat.replicator.mode", TextFormatting.AQUA, modeName(mode));
            return ActionResult.consume(card);
        }
        if (level.isClientSide) {
            return ActionResult.success(card);
        }
        CompoundNBT root = card.getTag();
        if (root == null || !root.contains(TAG_CONFIGURATION, Constants.NBT.TAG_COMPOUND)) {
            show(player, "message.infwhat.replicator.empty", TextFormatting.RED);
            return ActionResult.consume(card);
        }
        root.remove(TAG_CONFIGURATION);
        card.setTag(root);
        show(player, "message.infwhat.replicator.cleared", TextFormatting.GREEN);
        return ActionResult.consume(card);
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
            int index = direction.get3DDataValue();
            rates[index] = hasSideRates
                    ? (config.contains(SIDE_RATE_KEYS[index], Constants.NBT.TAG_INT)
                            ? config.getInt(SIDE_RATE_KEYS[index]) : legacyRate)
                    : legacyRate;
        }
        return rates;
    }

    private static long[] readFluidRates(CompoundNBT config) {
        long[] rates = new long[Direction.values().length];
        boolean hasSideRates = config.contains(SIDE_RATE_KEYS[0], Constants.NBT.TAG_LONG);
        long legacyRate = Math.max(1L, config.contains(KEY_OUTPUT_RATE, Constants.NBT.TAG_LONG)
                ? config.getLong(KEY_OUTPUT_RATE) : 1L);
        for (Direction direction : Direction.values()) {
            int index = direction.get3DDataValue();
            rates[index] = hasSideRates
                    ? (config.contains(SIDE_RATE_KEYS[index], Constants.NBT.TAG_LONG)
                            ? config.getLong(SIDE_RATE_KEYS[index]) : legacyRate)
                    : legacyRate;
        }
        return rates;
    }

    private static void copyConfiguration(ItemStack card, TileEntity target, boolean itemTarget) {
        CompoundNBT config = new CompoundNBT();
        if (itemTarget) {
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
                config.putInt(SIDE_RATE_KEYS[direction.get3DDataValue()],
                        generator.getSideRate(direction));
            }
        } else {
            InfFluidGeneratorBlockEntity generator = (InfFluidGeneratorBlockEntity) target;
            config.putString(KEY_TYPE, "fluid");
            config.putBoolean(KEY_OUTPUT_ENABLED, generator.isOutputEnabled());
            config.putInt(KEY_OUTPUT_SIDES, generator.getOutputSideMask());
            config.putInt(KEY_REDSTONE_MODE, generator.getRedstoneMode().ordinal());
            config.putInt(KEY_PULSE_ON, generator.getPulseOnTicks());
            config.putInt(KEY_PULSE_OFF, generator.getPulseOffTicks());
            config.putInt(KEY_BATCH_SIZE, generator.getBatchSize());
            for (Direction direction : Direction.values()) {
                config.putLong(SIDE_RATE_KEYS[direction.get3DDataValue()],
                        generator.getSideRateMb(direction));
            }
        }
        card.getOrCreateTag().put(TAG_CONFIGURATION, config);
    }

    private static String[] createSideRateKeys() {
        Direction[] directions = Direction.values();
        String[] keys = new String[directions.length];
        for (Direction direction : directions) {
            String name = direction.getName();
            keys[direction.get3DDataValue()] = "SideRate"
                    + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
        return keys;
    }

    private static void show(ItemUseContext context, String translationKey, TextFormatting color) {
        context.getPlayer().displayClientMessage(
                new TranslationTextComponent(translationKey).withStyle(color), true);
    }

    private static void show(ItemUseContext context, String translationKey, TextFormatting color,
                             ITextComponent argument) {
        context.getPlayer().displayClientMessage(
                new TranslationTextComponent(translationKey, argument).withStyle(color), true);
    }

    private static void show(PlayerEntity player, String translationKey, TextFormatting color) {
        player.displayClientMessage(
                new TranslationTextComponent(translationKey).withStyle(color), true);
    }

    private static void show(PlayerEntity player, String translationKey, TextFormatting color,
                             ITextComponent argument) {
        player.displayClientMessage(
                new TranslationTextComponent(translationKey, argument).withStyle(color), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World level,
                                List<ITextComponent> tooltip, ITooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        CompoundNBT root = stack.getTag();
        if (root != null && root.contains(TAG_CONFIGURATION, Constants.NBT.TAG_COMPOUND)) {
            String type = root.getCompound(TAG_CONFIGURATION).getString(KEY_TYPE);
            tooltip.add(new TranslationTextComponent("item.infwhat.infinite_source_config_replicator.saved",
                    new TranslationTextComponent("item.infwhat.infinite_source_config_replicator.type." + type))
                    .withStyle(TextFormatting.GRAY));
        } else {
            tooltip.add(new TranslationTextComponent("item.infwhat.infinite_source_config_replicator.empty")
                    .withStyle(TextFormatting.GRAY));
        }
        ReplicatorPasteMode mode = getPasteMode(stack);
        tooltip.add(new TranslationTextComponent("item.infwhat.infinite_source_config_replicator.mode",
                        modeName(mode))
                .withStyle(TextFormatting.AQUA));
        tooltip.add(new TranslationTextComponent("item.infwhat.infinite_source_config_replicator.fields",
                        new TranslationTextComponent("item.infwhat.infinite_source_config_replicator.fields."
                                + mode.key()))
                .withStyle(TextFormatting.DARK_GRAY));
        // The single instructions line was wider than the screen on small GUI scales, so the
        // whole tooltip was shifted off the left edge. Keep each step on its own short line.
        for (int i = 1; i <= 4; i++) {
            tooltip.add(new TranslationTextComponent(
                    "item.infwhat.infinite_source_config_replicator.instructions." + i)
                    .withStyle(TextFormatting.DARK_GRAY));
        }
    }
}
