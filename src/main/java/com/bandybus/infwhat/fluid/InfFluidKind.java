package com.bandybus.infwhat.fluid;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

/**
 * The two fluids supported by the 1.13.2 self-contained tank. Forge 25's fluid registry and
 * FluidStack are placeholders in this Minecraft version, so the generator stores the fluid kind
 * and amount itself instead of relying on {@code FluidStack}.
 */
public enum InfFluidKind {

    WATER("water", "fluid.infwhat.water",
            new ResourceLocation("minecraft:block/water_still"),
            0xFF3F76E4, Items.WATER_BUCKET),
    LAVA("lava", "fluid.infwhat.lava",
            new ResourceLocation("minecraft:block/lava_still"),
            0xFFFF8A00, Items.LAVA_BUCKET);

    private final String name;
    private final String translationKey;
    private final ResourceLocation stillTexture;
    private final int color;
    private final Item filledBucket;

    InfFluidKind(String name, String translationKey, ResourceLocation stillTexture,
                 int color, Item filledBucket) {
        this.name = name;
        this.translationKey = translationKey;
        this.stillTexture = stillTexture;
        this.color = color;
        this.filledBucket = filledBucket;
    }

    public String getName() {
        return this.name;
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public ResourceLocation getStillTexture() {
        return this.stillTexture;
    }

    public int getColor() {
        return this.color;
    }

    public Item getFilledBucket() {
        return this.filledBucket;
    }

    public static InfFluidKind byName(String name) {
        if ("water".equals(name)) {
            return WATER;
        }
        if ("lava".equals(name)) {
            return LAVA;
        }
        return null;
    }

    public static InfFluidKind byBucket(Item item) {
        if (item == Items.WATER_BUCKET) {
            return WATER;
        }
        if (item == Items.LAVA_BUCKET) {
            return LAVA;
        }
        return null;
    }
}
