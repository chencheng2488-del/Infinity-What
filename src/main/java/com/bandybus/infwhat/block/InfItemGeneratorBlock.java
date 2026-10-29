package com.bandybus.infwhat.block;

import com.bandybus.infwhat.block.entity.InfItemGeneratorBlockEntity;
import com.bandybus.infwhat.block.entity.ModBlockEntities;
import com.bandybus.infwhat.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
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
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

/**
 * The Infinity Item Generator block. It mirrors the Infinity Fluid Generator: right-clicking
 * opens a small UI, while every side exposes a Forge {@code IItemHandler} capability so pipe
 * mods can pull from it. The server tick handles only the explicitly configured active output;
 * redstone does not affect this block.
 */
public class InfItemGeneratorBlock extends BaseEntityBlock {

    public InfItemGeneratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InfItemGeneratorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(blockEntityType, ModBlockEntities.INF_ITEM_GENERATOR.get(),
                InfItemGeneratorBlockEntity::serverTick);
    }

    @Nullable
    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof MenuProvider provider ? provider : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).is(ModItems.INFINITE_SOURCE_CONFIG_REPLICATOR.get())) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof InfItemGeneratorBlockEntity generator) {
                // 1.18.2 calls this NetworkHooks#openGui (renamed to openScreen in 1.19).
                NetworkHooks.openGui(serverPlayer, generator, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Drops the template item that is currently stored in the generator when a player breaks the
     * block, so it is never silently deleted. The item is locked while infinite mode is enabled,
     * and this hook restores it either way.
     */
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof InfItemGeneratorBlockEntity generator) {
                ItemStack stored = generator.getStoredItem();
                if (!stored.isEmpty()) {
                    popResource(level, pos, stored.copy());
                }
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }
}
