package com.bandybus.infwhat.item;

import com.bandybus.infwhat.api.IConfigurableMachine;
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
@SuppressWarnings("unchecked")
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
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World worldIn, int x, int y,
                             int z, int side, float hitX, float hitY, float hitZ) {
        return useOnBlock(stack, player, worldIn, x, y, z);
    }

    /**
     * Copies from or pastes onto the machine block at the given position.
     *
     * @return true when this card handles the block and the click was consumed
     */
    public boolean useOnBlock(ItemStack card, EntityPlayer player, World worldIn, int x, int y,
                              int z) {
        if (card == null) {
            return false;
        }
        TileEntity tile = worldIn.getTileEntity(x, y, z);
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
            show(player, "copied", EnumChatFormatting.GREEN);
            return true;
        }

        NBTTagCompound root = card.getTagCompound();
        if (root == null || !root.hasKey(this.rootTagName, 10)) {
            show(player, "empty", EnumChatFormatting.RED);
            return true;
        }
        NBTTagCompound configuration = root.getCompoundTag(this.rootTagName);
        if (!machine.getConfigurationType().equals(configuration.getString(KEY_TYPE))) {
            show(player, "type_mismatch", EnumChatFormatting.RED);
            return true;
        }
        machine.applyConfiguration(configuration);
        show(player, "pasted", EnumChatFormatting.GREEN);
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

    /** Sends one coloured chat line to the player. */
    protected final void show(EntityPlayer player, String suffix, EnumChatFormatting color) {
        player.addChatComponentMessage(
                new ChatComponentTranslation(this.messageKeyPrefix + "." + suffix)
                        .setChatStyle(new ChatStyle().setColor(color)));
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
        NBTTagCompound root = stack == null ? null : stack.getTagCompound();
        if (root != null && root.hasKey(this.rootTagName, 10)) {
            String type = root.getCompoundTag(this.rootTagName).getString(KEY_TYPE);
            tooltip.add(StatCollector.translateToLocalFormatted(this.tooltipKeyPrefix + ".saved",
                    StatCollector.translateToLocal(this.tooltipKeyPrefix + ".type." + type)));
        } else {
            tooltip.add(StatCollector.translateToLocal(this.tooltipKeyPrefix + ".empty"));
        }
        tooltip.add(StatCollector.translateToLocal(this.tooltipKeyPrefix + ".instructions"));
    }
}
