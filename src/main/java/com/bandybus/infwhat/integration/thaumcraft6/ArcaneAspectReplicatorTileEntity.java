package com.bandybus.infwhat.integration.thaumcraft6;

import net.minecraft.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aura.AuraHelper;

/**
 * Arcane Aspect Replicator tile entity for Thaumcraft 6.
 *
 * <p>Crystal essence receives the locked aspect and, while infinite mode is active, the chunk aura
 * receives additional vis through {@link AuraHelper}.
 */
@SuppressWarnings("deprecation")
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
        if (this.targetStack.isEmpty() || this.targetStack.getCount() <= 0) {
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
        AuraHelper.addVis(this.world, this.pos, 1.0F);
        if (isOutputEnabled()) {
            pushTargetOutput();
        }
    }
}
