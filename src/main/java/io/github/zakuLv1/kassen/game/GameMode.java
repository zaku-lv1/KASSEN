package io.github.zakuLv1.kassen.game;

/**
 * Game modes available in KASSEN.
 */
public enum GameMode {

    /** PvE solo mode - single player vs AI. */
    PVE_SOLO("PvEソロ", 1, 1),

    /** PvP 1v1 mode - individual duel. */
    PVP_1V1("PvP 1vs1", 1, 1),

    /** SENGOKU mode - 3v3 team strategic battle. */
    SENGOKU("PvP SENGOKU", 3, 2),

    /** Battle Royale mode - 7v7 team battle. */
    BATTLE_ROYALE("PvP バトルロイヤル", 7, 2);

    private final String displayName;
    private final int playersPerTeam;
    private final int teamCount;

    GameMode(String displayName, int playersPerTeam, int teamCount) {
        this.displayName = displayName;
        this.playersPerTeam = playersPerTeam;
        this.teamCount = teamCount;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getPlayersPerTeam() {
        return playersPerTeam;
    }

    public int getTeamCount() {
        return teamCount;
    }

    public int getMaxPlayers() {
        return playersPerTeam * teamCount;
    }
}
