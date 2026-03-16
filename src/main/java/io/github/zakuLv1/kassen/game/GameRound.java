package io.github.zakuLv1.kassen.game;

import io.github.zakuLv1.kassen.map.KassenMap;

/**
 * Tracks state for the current round within a KASSEN SENGOKU game.
 * A round ends when a team destroys the enemy castle or captures both fortresses.
 */
public class GameRound {

    private final int roundNumber;
    private GameTeam winner;
    private RoundEndReason endReason;

    public enum RoundEndReason {
        /** A team captured one fortress and destroyed the enemy castle. */
        CASTLE_DESTROYED,
        /** A team captured both fortresses. */
        BOTH_FORTRESSES_CAPTURED
    }

    public GameRound(int roundNumber) {
        this.roundNumber = roundNumber;
    }

    /**
     * Checks whether the round win conditions are met for either team.
     * In SENGOKU mode a round is won by:
     *   1) Capturing 1 fortress AND destroying the enemy castle
     *   2) Capturing both fortresses
     *
     * @param map   the game map
     * @param teamA Team A
     * @param teamB Team B
     * @return the winning team, or null if the round is still ongoing
     */
    public GameTeam checkRoundWin(KassenMap map, GameTeam teamA, GameTeam teamB) {
        // Check two-fortress capture for each team
        if (map.getCapturedFortressCount(teamA.getId()) >= 2) {
            winner = teamA;
            endReason = RoundEndReason.BOTH_FORTRESSES_CAPTURED;
            return teamA;
        }
        if (map.getCapturedFortressCount(teamB.getId()) >= 2) {
            winner = teamB;
            endReason = RoundEndReason.BOTH_FORTRESSES_CAPTURED;
            return teamB;
        }
        return null;
    }

    /**
     * Declares this round's winner because the given team destroyed the enemy castle.
     *
     * @param winningTeam team that won by castle destruction
     */
    public void declareCastleDestroyed(GameTeam winningTeam) {
        this.winner = winningTeam;
        this.endReason = RoundEndReason.CASTLE_DESTROYED;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public GameTeam getWinner() {
        return winner;
    }

    public RoundEndReason getEndReason() {
        return endReason;
    }

    public boolean isOver() {
        return winner != null;
    }
}
