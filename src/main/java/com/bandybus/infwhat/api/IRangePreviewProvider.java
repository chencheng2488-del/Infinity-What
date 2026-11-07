package com.bandybus.infwhat.api;

/**
 * Implemented by arcane machines that can show their working range as a world-space box.
 * The interface deliberately avoids Thaumcraft and Minecraft types so client renderers can
 * detect providers without loading version-specific machine classes.
 */
public interface IRangePreviewProvider {

    /** Whether the range box should stay visible in the world. */
    boolean isRangePreviewEnabled();

    /** Cubic radius in blocks used by the range box. */
    int getRangePreviewRadius();
}
