package com.bandybus.infwhat.item;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.translation.I18n;

/**
 * ItemBlock for the 1.9.x branch. Vanilla appends ".name" to the block's unlocalized name and
 * looks that up in the common language map; on these versions the suffixed key is not always
 * resolved, so the plain block key (which is present in both en_US and zh_CN) is translated
 * directly instead.
 */
public class InfinityItemBlock extends ItemBlock {

    public InfinityItemBlock(Block block) {
        super(block);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return I18n.translateToLocal(this.block.getUnlocalizedName()).trim();
    }
}
