package com.bandybus.infwhat.client.gui;

/** Top-level pages of the generator GUI; the face detail view is a sub-page of I/O. */
public enum GeneratorPage {
    MAIN,
    IO_LIST,
    FACE_DETAIL,
    REDSTONE,
    BATCH;

    public boolean isIoPage() {
        return this == IO_LIST || this == FACE_DETAIL;
    }

    public boolean isFaceDetail() {
        return this == FACE_DETAIL;
    }

    public boolean isRedstone() {
        return this == REDSTONE;
    }

    public boolean isBatch() {
        return this == BATCH;
    }

    public boolean isSecondary() {
        return this != MAIN;
    }

    /** Pages that keep the player inventory visible (the face detail, redstone and batch pages use their own textures). */
    public boolean showsInventory() {
        return this == MAIN || this == IO_LIST;
    }
}
