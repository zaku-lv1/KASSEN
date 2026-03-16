package io.github.zakuLv1.kassen.game;

/**
 * The three lanes present on the KASSEN map.
 */
public enum Lane {

    /** Top lane. */
    TOP("Topレーン"),

    /** Middle lane. */
    MID("Midレーン"),

    /** Bottom lane. */
    BOT("Botレーン");

    private final String displayName;

    Lane(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
