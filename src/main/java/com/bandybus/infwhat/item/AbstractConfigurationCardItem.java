package com.bandybus.infwhat.item;

import com.bandybus.infwhat.api.IConfigurableMachine;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
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

import javax.annotation.Nullable;
import java.util.List;

/**
 * Shared copy/paste protocol for configuration cards.
 *
 * <p>The card stores an opaque settings snapshot produced by a machine. The {@code Type} key inside
 * the snapshot is the machine kind, so a profile can only be pasted onto a machine of the same
 * kind. Subclasses define the root NBT tag, the translation key prefixes and which machines the
 * card accepts.
 *
 * <p>Sneak-use copies and a regular use pastes. The machine blocks call {@link #useOnBlock} before
 * opening their GUI, so the card wins over the screen on both sides.
 */
public abstract class AbstractConfigurationCardItem extends Item {

    private static final String KEY_TYPE = "Type";

    private final String rootTagName;
    private final String messageKeyPrefix;
    private final String tooltipKeyPrefix;

    protected AbstractConfigurationCardItem(String rootTagName, String messageKeyPrefix,
                                            String tooltipKeyPrefix) {
        this.rootTagName = rootTagName;
        this.messageKeyPrefix = messageKeyPrefix;
        this.tooltipKeyPrefix = tooltipKeyPrefix;
    }

    /**
     * Returns the machine this card can read and write.
     *
     * @param tile the clicked tile entity, never {@code null}
     * @return the machine, or {@code null} when the tile is not supported by this card
     */
    protected abstract IConfigurableMachine resolveTarget(TileEntity tile);

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand,
                                      EnumFacing facing, float hitX, float hitY, float hitZ) {
        return useOnBlock(player.getHeldItem(hand), player, worldIn, pos)
                ? EnumActionResult.SUCCESS : EnumActionResult.PASS;
    }

    /**
     * Copies from or pastes onto the machine block at the given position.
     *
     * @return true when this card handles the block and the click was consumed
     */
    public boolean useOnBlock(ItemStack card, EntityPlayer player, World worldIn, BlockPos pos) {
        if (card == null || card.isEmpty()) {
            return false;
        }
        TileEntity tile = worldIn.getTileEntity(pos);
        if (tile == null) {
            return false;
        }
        IConfigurableMachine machine = resolveTarget(tile);
        if (machine == null) {
            return false;
        }
        if (worldIn.isRemote) {
            return true;
        }
        if (player == null) {
            return false;
        }

        if (player.isSneaking()) {
            copyConfiguration(card, machine);
            show(player, "copied", TextFormatting.GREEN);
            return true;
        }

        NBTTagCompound root = card.getTagCompound();
        if (root == null || !root.hasKey(this.rootTagName, 10)) {
            show(player, "empty", TextFormatting.RED);
            return true;
        }
        NBTTagCompound configuration = root.getCompoundTag(this.rootTagName);
        if (!machine.getConfigurationType().equals(configuration.getString(KEY_TYPE))) {
            show(player, "type_mismatch", TextFormatting.RED);
            return true;
        }
        machine.applyConfiguration(configuration);
        show(player, "pasted", TextFormatting.GREEN);
        return true;
    }

    /** Stores a snapshot of the machine settings on the card. */
    private void copyConfiguration(ItemStack card, IConfigurableMachine machine) {
        NBTTagCompound configuration = machine.writeConfiguration();
        configuration.setString(KEY_TYPE, machine.getConfigurationType());
        NBTTagCompound root = card.getTagCompound();
        if (root == null) {
            root = new NBTTagCompound();
            card.setTagCompound(root);
        }
        root.setTag(this.rootTagName, configuration);
    }

    protected final void show(EntityPlayer player, String suffix, TextFormatting color) {
        player.sendStatusMessage(
                new TextComponentTranslation(this.messageKeyPrefix + "." + suffix)
                        .setStyle(new Style().setColor(color)), true);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip,
                               ITooltipFlag flag) {
        super.addInformation(stack, worldIn, tooltip, flag);
        NBTTagCompound root = stack.getTagCompound();
        if (root != null && root.hasKey(this.rootTagName, 10)) {
            String type = root.getCompoundTag(this.rootTagName).getString(KEY_TYPE);
            tooltip.add(I18n.format(this.tooltipKeyPrefix + ".saved",
                    I18n.format(this.tooltipKeyPrefix + ".type." + type)));
        } else {
            tooltip.add(I18n.format(this.tooltipKeyPrefix + ".empty"));
        }
        tooltip.add(I18n.format(this.tooltipKeyPrefix + ".instructions"));
    }
}
