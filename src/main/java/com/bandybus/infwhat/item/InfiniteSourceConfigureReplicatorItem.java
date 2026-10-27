package com.bandybus.infwhat.item;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.entity.InfFluidGeneratorTileEntity;
import com.bandybus.infwhat.block.entity.InfItemGeneratorTileEntity;
import com.bandybus.infwhat.creative.ModCreativeTab;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.List;

/** Sneak-use copies a generator configuration; regular use pastes it onto a matching generator. */
public class InfiniteSourceConfigureReplicatorItem extends Item {

    private static final String TAG_CONFIGURATION = "Configuration";
    private static final String KEY_TYPE = "Type";
    private static final String KEY_OUTPUT_ENABLED = "OutputEnabled";
    private static final String KEY_OUTPUT_SIDES = "OutputSides";
    private static final String KEY_OUTPUT_RATE = "OutputRate";
    private static final String KEY_INFINITE_MODE = "InfiniteMode";

    public InfiniteSourceConfigureReplicatorItem() {
        this.setRegistryName(InfinityWhat.MODID, "infinite_source_config_replicator");
        this.setTranslationKey(InfinityWhat.MODID + ".infinite_source_config_replicator");
        this.setMaxStackSize(1);
        this.setCreativeTab(ModCreativeTab.INSTANCE);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos,
                                      EnumHand hand, EnumFacing facing,
                                      float hitX, float hitY, float hitZ) {
        TileEntity target = worldIn.getTileEntity(pos);
        boolean itemTarget = target instanceof InfItemGeneratorTileEntity;
        boolean fluidTarget = target instanceof InfFluidGeneratorTileEntity;
        if (!itemTarget && !fluidTarget) {
            return EnumActionResult.PASS;
        }
        if (worldIn.isRemote) {
            return EnumActionResult.SUCCESS;
        }
        if (player == null) {
            return EnumActionResult.PASS;
        }

        ItemStack card = player.getHeldItem(hand);
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
        if (!expectedType.equals(config.getString(KEY_TYPE))) {
            show(player, "message.infwhat.replicator.type_mismatch", TextFormatting.RED);
            return EnumActionResult.SUCCESS;
        }

        boolean outputEnabled = config.getBoolean(KEY_OUTPUT_ENABLED);
        int sideMask = config.getInteger(KEY_OUTPUT_SIDES);
        if (itemTarget) {
            ((InfItemGeneratorTileEntity) target).applyOutputConfiguration(
                    outputEnabled, sideMask, config.getInteger(KEY_OUTPUT_RATE),
                    config.getBoolean(KEY_INFINITE_MODE));
        } else {
            ((InfFluidGeneratorTileEntity) target).applyOutputConfiguration(
                    outputEnabled, sideMask, config.getLong(KEY_OUTPUT_RATE));
        }
        show(player, "message.infwhat.replicator.pasted", TextFormatting.GREEN);
        return EnumActionResult.SUCCESS;
    }

    private static void copyConfiguration(ItemStack card, TileEntity target, boolean itemTarget) {
        NBTTagCompound config = new NBTTagCompound();
        if (itemTarget) {
            InfItemGeneratorTileEntity generator = (InfItemGeneratorTileEntity) target;
            config.setString(KEY_TYPE, "item");
            config.setBoolean(KEY_OUTPUT_ENABLED, generator.isOutputEnabled());
            config.setInteger(KEY_OUTPUT_SIDES, generator.getOutputSideMask());
            config.setInteger(KEY_OUTPUT_RATE, generator.getOutputItemsPerTick());
            config.setBoolean(KEY_INFINITE_MODE, generator.isInfiniteModeEnabled());
        } else {
            InfFluidGeneratorTileEntity generator = (InfFluidGeneratorTileEntity) target;
            config.setString(KEY_TYPE, "fluid");
            config.setBoolean(KEY_OUTPUT_ENABLED, generator.isOutputEnabled());
            config.setInteger(KEY_OUTPUT_SIDES, generator.getOutputSideMask());
            config.setLong(KEY_OUTPUT_RATE, generator.getOutputAmountPerTick());
        }
        NBTTagCompound root = card.getTagCompound();
        if (root == null) {
            root = new NBTTagCompound();
            card.setTagCompound(root);
        }
        root.setTag(TAG_CONFIGURATION, config);
    }

    private static void show(EntityPlayer player, String translationKey, TextFormatting color) {
        player.sendStatusMessage(new TextComponentTranslation(translationKey)
                .setStyle(new Style().setColor(color)), true);
    }

    @Override
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip,
                               net.minecraft.client.util.ITooltipFlag flag) {
        super.addInformation(stack, worldIn, tooltip, flag);
        NBTTagCompound root = stack.getTagCompound();
        if (root != null && root.hasKey(TAG_CONFIGURATION, 10)) {
            String type = root.getCompoundTag(TAG_CONFIGURATION).getString(KEY_TYPE);
            tooltip.add(I18n.format("item.infwhat.infinite_source_config_replicator.saved",
                    I18n.format("item.infwhat.infinite_source_config_replicator.type." + type)));
        } else {
            tooltip.add(I18n.format("item.infwhat.infinite_source_config_replicator.empty"));
        }
        tooltip.add(I18n.format("item.infwhat.infinite_source_config_replicator.instructions"));
    }
}
