package com.bandybus.infwhat.integration.thaumcraft6;

import net.minecraft.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.api.items.ItemsTC;

/**
 * Bridges Thaumcraft 6 aspect and essentia containers for the arcane machines.
 *
 * <p>Crystal essence selects an aspect and can receive the locked aspect. Phials and crystal
 * essence are the essentia storage items. Thaumcraft 6 has no item-based per-aspect vis storage,
 * so the aspect replicator injects generic vis into the chunk aura with {@code AuraHelper}.
 */
public final class ArcaneMachineHelper {

    private ArcaneMachineHelper() {
    }

    /** Aspect encoded by a crystal essence sample, or null for anything else. */
    public static Aspect sampleAspect(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem() != ItemsTC.crystalEssence
                || !(stack.getItem() instanceof IEssentiaContainerItem)) {
            return null;
        }
        return firstAspect(stack);
    }

    public static boolean isAspectSample(ItemStack stack) {
        return sampleAspect(stack) != null;
    }

    /** Aspect encoded by a filled phial or crystal essence, used by the emanator. */
    public static Aspect essentiaSampleAspect(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof IEssentiaContainerItem)) {
            return null;
        }
        if (stack.getItem() != ItemsTC.phial && stack.getItem() != ItemsTC.crystalEssence) {
            return null;
        }
        return firstAspect(stack);
    }

    public static boolean isEssentiaSample(ItemStack stack) {
        return essentiaSampleAspect(stack) != null;
    }

    /** Crystal essence is the only item that can receive the locked aspect in TC6. */
    public static boolean canStoreAspect(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() == ItemsTC.crystalEssence;
    }

    /** Writes one unit of the locked aspect into crystal essence. */
    public static boolean fillAspect(ItemStack stack, Aspect aspect) {
        if (!canStoreAspect(stack) || aspect == null) {
            return false;
        }
        return writeSingleAspect(stack, aspect, 1);
    }

    /** Phials and crystal essence can receive essentia. */
    public static boolean canStoreEssentia(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return stack.getItem() == ItemsTC.phial || stack.getItem() == ItemsTC.crystalEssence;
    }

    /** Writes essentia into a phial or crystal essence. */
    public static boolean fillEssentia(ItemStack stack, Aspect aspect) {
        if (!canStoreEssentia(stack) || aspect == null) {
            return false;
        }
        if (stack.getItem() == ItemsTC.phial) {
            return fillPhial(stack, aspect);
        }
        return writeSingleAspect(stack, aspect, 1);
    }

    private static Aspect firstAspect(ItemStack stack) {
        IEssentiaContainerItem container = (IEssentiaContainerItem) stack.getItem();
        AspectList list = container.getAspects(stack);
        if (list == null || list.size() == 0) {
            return null;
        }
        Aspect[] aspects = list.getAspects();
        return aspects.length == 0 ? null : aspects[0];
    }

    /**
     * TC6 phials use metadata 1 for the filled model and hold ten units (the same values as
     * {@code ItemPhial.makeFilledPhial}). Writing only NBT would leave the empty model visible.
     */
    private static boolean fillPhial(ItemStack stack, Aspect aspect) {
        int metadata = stack.getMetadata();
        IEssentiaContainerItem container = (IEssentiaContainerItem) stack.getItem();
        AspectList current = container.getAspects(stack);
        if (current != null && current.size() > 0) {
            Aspect existing = current.getAspects()[0];
            if (existing != null && existing != aspect) {
                return false;
            }
            if (existing == aspect && current.getAmount(aspect) >= 10) {
                return false;
            }
        }
        if (metadata != 0 && metadata != 1) {
            return false;
        }
        stack.setItemDamage(1);
        container.setAspects(stack, new AspectList().add(aspect, 10));
        return true;
    }

    private static boolean writeSingleAspect(ItemStack stack, Aspect aspect, int amount) {
        IEssentiaContainerItem container = (IEssentiaContainerItem) stack.getItem();
        AspectList current = container.getAspects(stack);
        if (current != null && current.size() > 0) {
            Aspect existing = current.getAspects()[0];
            if (existing != null && existing != aspect) {
                return false;
            }
            if (existing == aspect && current.getAmount(aspect) >= amount) {
                return false;
            }
        }
        container.setAspects(stack, new AspectList().add(aspect, amount));
        return true;
    }
}
