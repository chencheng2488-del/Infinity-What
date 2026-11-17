package com.bandybus.infwhat.item;

import com.bandybus.infwhat.block.RedstoneMode;
import com.bandybus.infwhat.block.entity.InfFluidGeneratorBlockEntity;
import com.bandybus.infwhat.block.entity.InfItemGeneratorBlockEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.world.RayTraceContext;
import net.minecraft.world.World;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.hit.HitResult;

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

    public InfiniteSourceConfigureReplicatorItem(Item.Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockEntity target = world.getBlockEntity(context.getBlockPos());
        boolean itemTarget = target instanceof InfItemGeneratorBlockEntity;
        boolean fluidTarget = target instanceof InfFluidGeneratorBlockEntity;
        if (!itemTarget && !fluidTarget) {
            // Sneak-use on a non-generator block must not fall through to use(Level, PlayerEntity, ...),
            // which is reserved for clearing the card in air.
            return context.getPlayer() != null && context.getPlayer().isSneaking()
                    ? ActionResult.SUCCESS : ActionResult.PASS;
        }

        if (world.isClient) return ActionResult.SUCCESS;
        if (context.getPlayer() == null) return ActionResult.PASS;
        ItemStack card = context.getStack();
        if (context.getPlayer().isSneaking()) {
            copyConfiguration(card, target, itemTarget);
            show(context, "message.infwhat.replicator.copied", Formatting.GREEN);
            return ActionResult.SUCCESS;
        }

        CompoundTag root = card.getTag();
        if (root == null || !root.containsKey(TAG_CONFIGURATION)) {
            show(context, "message.infwhat.replicator.empty", Formatting.RED);
            return ActionResult.SUCCESS;
        }
        CompoundTag config = root.getCompound(TAG_CONFIGURATION);
        String expectedType = itemTarget ? "item" : "fluid";
        if (!config.getString(KEY_TYPE).equals(expectedType)) {
            show(context, "message.infwhat.replicator.type_mismatch", Formatting.RED);
            return ActionResult.SUCCESS;
        }

        boolean outputEnabled = config.getBoolean(KEY_OUTPUT_ENABLED);
        int sideMask = config.getInt(KEY_OUTPUT_SIDES);
        ReplicatorPasteMode pasteMode = getPasteMode(card);
        boolean applied;
        if (itemTarget) {
            InfItemGeneratorBlockEntity generator = (InfItemGeneratorBlockEntity) target;
            switch (pasteMode) {
                case OUTPUT:
                    applied = generator.applyOutputState(outputEnabled, sideMask);
                    break;
                case RATE:
                    applied = generator.applyRates(readItemRates(config));
                    break;
                case REDSTONE:
                    applied = generator.applyRedstoneConfiguration(
                            RedstoneMode.byId(config.getInt(KEY_REDSTONE_MODE)),
                            readPulse(config, KEY_PULSE_ON), readPulse(config, KEY_PULSE_OFF));
                    break;
                case BATCH:
                    applied = generator.setBatchSize(readBatch(config));
                    break;
                case ALL:
                default:
                    applied = generator.applyFullConfiguration(outputEnabled, sideMask,
                            readItemRates(config), config.getBoolean(KEY_INFINITE_MODE))
                            && generator.setBatchSize(readBatch(config));
                    break;
            }
        } else {
            InfFluidGeneratorBlockEntity generator = (InfFluidGeneratorBlockEntity) target;
            switch (pasteMode) {
                case OUTPUT:
                    applied = generator.applyOutputState(outputEnabled, sideMask);
                    break;
                case RATE:
                    applied = generator.applyRates(readFluidRates(config));
                    break;
                case REDSTONE:
                    applied = generator.applyRedstoneConfiguration(
                            RedstoneMode.byId(config.getInt(KEY_REDSTONE_MODE)),
                            readPulse(config, KEY_PULSE_ON), readPulse(config, KEY_PULSE_OFF));
                    break;
                case BATCH:
                    applied = generator.setBatchSize(readBatch(config));
                    break;
                case ALL:
                default:
                    applied = generator.applyFullConfiguration(outputEnabled, sideMask,
                            readFluidRates(config))
                            && generator.setBatchSize(readBatch(config));
                    break;
            }
        }
        if (!applied) {
            show(context, "message.infwhat.replicator.rejected", Formatting.RED);
            return ActionResult.SUCCESS;
        }
        show(context, "message.infwhat.replicator.pasted_mode", Formatting.GREEN,
                modeName(pasteMode));
        return ActionResult.SUCCESS;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack card = player.getStackInHand(hand);
        if (!player.isSneaking()) {
            // Regular air use cycles the paste mode; sneak-air keeps its 2.2 clear action.
            // Ignore the fall-through use() that follows a normal block interaction.
            // A miss still returns a BlockHitResult, so the hit type must be checked instead.
            if (rayTrace(world, player, RayTraceContext.FluidHandling.NONE).getType()
                    != HitResult.Type.MISS) {
                return new TypedActionResult<>(ActionResult.PASS, card);
            }
            if (world.isClient) {
                return new TypedActionResult<>(ActionResult.SUCCESS, card);
            }
            ReplicatorPasteMode mode = getPasteMode(card).next();
            card.getOrCreateTag().putInt(KEY_MODE, mode.ordinal());
            show(player, "message.infwhat.replicator.mode", Formatting.AQUA, modeName(mode));
            return new TypedActionResult<>(ActionResult.SUCCESS, card);
        }
        if (world.isClient) {
            return new TypedActionResult<>(ActionResult.SUCCESS, card);
        }
        CompoundTag root = card.getTag();
        if (root == null || !root.containsKey(TAG_CONFIGURATION)) {
            show(player, "message.infwhat.replicator.empty", Formatting.RED);
            return new TypedActionResult<>(ActionResult.SUCCESS, card);
        }
        root.remove(TAG_CONFIGURATION);
        card.setTag(root);
        show(player, "message.infwhat.replicator.cleared", Formatting.GREEN);
        return new TypedActionResult<>(ActionResult.SUCCESS, card);
    }

    private static ReplicatorPasteMode getPasteMode(ItemStack card) {
        CompoundTag root = card.getTag();
        return root == null ? ReplicatorPasteMode.ALL
                : ReplicatorPasteMode.byId(root.getInt(KEY_MODE));
    }

    private static Text modeName(ReplicatorPasteMode mode) {
        return new TranslatableText(
                "item.infwhat.infinite_source_config_replicator.mode." + mode.key());
    }

    private static int readPulse(CompoundTag config, String key) {
        if (!config.containsKey(key)) {
            return InfItemGeneratorBlockEntity.DEFAULT_PULSE_TICKS;
        }
        return Math.max(InfItemGeneratorBlockEntity.MIN_PULSE_TICKS,
                Math.min(InfItemGeneratorBlockEntity.MAX_PULSE_TICKS, config.getInt(key)));
    }

    private static int readBatch(CompoundTag config) {
        int size = config.containsKey(KEY_BATCH_SIZE)
                ? config.getInt(KEY_BATCH_SIZE) : InfItemGeneratorBlockEntity.DEFAULT_BATCH_SIZE;
        return Math.max(InfItemGeneratorBlockEntity.MIN_BATCH_SIZE,
                Math.min(InfItemGeneratorBlockEntity.MAX_BATCH_SIZE, size));
    }

    private static int[] readItemRates(CompoundTag config) {
        int[] rates = new int[Direction.values().length];
        boolean hasSideRates = config.containsKey(SIDE_RATE_KEYS[0]);
        int legacyRate = Math.max(1, config.containsKey(KEY_OUTPUT_RATE)
                ? config.getInt(KEY_OUTPUT_RATE) : 1);
        for (Direction direction : Direction.values()) {
            int index = direction.getId();
            rates[index] = hasSideRates
                    ? (config.containsKey(SIDE_RATE_KEYS[index])
                            ? config.getInt(SIDE_RATE_KEYS[index]) : legacyRate)
                    : legacyRate;
        }
        return rates;
    }

    private static long[] readFluidRates(CompoundTag config) {
        long[] rates = new long[Direction.values().length];
        boolean hasSideRates = config.containsKey(SIDE_RATE_KEYS[0]);
        long legacyRate = Math.max(1L, config.containsKey(KEY_OUTPUT_RATE)
                ? config.getLong(KEY_OUTPUT_RATE) : 1L);
        for (Direction direction : Direction.values()) {
            int index = direction.getId();
            rates[index] = hasSideRates
                    ? (config.containsKey(SIDE_RATE_KEYS[index])
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
                config.putInt(SIDE_RATE_KEYS[direction.getId()],
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
                config.putLong(SIDE_RATE_KEYS[direction.getId()],
                        generator.getSideRateMb(direction));
            }
        }
        card.getOrCreateTag().put(TAG_CONFIGURATION, config);
    }

    private static String[] createSideRateKeys() {
        Direction[] directions = Direction.values();
        String[] keys = new String[directions.length];
        for (Direction direction : directions) {
            String name = direction.getName();
            keys[direction.getId()] = "SideRate"
                    + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
        return keys;
    }

    private static void show(ItemUsageContext context, String translationKey, Formatting color) {
        context.getPlayer().addChatMessage(
                new TranslatableText(translationKey).formatted(color), true);
    }

    private static void show(ItemUsageContext context, String translationKey, Formatting color,
                             Text argument) {
        context.getPlayer().addChatMessage(
                new TranslatableText(translationKey, argument).formatted(color), true);
    }

    private static void show(PlayerEntity player, String translationKey, Formatting color) {
        player.addChatMessage(
                new TranslatableText(translationKey).formatted(color), true);
    }

    private static void show(PlayerEntity player, String translationKey, Formatting color,
                             Text argument) {
        player.addChatMessage(
                new TranslatableText(translationKey, argument).formatted(color), true);
    }

    @Override
    public void appendTooltip(ItemStack stack, World world,
                                List<Text> tooltip, TooltipContext flag) {
        super.appendTooltip(stack, world, tooltip, flag);
        CompoundTag root = stack.getTag();
        if (root != null && root.containsKey(TAG_CONFIGURATION)) {
            String type = root.getCompound(TAG_CONFIGURATION).getString(KEY_TYPE);
            tooltip.add(new TranslatableText("item.infwhat.infinite_source_config_replicator.saved",
                    new TranslatableText("item.infwhat.infinite_source_config_replicator.type." + type))
                    .formatted(Formatting.GRAY));
        } else {
            tooltip.add(new TranslatableText("item.infwhat.infinite_source_config_replicator.empty")
                    .formatted(Formatting.GRAY));
        }
        ReplicatorPasteMode mode = getPasteMode(stack);
        tooltip.add(new TranslatableText("item.infwhat.infinite_source_config_replicator.mode",
                        modeName(mode))
                .formatted(Formatting.AQUA));
        tooltip.add(new TranslatableText("item.infwhat.infinite_source_config_replicator.fields",
                        new TranslatableText(
                                "item.infwhat.infinite_source_config_replicator.fields." + mode.key()))
                .formatted(Formatting.DARK_GRAY));
        tooltip.add(new TranslatableText("item.infwhat.infinite_source_config_replicator.instructions")
                .formatted(Formatting.DARK_GRAY));
    }
}
