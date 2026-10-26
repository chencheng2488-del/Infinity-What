package com.bandybus.infwhat.block;

import com.bandybus.infwhat.block.entity.InfItemGeneratorBlockEntity;
import com.bandybus.infwhat.block.entity.ModBlockEntities;
import com.bandybus.infwhat.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import com.mojang.serialization.MapCodec;


/**
 * The Infinity Item Generator block. It mirrors the Infinity Fluid Generator: right-clicking
 * opens a small UI, while every side exposes an {@code ItemStorage} so pipe mods can pull from
 * it. The server tick handles only the explicitly configured active output; redstone does not
 * affect this block.
 */
public class InfItemGeneratorBlock extends BaseEntityBlock {
    public static final MapCodec<InfItemGeneratorBlock> CODEC = simpleCodec(InfItemGeneratorBlock::new);

    public InfItemGeneratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InfItemGeneratorBlockEntity(pos, state);
    }
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(blockEntityType, ModBlockEntities.INF_ITEM_GENERATOR,
                InfItemGeneratorBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                BlockHitResult hit) {
        if (player.getMainHandItem().is(ModItems.INFINITE_SOURCE_CONFIG_REPLICATOR)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof InfItemGeneratorBlockEntity generator) {
                serverPlayer.openMenu(generator);
            }
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    /**
     * Drops the template item that is currently stored in the generator when a player breaks the
     * block, so it is never silently deleted. The item is locked while infinite mode is enabled,
     * and this hook restores it either way.
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof InfItemGeneratorBlockEntity generator) {
                ItemStack stored = generator.getStoredItem();
                if (!stored.isEmpty()) {
                    popResource(level, pos, stored.copy());
                }
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
