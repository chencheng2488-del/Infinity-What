package com.bandybus.infwhat.creative;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.ModBlocks;
import com.bandybus.infwhat.item.ModItems;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.minecraft.util.Identifier;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;

/** Dedicated creative tab for all Infinity What items. */
public final class ModCreativeTabs {

    public static ItemGroup INFINITY_WHAT;

    public static void register() {
        // 1.19.2 has no per-tab registry: Fabric's item-group builder appends the tab to
        // ItemGroup.TABS (expanding the array when needed) when build() is called, and the
        // display name resolves to "itemGroup.infwhat.infinity_what".
        INFINITY_WHAT = FabricItemGroupBuilder.create(
                        new Identifier(InfinityWhat.MOD_ID, "infinity_what"))
                .icon(() -> new ItemStack(ModBlocks.INF_ITEM_GENERATOR))
                .appendItems(stacks -> {
                    stacks.add(new ItemStack(ModBlocks.INF_FLUID_GENERATOR));
                    stacks.add(new ItemStack(ModBlocks.INF_ITEM_GENERATOR));
                    stacks.add(new ItemStack(ModItems.INFINITE_SOURCE_CONFIG_REPLICATOR));
                })
                .build();
    }

    private ModCreativeTabs() {
    }
}
