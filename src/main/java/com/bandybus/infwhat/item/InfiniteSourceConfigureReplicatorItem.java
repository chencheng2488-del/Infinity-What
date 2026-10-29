package com.bandybus.infwhat.item;

import com.bandybus.infwhat.block.entity.InfFluidGeneratorBlockEntity;
import com.bandybus.infwhat.block.entity.InfItemGeneratorBlockEntity;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.List;

/** Sneak-use copies a generator configuration; regular use pastes it onto a matching generator. */
public class InfiniteSourceConfigureReplicatorItem extends Item {
    private static final String TAG_CONFIGURATION = "Configuration";
    private static final String KEY_TYPE = "Type";
    private static final String KEY_OUTPUT_ENABLED = "OutputEnabled";
    private static final String KEY_OUTPUT_SIDES = "OutputSides";
    private static final String KEY_OUTPUT_RATE = "OutputRate";
    private static final String KEY_INFINITE_MODE = "InfiniteMode";

    public InfiniteSourceConfigureReplicatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public ActionResultType useOn(ItemUseContext context) {
        World level = context.getLevel();
        net.minecraft.tileentity.TileEntity target = level.getBlockEntity(context.getClickedPos());
        boolean itemTarget = target instanceof InfItemGeneratorBlockEntity;
        boolean fluidTarget = target instanceof InfFluidGeneratorBlockEntity;
        if (!itemTarget && !fluidTarget) {
            return ActionResultType.PASS;
        }

        if (level.isClientSide) {
            return ActionResultType.SUCCESS;
        }
        if (context.getPlayer() == null) {
            return ActionResultType.PASS;
        }
        ItemStack card = context.getItemInHand();
        if (context.getPlayer().isShiftKeyDown()) {
            copyConfiguration(card, target, itemTarget);
            show(context, "message.infwhat.replicator.copied", TextFormatting.GREEN);
            return ActionResultType.CONSUME;
        }

        CompoundNBT root = card.getTag();
        if (root == null || !root.contains(TAG_CONFIGURATION, Constants.NBT.TAG_COMPOUND)) {
            show(context, "message.infwhat.replicator.empty", TextFormatting.RED);
            return ActionResultType.CONSUME;
        }
        CompoundNBT config = root.getCompound(TAG_CONFIGURATION);
        String expectedType = itemTarget ? "item" : "fluid";
        if (!config.getString(KEY_TYPE).equals(expectedType)) {
            show(context, "message.infwhat.replicator.type_mismatch", TextFormatting.RED);
            return ActionResultType.CONSUME;
        }

        boolean outputEnabled = config.getBoolean(KEY_OUTPUT_ENABLED);
        int sideMask = config.getInt(KEY_OUTPUT_SIDES);
        if (itemTarget) {
            ((InfItemGeneratorBlockEntity) target).applyOutputConfiguration(
                    outputEnabled, sideMask, config.getInt(KEY_OUTPUT_RATE),
                    config.getBoolean(KEY_INFINITE_MODE));
        } else {
            ((InfFluidGeneratorBlockEntity) target).applyOutputConfiguration(
                    outputEnabled, sideMask, config.getLong(KEY_OUTPUT_RATE));
        }
        show(context, "message.infwhat.replicator.pasted", TextFormatting.GREEN);
        return ActionResultType.CONSUME;
    }

    private static void copyConfiguration(ItemStack card, net.minecraft.tileentity.TileEntity target,
                                          boolean itemTarget) {
        CompoundNBT config = new CompoundNBT();
        if (itemTarget) {
            InfItemGeneratorBlockEntity generator = (InfItemGeneratorBlockEntity) target;
            config.putString(KEY_TYPE, "item");
            config.putBoolean(KEY_OUTPUT_ENABLED, generator.isOutputEnabled());
            config.putInt(KEY_OUTPUT_SIDES, generator.getOutputSideMask());
            config.putInt(KEY_OUTPUT_RATE, generator.getOutputItemsPerTick());
            config.putBoolean(KEY_INFINITE_MODE, generator.isInfiniteModeEnabled());
        } else {
            InfFluidGeneratorBlockEntity generator = (InfFluidGeneratorBlockEntity) target;
            config.putString(KEY_TYPE, "fluid");
            config.putBoolean(KEY_OUTPUT_ENABLED, generator.isOutputEnabled());
            config.putInt(KEY_OUTPUT_SIDES, generator.getOutputSideMask());
            config.putLong(KEY_OUTPUT_RATE, generator.getOutputAmountPerTick());
        }
        card.getOrCreateTag().put(TAG_CONFIGURATION, config);
    }

    private static void show(ItemUseContext context, String translationKey, TextFormatting color) {
        context.getPlayer().displayClientMessage(
                new TranslationTextComponent(translationKey).withStyle(color), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World level,
                                List<ITextComponent> tooltip, ITooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        CompoundNBT root = stack.getTag();
        if (root != null && root.contains(TAG_CONFIGURATION, Constants.NBT.TAG_COMPOUND)) {
            String type = root.getCompound(TAG_CONFIGURATION).getString(KEY_TYPE);
            tooltip.add(new TranslationTextComponent("item.infwhat.infinite_source_config_replicator.saved",
                    new TranslationTextComponent("item.infwhat.infinite_source_config_replicator.type." + type))
                    .withStyle(TextFormatting.GRAY));
        } else {
            tooltip.add(new TranslationTextComponent("item.infwhat.infinite_source_config_replicator.empty")
                    .withStyle(TextFormatting.GRAY));
        }
        tooltip.add(new TranslationTextComponent("item.infwhat.infinite_source_config_replicator.instructions")
                .withStyle(TextFormatting.DARK_GRAY));
    }
}
