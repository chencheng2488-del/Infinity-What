package com.bandybus.infwhat.integration.thaumcraft4;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.common.tiles.TileInfusionMatrix;

import java.util.ArrayList;

/**
 * Infusion recipe that only matches while the nearby altar is stable enough.
 *
 * <p>Thaumcraft 4 has no stability value; the altar exposes an instability amount where lower is
 * better. The threshold is therefore expressed as an upper bound on that value.
 */
public class InstabilityGatedInfusionRecipe extends InfusionRecipe {

    /** Search radius around the player for the altar that is about to start the infusion. */
    private static final double ALTAR_SEARCH_RADIUS = 8.0D;

    private final int maximumInstability;

    public InstabilityGatedInfusionRecipe(String research, Object output, int instability,
                                          AspectList aspects, ItemStack central,
                                          ItemStack[] components) {
        this(research, output, instability, aspects, central, 20, components);
    }

    public InstabilityGatedInfusionRecipe(String research, Object output, int instability,
                                          AspectList aspects, ItemStack central,
                                          int maximumInstability, ItemStack[] components) {
        super(research, output, instability, aspects, central, components);
        this.maximumInstability = maximumInstability;
    }

    @Override
    public boolean matches(ArrayList<ItemStack> input, ItemStack central, World world,
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
        for (Object entry : world.loadedTileEntityList) {
            if (!(entry instanceof TileInfusionMatrix)) {
                continue;
            }
            TileInfusionMatrix altar = (TileInfusionMatrix) entry;
            double dx = altar.xCoord + 0.5D - player.posX;
            double dy = altar.yCoord + 0.5D - player.posY;
            double dz = altar.zCoord + 0.5D - player.posZ;
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance > ALTAR_SEARCH_RADIUS * ALTAR_SEARCH_RADIUS
                    || distance > nearestDistance) {
                continue;
            }
            nearest = altar;
            nearestDistance = distance;
        }
        return nearest != null && nearest.instability < this.maximumInstability;
    }
}
