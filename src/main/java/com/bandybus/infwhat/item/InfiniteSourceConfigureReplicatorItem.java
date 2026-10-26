package com.bandybus.infwhat.item;

import com.bandybus.infwhat.block.entity.InfFluidGeneratorBlockEntity;
import com.bandybus.infwhat.block.entity.InfItemGeneratorBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Consumer;

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

        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (context.getPlayer() == null) return InteractionResult.PASS;
        ItemStack card = context.getItemInHand();
        if (context.getPlayer().isShiftKeyDown()) {
            copyConfiguration(card, target, itemTarget);
            show(context, "message.infwhat.replicator.copied", ChatFormatting.GREEN);
            return InteractionResult.CONSUME;
        }

        CompoundTag root = getCustomData(card);
        if (root == null || !root.contains(TAG_CONFIGURATION)) {
            show(context, "message.infwhat.replicator.empty", ChatFormatting.RED);
            return InteractionResult.CONSUME;
        }
        CompoundTag config = root.getCompoundOrEmpty(TAG_CONFIGURATION);
        String expectedType = itemTarget ? "item" : "fluid";
        if (!config.getStringOr(KEY_TYPE, "").equals(expectedType)) {
            show(context, "message.infwhat.replicator.type_mismatch", ChatFormatting.RED);
            return InteractionResult.CONSUME;
        }

        boolean outputEnabled = config.getBooleanOr(KEY_OUTPUT_ENABLED, false);
        int sideMask = config.getIntOr(KEY_OUTPUT_SIDES, 0);
        if (itemTarget) {
            ((InfItemGeneratorBlockEntity) target).applyOutputConfiguration(
                    outputEnabled, sideMask, config.getIntOr(KEY_OUTPUT_RATE, 1),
                    config.getBooleanOr(KEY_INFINITE_MODE, false));
        } else {
            ((InfFluidGeneratorBlockEntity) target).applyOutputConfiguration(
                    outputEnabled, sideMask, config.getLongOr(KEY_OUTPUT_RATE, 1L));
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
        if (context.getPlayer() instanceof net.minecraft.server.level.ServerPlayer player) {
            player.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.translatable(translationKey).withStyle(color)));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        CompoundTag root = getCustomData(stack);
        if (root != null && root.contains(TAG_CONFIGURATION)) {
            String type = root.getCompoundOrEmpty(TAG_CONFIGURATION).getStringOr(KEY_TYPE, "");
            tooltip.accept(Component.translatable("item.infwhat.infinite_source_config_replicator.saved",
                    Component.translatable("item.infwhat.infinite_source_config_replicator.type." + type))
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.accept(Component.translatable("item.infwhat.infinite_source_config_replicator.empty")
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltip.accept(Component.translatable("item.infwhat.infinite_source_config_replicator.instructions")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    private static CompoundTag getCustomData(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData == null ? null : customData.copyTag();
    }
}
