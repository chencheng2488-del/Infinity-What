package com.bandybus.infwhat.item;

import com.bandybus.infwhat.block.RedstoneMode;
import com.bandybus.infwhat.block.entity.InfFluidGeneratorBlockEntity;
import com.bandybus.infwhat.block.entity.InfItemGeneratorBlockEntity;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.RayTraceFluidMode;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
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

    public InfiniteSourceConfigureReplicatorItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public EnumActionResult onItemUse(ItemUseContext context) {
        World world = context.getWorld();
        TileEntity target = world.getTileEntity(context.getPos());
        boolean itemTarget = target instanceof InfItemGeneratorBlockEntity;
        boolean fluidTarget = target instanceof InfFluidGeneratorBlockEntity;
        if (!itemTarget && !fluidTarget) {
            // Sneak-use on a non-generator block must not fall through to onItemRightClick,
            // which is reserved for clearing the card in air.
            return context.getPlayer() != null && context.getPlayer().isSneaking()
                    ? EnumActionResult.SUCCESS : EnumActionResult.PASS;
        }

        if (world.isRemote) {
            return EnumActionResult.SUCCESS;
        }
        if (context.getPlayer() == null) {
            return EnumActionResult.PASS;
        }
        ItemStack card = context.getItem();
        if (context.getPlayer().isSneaking()) {
            copyConfiguration(card, target, itemTarget);
            show(context, "message.infwhat.replicator.copied", TextFormatting.GREEN);
            return EnumActionResult.SUCCESS;
        }

        NBTTagCompound root = card.getTag();
        if (root == null || !root.hasKey(TAG_CONFIGURATION, Constants.NBT.TAG_COMPOUND)) {
            show(context, "message.infwhat.replicator.empty", TextFormatting.RED);
            return EnumActionResult.SUCCESS;
        }
        NBTTagCompound config = root.getCompoundTag(TAG_CONFIGURATION);
        String expectedType = itemTarget ? "item" : "fluid";
        if (!config.getString(KEY_TYPE).equals(expectedType)) {
            show(context, "message.infwhat.replicator.type_mismatch", TextFormatting.RED);
            return EnumActionResult.SUCCESS;
        }

        boolean outputEnabled = config.getBoolean(KEY_OUTPUT_ENABLED);
        int sideMask = config.getInteger(KEY_OUTPUT_SIDES);
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
                            RedstoneMode.byId(config.getInteger(KEY_REDSTONE_MODE)),
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
                            RedstoneMode.byId(config.getInteger(KEY_REDSTONE_MODE)),
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
            return EnumActionResult.SUCCESS;
        }
        show(context, "message.infwhat.replicator.pasted_mode", TextFormatting.GREEN,
                modeName(pasteMode));
        return EnumActionResult.SUCCESS;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack card = player.getHeldItem(hand);
        if (!player.isSneaking()) {
            // Regular air use cycles the paste mode; sneak-air keeps its 2.2 clear action.
            // Ignore the fall-through onItemRightClick that follows a normal block interaction.
            // A miss still returns a RayTraceResult, so the hit type must be checked instead.
            RayTraceResult hit = player.rayTrace(5.0D, 0.0F, RayTraceFluidMode.NEVER);
            if (hit != null && hit.type != RayTraceResult.Type.MISS) {
                return new ActionResult<ItemStack>(EnumActionResult.PASS, card);
            }
            if (world.isRemote) {
                return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, card);
            }
            ReplicatorPasteMode mode = getPasteMode(card).next();
            card.getOrCreateTag().setInteger(KEY_MODE, mode.ordinal());
            show(player, "message.infwhat.replicator.mode", TextFormatting.AQUA, modeName(mode));
            return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, card);
        }
        if (world.isRemote) {
            return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, card);
        }
        NBTTagCompound root = card.getTag();
        if (root == null || !root.hasKey(TAG_CONFIGURATION, Constants.NBT.TAG_COMPOUND)) {
            show(player, "message.infwhat.replicator.empty", TextFormatting.RED);
            return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, card);
        }
        root.removeTag(TAG_CONFIGURATION);
        card.setTag(root);
        show(player, "message.infwhat.replicator.cleared", TextFormatting.GREEN);
        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, card);
    }

    private static ReplicatorPasteMode getPasteMode(ItemStack card) {
        NBTTagCompound root = card.getTag();
        return root == null ? ReplicatorPasteMode.ALL
                : ReplicatorPasteMode.byId(root.getInteger(KEY_MODE));
    }

    private static ITextComponent modeName(ReplicatorPasteMode mode) {
        return new TextComponentTranslation(
                "item.infwhat.infinite_source_config_replicator.mode." + mode.key());
    }

    private static int readPulse(NBTTagCompound config, String key) {
        if (!config.hasKey(key, Constants.NBT.TAG_INT)) {
            return InfItemGeneratorBlockEntity.DEFAULT_PULSE_TICKS;
        }
        return Math.max(InfItemGeneratorBlockEntity.MIN_PULSE_TICKS,
                Math.min(InfItemGeneratorBlockEntity.MAX_PULSE_TICKS, config.getInteger(key)));
    }

    private static int readBatch(NBTTagCompound config) {
        int size = config.hasKey(KEY_BATCH_SIZE, Constants.NBT.TAG_INT)
                ? config.getInteger(KEY_BATCH_SIZE) : InfItemGeneratorBlockEntity.DEFAULT_BATCH_SIZE;
        return Math.max(InfItemGeneratorBlockEntity.MIN_BATCH_SIZE,
                Math.min(InfItemGeneratorBlockEntity.MAX_BATCH_SIZE, size));
    }

    private static int[] readItemRates(NBTTagCompound config) {
        int[] rates = new int[EnumFacing.values().length];
        boolean hasSideRates = config.hasKey(SIDE_RATE_KEYS[0], Constants.NBT.TAG_INT);
        int legacyRate = Math.max(1, config.hasKey(KEY_OUTPUT_RATE, Constants.NBT.TAG_INT)
                ? config.getInteger(KEY_OUTPUT_RATE) : 1);
        for (EnumFacing direction : EnumFacing.values()) {
            int index = direction.getIndex();
            rates[index] = hasSideRates
                    ? (config.hasKey(SIDE_RATE_KEYS[index], Constants.NBT.TAG_INT)
                            ? config.getInteger(SIDE_RATE_KEYS[index]) : legacyRate)
                    : legacyRate;
        }
        return rates;
    }

    private static long[] readFluidRates(NBTTagCompound config) {
        long[] rates = new long[EnumFacing.values().length];
        boolean hasSideRates = config.hasKey(SIDE_RATE_KEYS[0], Constants.NBT.TAG_LONG);
        long legacyRate = Math.max(1L, config.hasKey(KEY_OUTPUT_RATE, Constants.NBT.TAG_LONG)
                ? config.getLong(KEY_OUTPUT_RATE) : 1L);
        for (EnumFacing direction : EnumFacing.values()) {
            int index = direction.getIndex();
            rates[index] = hasSideRates
                    ? (config.hasKey(SIDE_RATE_KEYS[index], Constants.NBT.TAG_LONG)
                            ? config.getLong(SIDE_RATE_KEYS[index]) : legacyRate)
                    : legacyRate;
        }
        return rates;
    }

    private static void copyConfiguration(ItemStack card, TileEntity target, boolean itemTarget) {
        NBTTagCompound config = new NBTTagCompound();
        if (itemTarget) {
            InfItemGeneratorBlockEntity generator = (InfItemGeneratorBlockEntity) target;
            config.setString(KEY_TYPE, "item");
            config.setBoolean(KEY_OUTPUT_ENABLED, generator.isOutputEnabled());
            config.setInteger(KEY_OUTPUT_SIDES, generator.getOutputSideMask());
            config.setBoolean(KEY_INFINITE_MODE, generator.isInfiniteModeEnabled());
            config.setInteger(KEY_REDSTONE_MODE, generator.getRedstoneMode().ordinal());
            config.setInteger(KEY_PULSE_ON, generator.getPulseOnTicks());
            config.setInteger(KEY_PULSE_OFF, generator.getPulseOffTicks());
            config.setInteger(KEY_BATCH_SIZE, generator.getBatchSize());
            for (EnumFacing direction : EnumFacing.values()) {
                config.setInteger(SIDE_RATE_KEYS[direction.getIndex()],
                        generator.getSideRate(direction));
            }
        } else {
            InfFluidGeneratorBlockEntity generator = (InfFluidGeneratorBlockEntity) target;
            config.setString(KEY_TYPE, "fluid");
            config.setBoolean(KEY_OUTPUT_ENABLED, generator.isOutputEnabled());
            config.setInteger(KEY_OUTPUT_SIDES, generator.getOutputSideMask());
            config.setInteger(KEY_REDSTONE_MODE, generator.getRedstoneMode().ordinal());
            config.setInteger(KEY_PULSE_ON, generator.getPulseOnTicks());
            config.setInteger(KEY_PULSE_OFF, generator.getPulseOffTicks());
            config.setInteger(KEY_BATCH_SIZE, generator.getBatchSize());
            for (EnumFacing direction : EnumFacing.values()) {
                config.setLong(SIDE_RATE_KEYS[direction.getIndex()],
                        generator.getSideRateMb(direction));
            }
        }
        card.getOrCreateTag().setTag(TAG_CONFIGURATION, config);
    }

    private static String[] createSideRateKeys() {
        EnumFacing[] directions = EnumFacing.values();
        String[] keys = new String[directions.length];
        for (EnumFacing direction : directions) {
            String name = direction.getName();
            keys[direction.getIndex()] = "SideRate"
                    + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
        return keys;
    }

    private static void show(ItemUseContext context, String translationKey, TextFormatting color) {
        context.getPlayer().sendStatusMessage(
                new TextComponentTranslation(translationKey).applyTextStyle(color), true);
    }

    private static void show(ItemUseContext context, String translationKey, TextFormatting color,
                             ITextComponent argument) {
        context.getPlayer().sendStatusMessage(
                new TextComponentTranslation(translationKey, argument).applyTextStyle(color), true);
    }

    private static void show(EntityPlayer player, String translationKey, TextFormatting color) {
        player.sendStatusMessage(
                new TextComponentTranslation(translationKey).applyTextStyle(color), true);
    }

    private static void show(EntityPlayer player, String translationKey, TextFormatting color,
                             ITextComponent argument) {
        player.sendStatusMessage(
                new TextComponentTranslation(translationKey, argument).applyTextStyle(color), true);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
                               List<ITextComponent> tooltip, ITooltipFlag flag) {
        super.addInformation(stack, world, tooltip, flag);
        NBTTagCompound root = stack.getTag();
        if (root != null && root.hasKey(TAG_CONFIGURATION, Constants.NBT.TAG_COMPOUND)) {
            String type = root.getCompoundTag(TAG_CONFIGURATION).getString(KEY_TYPE);
            tooltip.add(new TextComponentTranslation(
                    "item.infwhat.infinite_source_config_replicator.saved",
                    new TextComponentTranslation(
                            "item.infwhat.infinite_source_config_replicator.type." + type))
                    .applyTextStyle(TextFormatting.GRAY));
        } else {
            tooltip.add(new TextComponentTranslation(
                    "item.infwhat.infinite_source_config_replicator.empty")
                    .applyTextStyle(TextFormatting.GRAY));
        }
        ReplicatorPasteMode mode = getPasteMode(stack);
        tooltip.add(new TextComponentTranslation(
                "item.infwhat.infinite_source_config_replicator.mode", modeName(mode))
                .applyTextStyle(TextFormatting.AQUA));
        tooltip.add(new TextComponentTranslation(
                "item.infwhat.infinite_source_config_replicator.fields",
                new TextComponentTranslation(
                        "item.infwhat.infinite_source_config_replicator.fields." + mode.key()))
                .applyTextStyle(TextFormatting.DARK_GRAY));
        tooltip.add(new TextComponentTranslation(
                "item.infwhat.infinite_source_config_replicator.instructions")
                .applyTextStyle(TextFormatting.DARK_GRAY));
    }
}
