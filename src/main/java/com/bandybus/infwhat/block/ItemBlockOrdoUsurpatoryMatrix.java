package com.bandybus.infwhat.block;

import com.bandybus.infwhat.core.TooltipHelper;
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

    public ItemBlockOrdoUsurpatoryMatrix(int itemId) {
        super(itemId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip,
                               boolean advanced) {
        // 1.6.4 tooltips do not wrap and are flipped off-screen when a line is too wide.
        String normalKey = player != null && player.isSneaking() ? KEY_SHIFT + 1 : KEY_NORMAL;
        for (String line : TooltipHelper.wrap(StatCollector.translateToLocal(normalKey), 24)) {
            tooltip.add(VALUE_FORMAT + line);
        }
        for (String line : TooltipHelper.wrap(StatCollector.translateToLocal(KEY_LORE), 24)) {
            tooltip.add(LORE_FORMAT + line);
        }
    }
}
