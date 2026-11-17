package com.bandybus.infwhat.integration.thaumcraft4;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.common.config.ConfigItems;
import thaumcraft.common.items.wands.ItemWandCasting;

/**
 * Bridges the two Thaumcraft 4 resource families used by the arcane machines.
 *
 * <p>An "aspect sample" is a shard (primal aspect) or an ethereal essence (single stored aspect).
 * Aspect storage items are wands (vis) and ethereal essence. Essentia storage items are phials and
 * ethereal essence. Every read/write goes through Thaumcraft's own item APIs.
 *
 * <p>Thaumcraft 4.1.0g has no {@code itemCrystalEssence}; the equivalent item is
 * {@code itemWispEssence}, which also implements {@link IEssentiaContainerItem}.
 */
public final class ArcaneMachineHelper {

    private ArcaneMachineHelper() {
    }

    /** Aspect encoded by a sample item, or null when the item cannot identify one. */
    public static Aspect sampleAspect(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return null;
        }
        if (stack.getItem() == ConfigItems.itemShard) {
            switch (stack.getItemDamage()) {
                case 0:
                    return Aspect.AIR;
                case 1:
                    return Aspect.FIRE;
                case 2:
                    return Aspect.WATER;
                case 3:
                    return Aspect.EARTH;
                case 4:
                    return Aspect.ORDER;
                case 5:
                    return Aspect.ENTROPY;
                default:
                    return null;
            }
        }
        return firstAspect(stack, ConfigItems.itemWispEssence);
    }

    /** Whether the stack can be used to select an aspect for the replicator. */
    public static boolean isAspectSample(ItemStack stack) {
        return sampleAspect(stack) != null;
    }

    /** Aspect encoded by a filled phial or ethereal essence, used by the emanator. */
    public static Aspect essentiaSampleAspect(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return null;
        }
        if (stack.getItem() != ConfigItems.itemEssence
                && stack.getItem() != ConfigItems.itemWispEssence) {
            return null;
        }
        return firstAspect(stack, stack.getItem());
    }

    /** Whether the stack is a filled phial/ethereal essence sample. */
    public static boolean isEssentiaSample(ItemStack stack) {
        return essentiaSampleAspect(stack) != null;
    }

    /** Whether the item can receive the locked aspect (wand vis or ethereal essence). */
    public static boolean canStoreAspect(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return false;
        }
        return stack.getItem() instanceof ItemWandCasting
                || stack.getItem() == ConfigItems.itemWispEssence;
    }

    /**
     * Fills one aspect storage item. Wands are topped up to their maximum vis; ethereal essence is
     * written with one unit of the locked aspect. Returns true when the stack changed.
     */
    public static boolean fillAspect(ItemStack stack, Aspect aspect) {
        if (stack == null || aspect == null) {
            return false;
        }
        if (stack.getItem() instanceof ItemWandCasting) {
            if (!aspect.isPrimal()) {
                return false;
            }
            ItemWandCasting wand = (ItemWandCasting) stack.getItem();
            int maximum = wand.getMaxVis(stack);
            if (wand.getVis(stack, aspect) >= maximum) {
                return false;
            }
            wand.storeVis(stack, aspect, maximum);
            return true;
        }
        if (stack.getItem() == ConfigItems.itemWispEssence) {
            return writeSingleAspect(stack, aspect, 1);
        }
        return false;
    }

    /** Whether the item can receive essentia (phial or ethereal essence). */
    public static boolean canStoreEssentia(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return false;
        }
        return stack.getItem() == ConfigItems.itemEssence
                || stack.getItem() == ConfigItems.itemWispEssence;
    }

    /** Fills one empty phial/ethereal essence with the locked aspect. */
    public static boolean fillEssentia(ItemStack stack, Aspect aspect) {
        if (!canStoreEssentia(stack) || aspect == null) {
            return false;
        }
        if (stack.getItem() == ConfigItems.itemEssence) {
            return fillPhial(stack, aspect);
        }
        return writeSingleAspect(stack, aspect, 1);
    }

    /** Reads the first aspect of an essentia container item, restricted to one item type. */
    private static Aspect firstAspect(ItemStack stack, Item expectedItem) {
        if (stack == null || stack.getItem() != expectedItem
                || !(stack.getItem() instanceof IEssentiaContainerItem)) {
            return null;
        }
        AspectList list = ((IEssentiaContainerItem) stack.getItem()).getAspects(stack);
        if (list == null || list.size() == 0) {
            return null;
        }
        Aspect[] aspects = list.getAspects();
        return aspects.length == 0 ? null : aspects[0];
    }

    /**
     * TC4.1 phials hold eight units; the filled overlay is drawn for metadata 1, so both the
     * metadata and the aspect NBT are written here.
     */
    private static boolean fillPhial(ItemStack stack, Aspect aspect) {
        int metadata = stack.getItemDamage();
        IEssentiaContainerItem container = (IEssentiaContainerItem) stack.getItem();
        AspectList current = container.getAspects(stack);
        if (current != null && current.size() > 0) {
            Aspect existing = current.getAspects()[0];
            if (existing != null && existing != aspect) {
                return false;
            }
            if (existing == aspect && current.getAmount(aspect) >= 8) {
                return false;
            }
        }
        if (metadata != 0 && metadata != 1) {
            return false;
        }
        stack.setItemDamage(1);
        container.setAspects(stack, new AspectList().add(aspect, 8));
        return true;
    }

    /** Writes an aspect into a generic container without overwriting a different stored aspect. */
    private static boolean writeSingleAspect(ItemStack stack, Aspect aspect, int amount) {
        if (!(stack.getItem() instanceof IEssentiaContainerItem)) {
            return false;
        }
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
