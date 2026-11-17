package com.bandybus.infwhat.block;

import com.bandybus.infwhat.block.entity.InfFluidGeneratorBlockEntity;
import com.bandybus.infwhat.fluid.InfFluidKind;
import com.bandybus.infwhat.item.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ContainerBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;

/**
 * The Infinity Fluid Generator block. Buckets are handled directly because Forge 27's fluid
 * registry is a placeholder; right-clicking with anything else opens the tank UI.
 */
public class InfFluidGeneratorBlock extends ContainerBlock {

    public InfFluidGeneratorBlock(Block.Properties properties) {
        super(properties);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    /** Harvest metadata lives on the block (Forge 27 has no block-property setters for it). */
    @Override
    public ToolType getHarvestTool(BlockState state) {
        return ToolType.PICKAXE;
    }

    @Override
    public int getHarvestLevel(BlockState state) {
        return 0;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(IBlockReader world) {
        return new InfFluidGeneratorBlockEntity();
    }

    @Nullable
    @Override
    public INamedContainerProvider getContainer(BlockState state, World world, BlockPos pos) {
        TileEntity tileEntity = world.getTileEntity(pos);
        return tileEntity instanceof INamedContainerProvider ? (INamedContainerProvider) tileEntity : null;
    }

    /** A changed neighbor may free capacity or expose a new handler, so retry output immediately. */
    @Override
    public void neighborChanged(BlockState state, World world, BlockPos pos, Block block,
                                BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, world, pos, block, fromPos, isMoving);
        if (!world.isRemote && world.getTileEntity(pos) instanceof InfFluidGeneratorBlockEntity) {
            ((InfFluidGeneratorBlockEntity) world.getTileEntity(pos)).wakeOutput();
        }
    }

    @Override
    public boolean hasComparatorInputOverride(BlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(BlockState state, World world, BlockPos pos) {
        TileEntity tileEntity = world.getTileEntity(pos);
        return tileEntity instanceof InfFluidGeneratorBlockEntity
                ? ((InfFluidGeneratorBlockEntity) tileEntity).getComparatorOutput() : 0;
    }

    @Override
    public boolean onBlockActivated(BlockState state, World world, BlockPos pos,
                                    PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        ItemStack held = player.getHeldItem(hand);
        if (held.getItem() == ModItems.INFINITE_SOURCE_CONFIG_REPLICATOR) {
            return false;
        }
        if (!world.isRemote && player instanceof ServerPlayerEntity) {
            TileEntity tileEntity = world.getTileEntity(pos);
            if (tileEntity instanceof InfFluidGeneratorBlockEntity) {
                InfFluidGeneratorBlockEntity generator = (InfFluidGeneratorBlockEntity) tileEntity;
                // Buckets are handled directly: Forge 1.14.3 has no usable fluid registry.
                InfFluidKind bucketKind = InfFluidKind.byBucket(held.getItem());
                if (bucketKind != null && generator.fillTank(bucketKind, 1000, false) >= 1000) {
                    generator.fillTank(bucketKind, 1000, true);
                    if (!player.abilities.isCreativeMode) {
                        player.setHeldItem(hand, new ItemStack(Items.BUCKET));
                    }
                    return true;
                }
                if (held.getItem() == Items.BUCKET) {
                    InfFluidKind stored = generator.getStoredKind();
                    if (stored != null && generator.drainTank(1000, false) >= 1000) {
                        generator.drainTank(1000, true);
                        if (!player.abilities.isCreativeMode) {
                            player.setHeldItem(hand, new ItemStack(stored.getFilledBucket()));
                        }
                        return true;
                    }
                }
                NetworkHooks.openGui((ServerPlayerEntity) player, generator, pos);
            }
        }
        // Always report success on the client so the arm swings; the server opens the menu.
        return true;
    }
}
