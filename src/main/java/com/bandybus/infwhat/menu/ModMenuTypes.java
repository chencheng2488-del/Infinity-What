package com.bandybus.infwhat.menu;

import com.bandybus.infwhat.InfinityWhat;
import com.bandybus.infwhat.block.entity.InfFluidGeneratorBlockEntity;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.inventory.MenuType;

public class ModMenuTypes {
    // 1.20.5 的 ExtendedScreenHandlerType 只接受 RegistryFriendlyByteBuf 编码器，
    // 而 BlockPos.STREAM_CODEC 的泛型上界是 ByteBuf，需要在此显式收窄。
    private static final StreamCodec<RegistryFriendlyByteBuf, BlockPos> BLOCK_POS_CODEC =
            StreamCodec.of((buf, pos) -> buf.writeBlockPos(pos), buf -> buf.readBlockPos());

    public static final MenuType<InfFluidGeneratorMenu> INF_FLUID_GENERATOR =
            Registry.register(BuiltInRegistries.MENU, InfinityWhat.id("inf_fluid_generator"),
                    new ExtendedScreenHandlerType<>(InfFluidGeneratorMenu::new, BLOCK_POS_CODEC));

    public static final MenuType<InfItemGeneratorMenu> INF_ITEM_GENERATOR =
            Registry.register(BuiltInRegistries.MENU, InfinityWhat.id("inf_item_generator"),
                    new ExtendedScreenHandlerType<>(InfItemGeneratorMenu::new, BLOCK_POS_CODEC));

    public static void register() {
    }

    private ModMenuTypes() {
    }
}
