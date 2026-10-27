package com.bandybus.infwhat.item;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.entity.InfFluidGeneratorTileEntity;
import com.bandybus.infwhat.block.entity.InfItemGeneratorTileEntity;
import com.bandybus.infwhat.creative.ModCreativeTab;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
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
        this.setUnlocalizedName(InfinityWhat.MODID + ".infinite_source_config_replicator");
        this.setTextureName(InfinityWhat.MODID + ":infinite_source_config_replicator");
        this.setMaxStackSize(1);
        this.setCreativeTab(ModCreativeTab.INSTANCE);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World worldIn, int x, int y,
                             int z, int side, float hitX, float hitY, float hitZ) {
        TileEntity target = worldIn.getTileEntity(x, y, z);
        boolean itemTarget = target instanceof InfItemGeneratorTileEntity;
        boolean fluidTarget = target instanceof InfFluidGeneratorTileEntity;
        if (!itemTarget && !fluidTarget) {
            return false;
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
        if (root == null || !root.hasKey(TAG_CONFIGURATION, 10)) {
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
        if (itemTarget) {
            ((InfItemGeneratorTileEntity) target).applyOutputConfiguration(
                    outputEnabled, sideMask, config.getInteger(KEY_OUTPUT_RATE),
                    config.getBoolean(KEY_INFINITE_MODE));
        } else {
            ((InfFluidGeneratorTileEntity) target).applyOutputConfiguration(
                    outputEnabled, sideMask, config.getLong(KEY_OUTPUT_RATE));
        }
        show(player, "message.infwhat.replicator.pasted", EnumChatFormatting.GREEN);
        return true;
    }

    /** Writes the settings of one generator into the replicator item. */
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

    /** Sends one coloured chat line to the player. */
    private static void show(EntityPlayer player, String translationKey, EnumChatFormatting color) {
        player.addChatComponentMessage(new ChatComponentTranslation(translationKey)
                .setChatStyle(new ChatStyle().setColor(color)));
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        NBTTagCompound root = stack == null ? null : stack.getTagCompound();
        if (root != null && root.hasKey(TAG_CONFIGURATION, 10)) {
            String type = root.getCompoundTag(TAG_CONFIGURATION).getString(KEY_TYPE);
            tooltip.add(StatCollector.translateToLocalFormatted(
                    "item.infwhat.infinite_source_config_replicator.saved",
                    StatCollector.translateToLocal(
                            "item.infwhat.infinite_source_config_replicator.type." + type)));
        } else {
            tooltip.add(StatCollector.translateToLocal(
                    "item.infwhat.infinite_source_config_replicator.empty"));
        }
        tooltip.add(StatCollector.translateToLocal(
                "item.infwhat.infinite_source_config_replicator.instructions"));
    }
}
