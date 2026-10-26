package com.bandybus.infwhat.item;

import com.bandybus.infwhat.block.entity.InfFluidGeneratorBlockEntity;
import com.bandybus.infwhat.block.entity.InfItemGeneratorBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

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
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockEntity target = level.getBlockEntity(context.getClickedPos());
        boolean itemTarget = target instanceof InfItemGeneratorBlockEntity;
        boolean fluidTarget = target instanceof InfFluidGeneratorBlockEntity;
        if (!itemTarget && !fluidTarget) return InteractionResult.PASS;

        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (context.getPlayer() == null) return InteractionResult.PASS;
        ItemStack card = context.getItemInHand();
        if (context.getPlayer().isShiftKeyDown()) {
            copyConfiguration(card, target, itemTarget);
            show(context, "message.infwhat.replicator.copied", ChatFormatting.GREEN);
            return InteractionResult.CONSUME;
        }

        CompoundTag root = getCustomData(card);
        if (root == null || !root.contains(TAG_CONFIGURATION, CompoundTag.TAG_COMPOUND)) {
            show(context, "message.infwhat.replicator.empty", ChatFormatting.RED);
            return InteractionResult.CONSUME;
        }
        CompoundTag config = root.getCompound(TAG_CONFIGURATION);
        String expectedType = itemTarget ? "item" : "fluid";
        if (!config.getString(KEY_TYPE).equals(expectedType)) {
            show(context, "message.infwhat.replicator.type_mismatch", ChatFormatting.RED);
            return InteractionResult.CONSUME;
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
        show(context, "message.infwhat.replicator.pasted", ChatFormatting.GREEN);
        return InteractionResult.CONSUME;
    }

    private static void copyConfiguration(ItemStack card, BlockEntity target, boolean itemTarget) {
        CompoundTag config = new CompoundTag();
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
        CustomData.update(DataComponents.CUSTOM_DATA, card, tag -> tag.put(TAG_CONFIGURATION, config));
    }

    private static void show(UseOnContext context, String translationKey, ChatFormatting color) {
        context.getPlayer().displayClientMessage(Component.translatable(translationKey).withStyle(color), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        CompoundTag root = getCustomData(stack);
        if (root != null && root.contains(TAG_CONFIGURATION, CompoundTag.TAG_COMPOUND)) {
            String type = root.getCompound(TAG_CONFIGURATION).getString(KEY_TYPE);
            tooltip.add(Component.translatable("item.infwhat.infinite_source_config_replicator.saved",
                    Component.translatable("item.infwhat.infinite_source_config_replicator.type." + type))
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("item.infwhat.infinite_source_config_replicator.empty")
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("item.infwhat.infinite_source_config_replicator.instructions")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    private static CompoundTag getCustomData(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData == null ? null : customData.copyTag();
    }
}
