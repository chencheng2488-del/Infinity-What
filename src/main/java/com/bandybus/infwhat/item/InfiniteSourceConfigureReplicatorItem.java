package com.bandybus.infwhat.item;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.RedstoneMode;
import com.bandybus.infwhat.block.entity.InfFluidGeneratorTileEntity;
import com.bandybus.infwhat.block.entity.InfItemGeneratorTileEntity;
import com.bandybus.infwhat.creative.ModCreativeTab;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

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

    public InfiniteSourceConfigureReplicatorItem() {
        this.setRegistryName(InfinityWhat.MODID, "infinite_source_config_replicator");
        this.setTranslationKey(InfinityWhat.MODID + ".infinite_source_config_replicator");
        this.setMaxStackSize(1);
        this.setCreativeTab(ModCreativeTab.INSTANCE);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand,
                                      EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack card = player.getHeldItem(hand);
        TileEntity target = worldIn.getTileEntity(pos);
        boolean itemTarget = target instanceof InfItemGeneratorTileEntity;
        boolean fluidTarget = target instanceof InfFluidGeneratorTileEntity;
        if (!itemTarget && !fluidTarget) {
            // Sneak-use on a non-generator block must not fall through to onItemRightClick,
            // which is reserved for cycling/clearing the card in air.
            return player.isSneaking() ? EnumActionResult.SUCCESS : EnumActionResult.PASS;
        }

        if (worldIn.isRemote) {
            return EnumActionResult.SUCCESS;
        }
        if (player.isSneaking()) {
            copyConfiguration(card, target, itemTarget);
            show(player, "message.infwhat.replicator.copied", TextFormatting.GREEN);
            return EnumActionResult.SUCCESS;
        }

        NBTTagCompound root = card.getTagCompound();
        if (root == null || !root.hasKey(TAG_CONFIGURATION, 10)) {
            show(player, "message.infwhat.replicator.empty", TextFormatting.RED);
            return EnumActionResult.SUCCESS;
        }
        NBTTagCompound config = root.getCompoundTag(TAG_CONFIGURATION);
        String expectedType = itemTarget ? "item" : "fluid";
        if (!config.getString(KEY_TYPE).equals(expectedType)) {
            show(player, "message.infwhat.replicator.type_mismatch", TextFormatting.RED);
            return EnumActionResult.SUCCESS;
        }

        boolean outputEnabled = config.getBoolean(KEY_OUTPUT_ENABLED);
        int sideMask = config.getInteger(KEY_OUTPUT_SIDES);
        ReplicatorPasteMode pasteMode = getPasteMode(card);
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
                            readFluidRates(config)) && generator.setBatchSize(readBatch(config));
                    break;
            }
        }
        if (!applied) {
            show(player, "message.infwhat.replicator.rejected", TextFormatting.RED);
            return EnumActionResult.SUCCESS;
        }
        show(player, "message.infwhat.replicator.pasted_mode", TextFormatting.GREEN,
                modeName(pasteMode));
        return EnumActionResult.SUCCESS;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand hand) {
        ItemStack card = playerIn.getHeldItem(hand);
        if (!playerIn.isSneaking()) {
            // Ignore the fall-through call that follows a normal block interaction.
            RayTraceResult hit = playerIn.rayTrace(5.0D, 0.0F);
            if (hit != null && hit.typeOfHit != RayTraceResult.Type.MISS) {
                return new ActionResult<ItemStack>(EnumActionResult.PASS, card);
            }
            if (worldIn.isRemote) {
                return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, card);
            }
            ReplicatorPasteMode mode = getPasteMode(card).next();
            NBTTagCompound root = card.getTagCompound();
            if (root == null) {
                root = new NBTTagCompound();
                card.setTagCompound(root);
            }
            root.setInteger(KEY_MODE, mode.ordinal());
            show(playerIn, "message.infwhat.replicator.mode", TextFormatting.AQUA, modeName(mode));
            return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, card);
        }
        if (worldIn.isRemote) {
            return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, card);
        }
        NBTTagCompound root = card.getTagCompound();
        if (root == null || !root.hasKey(TAG_CONFIGURATION, 10)) {
            show(playerIn, "message.infwhat.replicator.empty", TextFormatting.RED);
            return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, card);
        }
        root.removeTag(TAG_CONFIGURATION);
        card.setTagCompound(root);
        show(playerIn, "message.infwhat.replicator.cleared", TextFormatting.GREEN);
        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, card);
    }

    private static ReplicatorPasteMode getPasteMode(ItemStack card) {
        NBTTagCompound root = card.getTagCompound();
        return root == null ? ReplicatorPasteMode.ALL
                : ReplicatorPasteMode.byId(root.getInteger(KEY_MODE));
    }

    private static String modeName(ReplicatorPasteMode mode) {
        return I18n.format("item.infwhat.infinite_source_config_replicator.mode." + mode.key());
    }

    private static int readPulse(NBTTagCompound config, String key) {
        if (!config.hasKey(key, 3)) {
            return InfItemGeneratorTileEntity.DEFAULT_PULSE_TICKS;
        }
        return Math.max(InfItemGeneratorTileEntity.MIN_PULSE_TICKS,
                Math.min(InfItemGeneratorTileEntity.MAX_PULSE_TICKS, config.getInteger(key)));
    }

    private static int readBatch(NBTTagCompound config) {
        int size = config.hasKey(KEY_BATCH_SIZE, 3)
                ? config.getInteger(KEY_BATCH_SIZE) : InfItemGeneratorTileEntity.DEFAULT_BATCH_SIZE;
        return Math.max(InfItemGeneratorTileEntity.MIN_BATCH_SIZE,
                Math.min(InfItemGeneratorTileEntity.MAX_BATCH_SIZE, size));
    }

    private static int[] readItemRates(NBTTagCompound config) {
        int[] rates = new int[EnumFacing.VALUES.length];
        boolean hasSideRates = config.hasKey(SIDE_RATE_KEYS[0], 3);
        int legacyRate = Math.max(1, config.hasKey(KEY_OUTPUT_RATE, 3)
                ? config.getInteger(KEY_OUTPUT_RATE) : 1);
        for (EnumFacing direction : EnumFacing.VALUES) {
            int index = direction.getIndex();
            rates[index] = hasSideRates
                    ? (config.hasKey(SIDE_RATE_KEYS[index], 3)
                            ? config.getInteger(SIDE_RATE_KEYS[index]) : legacyRate)
                    : legacyRate;
        }
        return rates;
    }

    private static long[] readFluidRates(NBTTagCompound config) {
        long[] rates = new long[EnumFacing.VALUES.length];
        boolean hasSideRates = config.hasKey(SIDE_RATE_KEYS[0], 4);
        long legacyRate = Math.max(1L, config.hasKey(KEY_OUTPUT_RATE, 4)
                ? config.getLong(KEY_OUTPUT_RATE) : 1L);
        for (EnumFacing direction : EnumFacing.VALUES) {
            int index = direction.getIndex();
            rates[index] = hasSideRates
                    ? (config.hasKey(SIDE_RATE_KEYS[index], 4)
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
            for (EnumFacing direction : EnumFacing.VALUES) {
                config.setInteger(SIDE_RATE_KEYS[direction.getIndex()],
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
            for (EnumFacing direction : EnumFacing.VALUES) {
                config.setLong(SIDE_RATE_KEYS[direction.getIndex()],
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
        String[] keys = new String[EnumFacing.VALUES.length];
        for (EnumFacing direction : EnumFacing.VALUES) {
            String name = direction.getName();
            keys[direction.getIndex()] = "SideRate"
                    + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
        return keys;
    }

    private static void show(EntityPlayer player, String translationKey, TextFormatting color,
                             Object... arguments) {
        player.sendStatusMessage(new TextComponentTranslation(translationKey, arguments)
                .setStyle(new Style().setColor(color)), true);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip,
                               ITooltipFlag flag) {
        super.addInformation(stack, worldIn, tooltip, flag);
        NBTTagCompound root = stack.getTagCompound();
        if (root != null && root.hasKey(TAG_CONFIGURATION, 10)) {
            String type = root.getCompoundTag(TAG_CONFIGURATION).getString(KEY_TYPE);
            tooltip.add(TextFormatting.GRAY + I18n.format(
                    "item.infwhat.infinite_source_config_replicator.saved",
                    I18n.format("item.infwhat.infinite_source_config_replicator.type." + type)));
        } else {
            tooltip.add(TextFormatting.GRAY + I18n.format(
                    "item.infwhat.infinite_source_config_replicator.empty"));
        }
        ReplicatorPasteMode mode = getPasteMode(stack);
        tooltip.add(TextFormatting.AQUA + I18n.format(
                "item.infwhat.infinite_source_config_replicator.mode", modeName(mode)));
        tooltip.add(TextFormatting.DARK_GRAY + I18n.format(
                "item.infwhat.infinite_source_config_replicator.fields",
                I18n.format("item.infwhat.infinite_source_config_replicator.fields." + mode.key())));
        // 1.12.2 tooltips do not wrap; split the long instruction text so the tooltip stays
        // narrow enough to render fully even when hovering the leftmost inventory column.
        String instructions = I18n.format(
                "item.infwhat.infinite_source_config_replicator.instructions");
        for (String line : instructions.split("[\uFF1B;]")) {
            if (!line.trim().isEmpty()) {
                tooltip.add(TextFormatting.DARK_GRAY + line.trim());
            }
        }
    }
}
