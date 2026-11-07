package com.bandybus.infwhat.block;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import java.util.List;

/**
 * Block item for the Ordo Usurpatory Matrix. It carries the machine description and the lore
 * entry; the shift lines are only added while the shift key is held.
 */
public class ItemBlockOrdoUsurpatoryMatrix extends ItemBlockMachine {

    private static final String KEY_NORMAL = "tooltip.infwhat.ordo_usurpatory_matrix.normal";
    private static final String KEY_SHIFT = "tooltip.infwhat.ordo_usurpatory_matrix.shift.";
    private static final String KEY_LORE = "tooltip.infwhat.ordo_usurpatory_matrix.lore";

    private static final String VALUE_FORMAT =
            EnumChatFormatting.GRAY.toString() + EnumChatFormatting.ITALIC;
    private static final String LORE_FORMAT =
            EnumChatFormatting.DARK_PURPLE.toString() + EnumChatFormatting.ITALIC;

    public ItemBlockOrdoUsurpatoryMatrix(Block block) {
        super(block);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip,
                               boolean advanced) {
        if (player != null && player.isSneaking()) {
            tooltip.add(VALUE_FORMAT + StatCollector.translateToLocal(KEY_SHIFT + 1));
        } else {
            tooltip.add(VALUE_FORMAT + StatCollector.translateToLocal(KEY_NORMAL));
        }
        tooltip.add(LORE_FORMAT + StatCollector.translateToLocal(KEY_LORE));
    }
}
