package com.bandybus.infwhat.menu;

import com.bandybus.infwhat.InfinityWhat;
import net.fabricmc.fabric.api.container.ContainerProviderRegistry;
import net.minecraft.util.Identifier;

/**
 * 1.15.2 Fabric API uses the legacy container registry (fabric-containers-v0) instead of
 * MenuType / ExtendedScreenHandlerFactory. The resource locations are the registration keys
 * for both the server-side menu factory and the client-side screen factory.
 */
public class ModMenuTypes {

    public static final Identifier INF_FLUID_GENERATOR =
            new Identifier(InfinityWhat.MOD_ID, "inf_fluid_generator");
    public static final Identifier INF_ITEM_GENERATOR =
            new Identifier(InfinityWhat.MOD_ID, "inf_item_generator");

    public static void register() {
        ContainerProviderRegistry.INSTANCE.registerFactory(INF_FLUID_GENERATOR,
                (syncId, id, player, data) ->
                        new InfFluidGeneratorMenu(syncId, player.inventory, data));
        ContainerProviderRegistry.INSTANCE.registerFactory(INF_ITEM_GENERATOR,
                (syncId, id, player, data) ->
                        new InfItemGeneratorMenu(syncId, player.inventory, data));
    }

    private ModMenuTypes() {
    }
}
