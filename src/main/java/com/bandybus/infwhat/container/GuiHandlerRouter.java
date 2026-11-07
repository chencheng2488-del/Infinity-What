package com.bandybus.infwhat.container;

import com.bandybus.infwhat.integration.thaumcraft6.ThaumcraftIntegration;
import net.minecraftforge.fml.common.network.IGuiHandler;

/**
 * Chooses the GUI handler at init time. The Thaumcraft handler is loaded reflectively so the core
 * machines keep working when Thaumcraft is absent from the classpath.
 */
public final class GuiHandlerRouter {

    private static final String THAUMCRAFT_HANDLER =
            "com.bandybus.infwhat.integration.thaumcraft6.ThaumcraftGuiHandler";

    private GuiHandlerRouter() {
    }

    public static IGuiHandler create() {
        if (!ThaumcraftIntegration.isActive()) {
            return new ModGuiHandler();
        }
        try {
            Class<?> type = Class.forName(THAUMCRAFT_HANDLER);
            return (IGuiHandler) type.getMethod("create").invoke(null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot create the Thaumcraft GUI handler",
                    exception);
        }
    }
}
