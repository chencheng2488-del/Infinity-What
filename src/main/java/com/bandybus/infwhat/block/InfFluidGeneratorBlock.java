package com.bandybus.infwhat.block;

import com.bandybus.infwhat.block.entity.InfFluidGeneratorBlockEntity;
import com.bandybus.infwhat.fluid.InfFluidKind;
import com.bandybus.infwhat.item.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.fml.network.NetworkHooks;

public class InfFluidGeneratorBlock extends BlockContainer {

    public InfFluidGeneratorBlock(Block.Properties properties) {
        super(properties);
    }

    @Override
    public TileEntity createNewTileEntity(IBlockReader world) {
        return new InfFluidGeneratorBlockEntity();
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public ToolType getHarvestTool(IBlockState state) {
        return ToolType.PICKAXE;
    }

    @Override
    public int getHarvestLevel(IBlockState state) {
        return 0;
    }

    /** A changed neighbor may free capacity or expose a new handler, so retry output immediately. */
    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        super.neighborChanged(state, world, pos, block, fromPos);
        if (!world.isRemote && world.getTileEntity(pos) instanceof InfFluidGeneratorBlockEntity) {
            ((InfFluidGeneratorBlockEntity) world.getTileEntity(pos)).wakeOutput();
        }
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        TileEntity tileEntity = world.getTileEntity(pos);
        return tileEntity instanceof InfFluidGeneratorBlockEntity
                ? ((InfFluidGeneratorBlockEntity) tileEntity).getComparatorOutput() : 0;
    }

    @Override
    public boolean onBlockActivated(IBlockState state, World world, BlockPos pos, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        if (held.getItem() == ModItems.INFINITE_SOURCE_CONFIG_REPLICATOR) {
            return false;
        }
        if (!world.isRemote && player instanceof EntityPlayerMP) {
            TileEntity tileEntity = world.getTileEntity(pos);
            if (tileEntity instanceof InfFluidGeneratorBlockEntity) {
                InfFluidGeneratorBlockEntity generator = (InfFluidGeneratorBlockEntity) tileEntity;
                // Buckets are handled directly: Forge 1.13.2 has no usable fluid registry.
                InfFluidKind bucketKind = InfFluidKind.byBucket(held.getItem());
                if (bucketKind != null && generator.fillTank(bucketKind, 1000, false) >= 1000) {
                    generator.fillTank(bucketKind, 1000, true);
                    if (!player.capabilities.isCreativeMode) {
                        player.setHeldItem(hand, new ItemStack(Items.BUCKET));
                    }
                    return true;
                }
                if (held.getItem() == Items.BUCKET) {
                    InfFluidKind stored = generator.getStoredKind();
                    if (stored != null && generator.drainTank(1000, false) >= 1000) {
                        generator.drainTank(1000, true);
                        if (!player.capabilities.isCreativeMode) {
                            player.setHeldItem(hand, new ItemStack(stored.getFilledBucket()));
                        }
                        return true;
                    }
                }
                NetworkHooks.openGui((EntityPlayerMP) player, generator, pos);
            }
        }
        // Always report success on the client so the arm swings; the server opens the menu.
        return true;
    }
}
