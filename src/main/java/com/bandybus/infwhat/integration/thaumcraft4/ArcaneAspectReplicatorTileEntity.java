package com.bandybus.infwhat.integration.thaumcraft4;

import net.minecraft.item.ItemStack;
import thaumcraft.api.aspects.Aspect;

/**
 * Arcane Aspect Replicator tile entity.
 *
 * <p>Slot 0 selects the aspect (shard or crystal essence); slot 1 receives the locked aspect.
 * Wands are topped up to their maximum vis, crystal essence receives one unit of the aspect.
 */
public class ArcaneAspectReplicatorTileEntity extends AbstractArcaneMachineTileEntity {

    @Override
    protected int defaultOutputRate() {
        return 8;
    }

    @Override
    protected boolean defaultOutputEnabled() {
        return false;
    }

    @Override
    public String getConfigurationType() {
        return "replicator";
    }

    @Override
    protected String getInventoryTranslationKey() {
        return "container.infwhat.arcane_aspect_replicator";
    }

    @Override
    protected boolean isValidSample(ItemStack stack) {
        return ArcaneMachineHelper.isAspectSample(stack);
    }

    @Override
    protected Aspect aspectOfSample(ItemStack stack) {
        return ArcaneMachineHelper.sampleAspect(stack);
    }

    @Override
    protected boolean isValidTarget(ItemStack stack) {
        return ArcaneMachineHelper.canStoreAspect(stack);
    }

    @Override
    protected boolean fillTargetInPlace() {
        if (this.targetStack == null || this.targetStack.stackSize <= 0) {
            return false;
        }
        if (!ArcaneMachineHelper.fillAspect(this.targetStack, this.lockedAspect)) {
            return false;
        }
        markUpdated();
        return true;
    }

    @Override
    protected void onProvidingTick() {
        if (isOutputEnabled()) {
            pushTargetOutput();
        }
    }
}
