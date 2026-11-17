package com.bandybus.infwhat.item;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.RedstoneMode;
import com.bandybus.infwhat.block.entity.InfFluidGeneratorTileEntity;
import com.bandybus.infwhat.block.entity.InfItemGeneratorTileEntity;
import com.bandybus.infwhat.creative.ModCreativeTab;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;

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

    public InfiniteSourceConfigureReplicatorItem(int itemId) {
        super(itemId);
        this.setUnlocalizedName(InfinityWhat.MODID + ".infinite_source_config_replicator");
        this.setTextureName(InfinityWhat.MODID + ":infinite_source_config_replicator");
        this.setMaxStackSize(1);
        this.setCreativeTab(ModCreativeTab.INSTANCE);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World worldIn, int x, int y,
                             int z, int side, float hitX, float hitY, float hitZ) {
        TileEntity target = worldIn.getBlockTileEntity(x, y, z);
        boolean itemTarget = target instanceof InfItemGeneratorTileEntity;
        boolean fluidTarget = target instanceof InfFluidGeneratorTileEntity;
        if (!itemTarget && !fluidTarget) {
            return player != null && player.isSneaking();
        }

        if (worldIn.isRemote) {
            return true;
        }
        if (player == null || stack == null) {
            return false;
        }
        if (player.isSneaking()) {
            copyConfiguration(stack, target, itemTarget);
            show(player, "message.infwhat.replicator.copied", EnumChatFormatting.GREEN);
            return true;
        }

        NBTTagCompound root = stack.getTagCompound();
        if (root == null || !com.bandybus.infwhat.core.NbtHelper.hasKey(root, TAG_CONFIGURATION, 10)) {
            show(player, "message.infwhat.replicator.empty", EnumChatFormatting.RED);
            return true;
        }
        NBTTagCompound config = root.getCompoundTag(TAG_CONFIGURATION);
        String expectedType = itemTarget ? "item" : "fluid";
        if (!expectedType.equals(config.getString(KEY_TYPE))) {
            show(player, "message.infwhat.replicator.type_mismatch", EnumChatFormatting.RED);
            return true;
        }

        boolean outputEnabled = config.getBoolean(KEY_OUTPUT_ENABLED);
        int sideMask = config.getInteger(KEY_OUTPUT_SIDES);
        ReplicatorPasteMode pasteMode = getPasteMode(stack);
        boolean applied;
        if (itemTarget) {
            InfItemGeneratorTileEntity generator = (InfItemGeneratorTileEntity) target;
            switch (pasteMode) {
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
                    applied = generator.setBatchSize(readBatch(config));
                    break;
                case ALL:
                default:
                    applied = generator.applyFullConfiguration(outputEnabled, sideMask,
                            readItemRates(config), config.getBoolean(KEY_INFINITE_MODE))
                            && generator.setBatchSize(readBatch(config));
                    break;
            }
        } else {
            InfFluidGeneratorTileEntity generator = (InfFluidGeneratorTileEntity) target;
            switch (pasteMode) {
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
                    applied = generator.setBatchSize(readBatch(config));
                    break;
                case ALL:
                default:
                    applied = generator.applyFullConfiguration(outputEnabled, sideMask,
                            readFluidRates(config))
                            && generator.setBatchSize(readBatch(config));
                    break;
            }
        }
        if (!applied) {
            show(player, "message.infwhat.replicator.rejected", EnumChatFormatting.RED);
            return true;
        }
        player.addChatMessage(EnumChatFormatting.GREEN + StatCollector.translateToLocalFormatted(
                "message.infwhat.replicator.pasted_mode", modeName(pasteMode)));
        return true;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World worldIn, EntityPlayer player) {
        if (!player.isSneaking()) {
            // Regular air use cycles the paste mode; sneak-air clears the copied data.
            if (worldIn.isRemote) {
                return stack;
            }
            ReplicatorPasteMode mode = getPasteMode(stack).next();
            NBTTagCompound root = stack.getTagCompound();
            if (root == null) {
                root = new NBTTagCompound();
                stack.setTagCompound(root);
            }
            root.setInteger(KEY_MODE, mode.ordinal());
            player.addChatMessage(EnumChatFormatting.AQUA + StatCollector.translateToLocalFormatted(
                    "message.infwhat.replicator.mode", modeName(mode)));
            return stack;
        }
        if (worldIn.isRemote) {
            return stack;
        }
        NBTTagCompound root = stack.getTagCompound();
        if (root == null || !com.bandybus.infwhat.core.NbtHelper.hasKey(root, TAG_CONFIGURATION, 10)) {
            show(player, "message.infwhat.replicator.empty", EnumChatFormatting.RED);
            return stack;
        }
        root.removeTag(TAG_CONFIGURATION);
        stack.setTagCompound(root);
        show(player, "message.infwhat.replicator.cleared", EnumChatFormatting.GREEN);
        return stack;
    }

    private static ReplicatorPasteMode getPasteMode(ItemStack stack) {
        NBTTagCompound root = stack == null ? null : stack.getTagCompound();
        return root == null ? ReplicatorPasteMode.ALL
                : ReplicatorPasteMode.byId(root.getInteger(KEY_MODE));
    }

    private static String modeName(ReplicatorPasteMode mode) {
        return StatCollector.translateToLocal(
                "item.infwhat.infinite_source_config_replicator.mode." + mode.key());
    }

    private static int readPulse(NBTTagCompound config, String key) {
        if (!com.bandybus.infwhat.core.NbtHelper.hasKey(config, key, 3)) {
            return InfItemGeneratorTileEntity.DEFAULT_PULSE_TICKS;
        }
        return Math.max(InfItemGeneratorTileEntity.MIN_PULSE_TICKS,
                Math.min(InfItemGeneratorTileEntity.MAX_PULSE_TICKS, config.getInteger(key)));
    }

    private static int readBatch(NBTTagCompound config) {
        int size = com.bandybus.infwhat.core.NbtHelper.hasKey(config, KEY_BATCH_SIZE, 3)
                ? config.getInteger(KEY_BATCH_SIZE) : InfItemGeneratorTileEntity.DEFAULT_BATCH_SIZE;
        return Math.max(InfItemGeneratorTileEntity.MIN_BATCH_SIZE,
                Math.min(InfItemGeneratorTileEntity.MAX_BATCH_SIZE, size));
    }

    private static int[] readItemRates(NBTTagCompound config) {
        int[] rates = new int[ForgeDirection.VALID_DIRECTIONS.length];
        boolean hasSideRates = com.bandybus.infwhat.core.NbtHelper.hasKey(config, SIDE_RATE_KEYS[0], 3);
        int legacyRate = Math.max(1, com.bandybus.infwhat.core.NbtHelper.hasKey(config, KEY_OUTPUT_RATE, 3)
                ? config.getInteger(KEY_OUTPUT_RATE) : 1);
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            int index = direction.ordinal();
            rates[index] = hasSideRates
                    ? (com.bandybus.infwhat.core.NbtHelper.hasKey(config, SIDE_RATE_KEYS[index], 3)
                            ? config.getInteger(SIDE_RATE_KEYS[index]) : legacyRate)
                    : legacyRate;
        }
        return rates;
    }

    private static long[] readFluidRates(NBTTagCompound config) {
        long[] rates = new long[ForgeDirection.VALID_DIRECTIONS.length];
        boolean hasSideRates = com.bandybus.infwhat.core.NbtHelper.hasKey(config, SIDE_RATE_KEYS[0], 4);
        long legacyRate = Math.max(1L, com.bandybus.infwhat.core.NbtHelper.hasKey(config, KEY_OUTPUT_RATE, 4)
                ? config.getLong(KEY_OUTPUT_RATE) : 1L);
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            int index = direction.ordinal();
            rates[index] = hasSideRates
                    ? (com.bandybus.infwhat.core.NbtHelper.hasKey(config, SIDE_RATE_KEYS[index], 4)
                            ? config.getLong(SIDE_RATE_KEYS[index]) : legacyRate)
                    : legacyRate;
        }
        return rates;
    }

    private static void copyConfiguration(ItemStack card, TileEntity target, boolean itemTarget) {
        NBTTagCompound config = new NBTTagCompound();
        if (itemTarget) {
            InfItemGeneratorTileEntity generator = (InfItemGeneratorTileEntity) target;
            config.setString(KEY_TYPE, "item");
            config.setBoolean(KEY_OUTPUT_ENABLED, generator.isOutputEnabled());
            config.setInteger(KEY_OUTPUT_SIDES, generator.getOutputSideMask());
            config.setBoolean(KEY_INFINITE_MODE, generator.isInfiniteModeEnabled());
            config.setInteger(KEY_REDSTONE_MODE, generator.getRedstoneMode().ordinal());
            config.setInteger(KEY_PULSE_ON, generator.getPulseOnTicks());
            config.setInteger(KEY_PULSE_OFF, generator.getPulseOffTicks());
            config.setInteger(KEY_BATCH_SIZE, generator.getBatchSize());
            for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
                config.setInteger(SIDE_RATE_KEYS[direction.ordinal()],
                        generator.getSideRate(direction));
            }
        } else {
            InfFluidGeneratorTileEntity generator = (InfFluidGeneratorTileEntity) target;
            config.setString(KEY_TYPE, "fluid");
            config.setBoolean(KEY_OUTPUT_ENABLED, generator.isOutputEnabled());
            config.setInteger(KEY_OUTPUT_SIDES, generator.getOutputSideMask());
            config.setInteger(KEY_REDSTONE_MODE, generator.getRedstoneMode().ordinal());
            config.setInteger(KEY_PULSE_ON, generator.getPulseOnTicks());
            config.setInteger(KEY_PULSE_OFF, generator.getPulseOffTicks());
            config.setInteger(KEY_BATCH_SIZE, generator.getBatchSize());
            for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
                config.setLong(SIDE_RATE_KEYS[direction.ordinal()],
                        generator.getSideRateMb(direction));
            }
        }
        NBTTagCompound root = card.getTagCompound();
        if (root == null) {
            root = new NBTTagCompound();
            card.setTagCompound(root);
        }
        root.setTag(TAG_CONFIGURATION, config);
    }

    private static String[] createSideRateKeys() {
        String[] keys = new String[ForgeDirection.VALID_DIRECTIONS.length];
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            String name = direction.name().toLowerCase(java.util.Locale.ROOT);
            keys[direction.ordinal()] = "SideRate"
                    + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
        return keys;
    }

    private static void show(EntityPlayer player, String translationKey, EnumChatFormatting color) {
        // 1.6.4 only has addChatMessage(String).
        player.addChatMessage(color + StatCollector.translateToLocal(translationKey));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        NBTTagCompound root = stack == null ? null : stack.getTagCompound();
        if (root != null && com.bandybus.infwhat.core.NbtHelper.hasKey(root, TAG_CONFIGURATION, 10)) {
            String type = root.getCompoundTag(TAG_CONFIGURATION).getString(KEY_TYPE);
            tooltip.add(StatCollector.translateToLocalFormatted(
                    "item.infwhat.infinite_source_config_replicator.saved",
                    StatCollector.translateToLocal(
                            "item.infwhat.infinite_source_config_replicator.type." + type)));
        } else {
            tooltip.add(StatCollector.translateToLocal(
                    "item.infwhat.infinite_source_config_replicator.empty"));
        }
        ReplicatorPasteMode mode = getPasteMode(stack);
        tooltip.add(StatCollector.translateToLocalFormatted(
                "item.infwhat.infinite_source_config_replicator.mode", modeName(mode)));
        tooltip.add(StatCollector.translateToLocalFormatted(
                "item.infwhat.infinite_source_config_replicator.fields",
                StatCollector.translateToLocal(
                        "item.infwhat.infinite_source_config_replicator.fields." + mode.key())));
        // 1.6.4 tooltips do not wrap; split the long instruction text so the tooltip stays
        // narrow enough to render fully even when hovering the leftmost inventory column.
        String instructions = StatCollector.translateToLocal(
                "item.infwhat.infinite_source_config_replicator.instructions");
        for (String line : com.bandybus.infwhat.core.TooltipHelper.wrap(instructions, 24)) {
            tooltip.add(line);
        }
    }
}
