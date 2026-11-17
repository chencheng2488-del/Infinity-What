package com.bandybus.infwhat.block;

import net.minecraft.block.Block;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Block item for the Ordo Usurpatory Matrix. It carries the machine description and the lore
 * entry; the shift lines are only added while the shift key is held.
 */
@SuppressWarnings("deprecation")
public class OrdoUsurpatoryMatrixItemBlock extends ItemBlock {

    private static final String KEY_NORMAL = "tooltip.infwhat.ordo_usurpatory_matrix.normal";
    private static final String KEY_SHIFT = "tooltip.infwhat.ordo_usurpatory_matrix.shift.";
    private static final String KEY_LORE = "tooltip.infwhat.ordo_usurpatory_matrix.lore";

    private static final String VALUE_FORMAT =
            TextFormatting.GRAY.toString() + TextFormatting.ITALIC;
    private static final String LORE_FORMAT =
            TextFormatting.DARK_PURPLE.toString() + TextFormatting.ITALIC;

    public OrdoUsurpatoryMatrixItemBlock(Block block) {
        super(block);
    }

    @SuppressWarnings("deprecation")
    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip,
                               ITooltipFlag flag) {
        if (GuiScreen.isShiftKeyDown()) {
            tooltip.add(VALUE_FORMAT + I18n.format(KEY_SHIFT + 1));
        } else {
            tooltip.add(VALUE_FORMAT + I18n.format(KEY_NORMAL));
        }
        tooltip.add(LORE_FORMAT + I18n.format(KEY_LORE));
    }
}
