package com.bandybus.infwhat.item;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.translation.I18n;

/**
 * ItemBlock for the 1.11 branch. Vanilla translates the block's unlocalized name first and then
 * appends ".name", so a block that has a real {@code tile.infwhat.*} entry ends up displaying
 * "name.name". Translating the plain block key directly avoids the double lookup.
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
