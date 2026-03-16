package io.github.zakuLv1.kassen.map;

import io.github.zakuLv1.kassen.game.GameTeam;
import io.github.zakuLv1.kassen.game.Lane;
import org.bukkit.Location;

/**
 * Represents a fortress (櫓) on the KASSEN map.
 * Each fortress is associated with a lane (Top or Bot) and can be
 * captured by either team after the guarding mini boss is defeated.
 * A captured fortress provides a forward respawn point and advances
 * the controlling team's front line toward the enemy castle.
 */
public class Fortress {

    /** Capture state of the fortress. */
    public enum FortressState {
        NEUTRAL,
        TEAM_A,
        TEAM_B
    }

    private final Lane lane;
    private Location location;
    private FortressState state;
    private boolean bossAlive;
    private boolean beingCaptured;
    private int captureProgress;
    private final int captureTime;
    private GameTeam capturingTeam;

    public Fortress(Lane lane, int captureTime) {
        this.lane = lane;
        this.captureTime = captureTime;
        this.state = FortressState.NEUTRAL;
        this.bossAlive = true;
        this.beingCaptured = false;
        this.captureProgress = 0;
    }

    /**
     * Called when the mini boss guarding this fortress is defeated.
     */
    public void onBossDefeated() {
        this.bossAlive = false;
    }

    /**
     * Returns whether this fortress can be captured.
     * Requires the boss to be dead first.
     *
     * @return true if the fortress can be captured
     */
    public boolean canBeCaptured() {
        return !bossAlive;
    }

    /**
     * Called each tick when a player is capturing this fortress.
     *
     * @param team the team attempting capture
     * @return true if capture is complete
     */
    public boolean tickCapture(GameTeam team) {
        if (bossAlive) {
            return false;
        }
        if (capturingTeam != team) {
            capturingTeam = team;
            captureProgress = 0;
        }
        captureProgress++;
        if (captureProgress >= captureTime) {
            completeCapture(team);
            return true;
        }
        return false;
    }

    /**
     * Interrupts the current capture progress.
     */
    public void interruptCapture() {
        beingCaptured = false;
        captureProgress = 0;
        capturingTeam = null;
    }

    private void completeCapture(GameTeam team) {
        if (team == null) return;
        switch (team.getId()) {
            case 0 -> state = FortressState.TEAM_A;
            case 1 -> state = FortressState.TEAM_B;
            default -> state = FortressState.NEUTRAL;
        }
        beingCaptured = false;
        captureProgress = 0;
        capturingTeam = null;
    }

    /**
     * Resets the fortress for a new round.
     */
    public void reset() {
        state = FortressState.NEUTRAL;
        bossAlive = true;
        beingCaptured = false;
        captureProgress = 0;
        capturingTeam = null;
    }

    /**
     * Returns whether this fortress is captured by the specified team.
     *
     * @param team team to check
     * @return true if the fortress belongs to that team
     */
    public boolean isCapturedBy(GameTeam team) {
        if (team == null) return false;
        return switch (team.getId()) {
            case 0 -> state == FortressState.TEAM_A;
            case 1 -> state == FortressState.TEAM_B;
            default -> false;
        };
    }

    /**
     * Returns whether this fortress is captured by any team.
     */
    public boolean isCaptured() {
        return state != FortressState.NEUTRAL;
    }

    public Lane getLane() {
        return lane;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public FortressState getState() {
        return state;
    }

    public boolean isBossAlive() {
        return bossAlive;
    }

    public boolean isBeingCaptured() {
        return beingCaptured;
    }

    public void setBeingCaptured(boolean beingCaptured) {
        this.beingCaptured = beingCaptured;
    }

    public int getCaptureProgress() {
        return captureProgress;
    }

    public int getCaptureTime() {
        return captureTime;
    }

    public GameTeam getCapturingTeam() {
        return capturingTeam;
    }

    /**
     * Returns the display name of the fortress.
     */
    public String getDisplayName() {
        return lane.getDisplayName() + "櫓";
    }
}
