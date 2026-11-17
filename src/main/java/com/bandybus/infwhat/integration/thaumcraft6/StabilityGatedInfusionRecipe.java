package com.bandybus.infwhat.integration.thaumcraft6;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.common.tiles.crafting.TileInfusionMatrix;

import java.util.List;

/**
 * Infusion recipe that only matches while the nearby altar is stable enough.
 *
 * <p>Thaumcraft 6 has no per-recipe stability requirement, so the check runs after the vanilla
 * match succeeded and looks for the altar the player is standing at. The altar stores its
 * stability as a float where higher is better (cap 25 by default), so the threshold is expressed
 * directly in that value.
 */
public class StabilityGatedInfusionRecipe extends InfusionRecipe {

    /** Search radius around the player for the altar that is about to start the infusion. */
    private static final double ALTAR_SEARCH_RADIUS_SQ = 8.0D * 8.0D;

    private final float minimumStability;

    public StabilityGatedInfusionRecipe(String research, Object output, int instability,
                                        AspectList aspects, Object central, Object... components) {
        this(research, output, instability, aspects, central, 5.0F, components);
    }

    public StabilityGatedInfusionRecipe(String research, Object output, int instability,
                                        AspectList aspects, Object central, float minimumStability,
                                        Object... components) {
        super(research, output, instability, aspects, central, components);
        this.minimumStability = minimumStability;
    }

    @Override
    public boolean matches(List<ItemStack> input, ItemStack central, World world,
                           EntityPlayer player) {
        if (!super.matches(input, central, world, player)) {
            return false;
        }
        return hasStableAltar(world, player);
    }

    private boolean hasStableAltar(World world, EntityPlayer player) {
        if (world == null || player == null || world.loadedTileEntityList.isEmpty()) {
            return false;
        }
        TileInfusionMatrix nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (TileEntity tile : world.loadedTileEntityList) {
            if (!(tile instanceof TileInfusionMatrix) || tile.isInvalid()) {
                continue;
            }
            double distance = tile.getPos().distanceSq(player.getPosition());
            if (distance > ALTAR_SEARCH_RADIUS_SQ || distance > nearestDistance) {
                continue;
            }
            nearest = (TileInfusionMatrix) tile;
            nearestDistance = distance;
        }
        return nearest != null && nearest.stability > this.minimumStability;
    }
}
