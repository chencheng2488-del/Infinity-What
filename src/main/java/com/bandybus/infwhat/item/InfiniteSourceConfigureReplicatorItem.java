package com.bandybus.infwhat.item;

import com.bandybus.infwhat.block.RedstoneMode;
import com.bandybus.infwhat.block.entity.InfFluidGeneratorBlockEntity;
import com.bandybus.infwhat.block.entity.InfItemGeneratorBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.HitResult;

import java.util.List;

/** Sneak-use copies a generator configuration; regular use pastes it onto a matching generator. */
public class InfiniteSourceConfigureReplicatorItem extends Item {
    private static final String TAG_CONFIGURATION = "Configuration";
    private static final String KEY_MODE = "PasteMode";
    private static final String KEY_TYPE = "Type";
    private static final String KEY_OUTPUT_ENABLED = "OutputEnabled";
    private static final String KEY_OUTPUT_SIDES = "OutputSides";
    private static final String KEY_REDSTONE_MODE = "RedstoneMode";
    private static final String KEY_PULSE_ON = "PulseOnTicks";
    private static final String KEY_PULSE_OFF = "PulseOffTicks";
    private static final String KEY_BATCH_SIZE = "BatchSize";
    /** Legacy single-rate key, only read from cards created before 2.2. */
    private static final String KEY_OUTPUT_RATE = "OutputRate";
    private static final String KEY_INFINITE_MODE = "InfiniteMode";
    private static final String[] SIDE_RATE_KEYS = createSideRateKeys();

    public InfiniteSourceConfigureReplicatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockEntity target = level.getBlockEntity(context.getClickedPos());
        boolean itemTarget = target instanceof InfItemGeneratorBlockEntity;
        boolean fluidTarget = target instanceof InfFluidGeneratorBlockEntity;
        if (!itemTarget && !fluidTarget) {
            // Sneak-use on a non-generator block must not fall through to use(Level, Player, ...),
            // which is reserved for clearing the card in air.
            return context.getPlayer() != null && context.getPlayer().isShiftKeyDown()
                    ? InteractionResult.CONSUME : InteractionResult.PASS;
        }

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
        ReplicatorPasteMode pasteMode = getPasteMode(card);
        boolean applied;
        if (itemTarget) {
            InfItemGeneratorBlockEntity generator = (InfItemGeneratorBlockEntity) target;
            applied = switch (pasteMode) {
                case ALL -> generator.applyFullConfiguration(outputEnabled, sideMask,
                        readItemRates(config), config.getBoolean(KEY_INFINITE_MODE))
                        && generator.setBatchSize(readBatch(config));
                case OUTPUT -> generator.applyOutputState(outputEnabled, sideMask);
                case RATE -> generator.applyRates(readItemRates(config));
                case REDSTONE -> generator.applyRedstoneConfiguration(
                        RedstoneMode.byId(config.getInt(KEY_REDSTONE_MODE)),
                        readPulse(config, KEY_PULSE_ON), readPulse(config, KEY_PULSE_OFF));
                case BATCH -> generator.setBatchSize(readBatch(config));
            };
        } else {
            InfFluidGeneratorBlockEntity generator = (InfFluidGeneratorBlockEntity) target;
            applied = switch (pasteMode) {
                case ALL -> generator.applyFullConfiguration(outputEnabled, sideMask,
                        readFluidRates(config))
                        && generator.setBatchSize(readBatch(config));
                case OUTPUT -> generator.applyOutputState(outputEnabled, sideMask);
                case RATE -> generator.applyRates(readFluidRates(config));
                case REDSTONE -> generator.applyRedstoneConfiguration(
                        RedstoneMode.byId(config.getInt(KEY_REDSTONE_MODE)),
                        readPulse(config, KEY_PULSE_ON), readPulse(config, KEY_PULSE_OFF));
                case BATCH -> generator.setBatchSize(readBatch(config));
            };
        }
        if (!applied) {
            show(context, "message.infwhat.replicator.rejected", ChatFormatting.RED);
            return InteractionResult.CONSUME;
        }
        show(context, "message.infwhat.replicator.pasted_mode", ChatFormatting.GREEN,
                modeName(pasteMode));
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack card = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            // Regular air use cycles the paste mode; sneak-air keeps its 2.2 clear action.
            // Ignore the fall-through use() that follows a normal block interaction.
            // A miss still returns a BlockHitResult, so the hit type must be checked instead.
            if (player.pick(5.0D, 0.0F, false).getType() != HitResult.Type.MISS) {
                return InteractionResultHolder.pass(card);
            }
            if (level.isClientSide) {
                return InteractionResultHolder.success(card);
            }
            ReplicatorPasteMode mode = getPasteMode(card).next();
            CustomData.update(DataComponents.CUSTOM_DATA, card,
                    tag -> tag.putInt(KEY_MODE, mode.ordinal()));
            show(player, "message.infwhat.replicator.mode", ChatFormatting.AQUA, modeName(mode));
            return InteractionResultHolder.consume(card);
        }
        if (level.isClientSide) {
            return InteractionResultHolder.success(card);
        }
        CompoundTag root = getCustomData(card);
        if (root == null || !root.contains(TAG_CONFIGURATION, CompoundTag.TAG_COMPOUND)) {
            show(player, "message.infwhat.replicator.empty", ChatFormatting.RED);
            return InteractionResultHolder.consume(card);
        }
        CustomData.update(DataComponents.CUSTOM_DATA, card, tag -> tag.remove(TAG_CONFIGURATION));
        show(player, "message.infwhat.replicator.cleared", ChatFormatting.GREEN);
        return InteractionResultHolder.consume(card);
    }

    private static ReplicatorPasteMode getPasteMode(ItemStack card) {
        CompoundTag root = getCustomData(card);
        return root == null ? ReplicatorPasteMode.ALL
                : ReplicatorPasteMode.byId(root.getInt(KEY_MODE));
    }

    private static Component modeName(ReplicatorPasteMode mode) {
        return Component.translatable(
                "item.infwhat.infinite_source_config_replicator.mode." + mode.key());
    }

    private static int readPulse(CompoundTag config, String key) {
        if (!config.contains(key, Tag.TAG_INT)) {
            return InfItemGeneratorBlockEntity.DEFAULT_PULSE_TICKS;
        }
        return Math.max(InfItemGeneratorBlockEntity.MIN_PULSE_TICKS,
                Math.min(InfItemGeneratorBlockEntity.MAX_PULSE_TICKS, config.getInt(key)));
    }

    private static int readBatch(CompoundTag config) {
        int size = config.contains(KEY_BATCH_SIZE, Tag.TAG_INT)
                ? config.getInt(KEY_BATCH_SIZE) : InfItemGeneratorBlockEntity.DEFAULT_BATCH_SIZE;
        return Math.max(InfItemGeneratorBlockEntity.MIN_BATCH_SIZE,
                Math.min(InfItemGeneratorBlockEntity.MAX_BATCH_SIZE, size));
    }

    private static int[] readItemRates(CompoundTag config) {
        int[] rates = new int[Direction.values().length];
        boolean hasSideRates = config.contains(SIDE_RATE_KEYS[0], Tag.TAG_INT);
        int legacyRate = Math.max(1, config.contains(KEY_OUTPUT_RATE, Tag.TAG_INT)
                ? config.getInt(KEY_OUTPUT_RATE) : 1);
        for (Direction direction : Direction.values()) {
            int index = direction.get3DDataValue();
            rates[index] = hasSideRates
                    ? (config.contains(SIDE_RATE_KEYS[index], Tag.TAG_INT)
                            ? config.getInt(SIDE_RATE_KEYS[index]) : legacyRate)
                    : legacyRate;
        }
        return rates;
    }

    private static long[] readFluidRates(CompoundTag config) {
        long[] rates = new long[Direction.values().length];
        boolean hasSideRates = config.contains(SIDE_RATE_KEYS[0], Tag.TAG_LONG);
        long legacyRate = Math.max(1L, config.contains(KEY_OUTPUT_RATE, Tag.TAG_LONG)
                ? config.getLong(KEY_OUTPUT_RATE) : 1L);
        for (Direction direction : Direction.values()) {
            int index = direction.get3DDataValue();
            rates[index] = hasSideRates
                    ? (config.contains(SIDE_RATE_KEYS[index], Tag.TAG_LONG)
                            ? config.getLong(SIDE_RATE_KEYS[index]) : legacyRate)
                    : legacyRate;
        }
        return rates;
    }

    private static void copyConfiguration(ItemStack card, BlockEntity target, boolean itemTarget) {
        CompoundTag config = new CompoundTag();
        if (itemTarget) {
            InfItemGeneratorBlockEntity generator = (InfItemGeneratorBlockEntity) target;
            config.putString(KEY_TYPE, "item");
            config.putBoolean(KEY_OUTPUT_ENABLED, generator.isOutputEnabled());
            config.putInt(KEY_OUTPUT_SIDES, generator.getOutputSideMask());
            config.putBoolean(KEY_INFINITE_MODE, generator.isInfiniteModeEnabled());
            config.putInt(KEY_REDSTONE_MODE, generator.getRedstoneMode().ordinal());
            config.putInt(KEY_PULSE_ON, generator.getPulseOnTicks());
            config.putInt(KEY_PULSE_OFF, generator.getPulseOffTicks());
            config.putInt(KEY_BATCH_SIZE, generator.getBatchSize());
            for (Direction direction : Direction.values()) {
                config.putInt(SIDE_RATE_KEYS[direction.get3DDataValue()],
                        generator.getSideRate(direction));
            }
        } else {
            InfFluidGeneratorBlockEntity generator = (InfFluidGeneratorBlockEntity) target;
            config.putString(KEY_TYPE, "fluid");
            config.putBoolean(KEY_OUTPUT_ENABLED, generator.isOutputEnabled());
            config.putInt(KEY_OUTPUT_SIDES, generator.getOutputSideMask());
            config.putInt(KEY_REDSTONE_MODE, generator.getRedstoneMode().ordinal());
            config.putInt(KEY_PULSE_ON, generator.getPulseOnTicks());
            config.putInt(KEY_PULSE_OFF, generator.getPulseOffTicks());
            config.putInt(KEY_BATCH_SIZE, generator.getBatchSize());
            for (Direction direction : Direction.values()) {
                config.putLong(SIDE_RATE_KEYS[direction.get3DDataValue()],
                        generator.getSideRateMb(direction));
            }
        }
        CustomData.update(DataComponents.CUSTOM_DATA, card, tag -> tag.put(TAG_CONFIGURATION, config));
    }

    private static String[] createSideRateKeys() {
        Direction[] directions = Direction.values();
        String[] keys = new String[directions.length];
        for (Direction direction : directions) {
            String name = direction.getName();
            keys[direction.get3DDataValue()] = "SideRate"
                    + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
        return keys;
    }

    private static void show(UseOnContext context, String translationKey, ChatFormatting color) {
        context.getPlayer().displayClientMessage(Component.translatable(translationKey).withStyle(color), true);
    }

    private static void show(UseOnContext context, String translationKey, ChatFormatting color,
                             Component argument) {
        context.getPlayer().displayClientMessage(
                Component.translatable(translationKey, argument).withStyle(color), true);
    }

    private static void show(Player player, String translationKey, ChatFormatting color) {
        player.displayClientMessage(Component.translatable(translationKey).withStyle(color), true);
    }

    private static void show(Player player, String translationKey, ChatFormatting color,
                             Component argument) {
        player.displayClientMessage(
                Component.translatable(translationKey, argument).withStyle(color), true);
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
        ReplicatorPasteMode mode = getPasteMode(stack);
        tooltip.add(Component.translatable("item.infwhat.infinite_source_config_replicator.mode",
                        modeName(mode))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.infwhat.infinite_source_config_replicator.fields",
                        Component.translatable("item.infwhat.infinite_source_config_replicator.fields."
                                + mode.key()))
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.infwhat.infinite_source_config_replicator.instructions")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    private static CompoundTag getCustomData(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData == null ? null : customData.copyTag();
    }
}
