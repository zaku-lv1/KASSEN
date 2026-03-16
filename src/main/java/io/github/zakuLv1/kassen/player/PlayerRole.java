package io.github.zakuLv1.kassen.player;

/**
 * Player roles in the KASSEN game.
 */
public enum PlayerRole {

    /** Attacker - focuses on eliminating enemies. */
    ATTACKER("アタッカー", "敵撃破"),

    /** Tank - maintains the front line. */
    TANK("タンク", "前線維持"),

    /** Support - assists teammates. */
    SUPPORT("サポート", "味方支援");

    private final String displayName;
    private final String description;

    PlayerRole(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
