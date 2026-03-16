package io.github.zakuLv1.kassen.player;

import io.github.zakuLv1.kassen.KassenPlugin;
import io.github.zakuLv1.kassen.game.GameTeam;
import io.github.zakuLv1.kassen.game.Lane;
import io.github.zakuLv1.kassen.gauge.SpecialGauge;
import org.bukkit.entity.Player;

/**
 * Represents a player participating in a KASSEN game.
 * Tracks lives, special gauge, role, team assignment, and elimination state.
 */
public class KassenPlayer {

    private final Player player;
    private final KassenPlugin plugin;
    private GameTeam team;
    private PlayerRole role;
    private Lane assignedLane;
    private int lives;
    private boolean eliminated;
    private final SpecialGauge specialGauge;

    public KassenPlayer(Player player, KassenPlugin plugin) {
        this.player = player;
        this.plugin = plugin;
        this.lives = 0; // set properly via resetForRound() when the game starts
        this.eliminated = false;
        this.role = PlayerRole.ATTACKER;
        this.specialGauge = new SpecialGauge(plugin);
    }

    /**
     * Called when this player dies in the game.
     * Decrements lives. Sets eliminated when lives reach zero.
     *
     * @return true if the player still has lives remaining
     */
    public boolean onDeath() {
        if (eliminated) {
            return false;
        }
        lives--;
        if (lives <= 0) {
            lives = 0;
            eliminated = true;
        }
        return !eliminated;
    }

    /**
     * Resets this player's state for a new round.
     */
    public void resetForRound(int maxLives) {
        this.lives = maxLives;
        this.eliminated = false;
        this.specialGauge.reset();
    }

    /**
     * Returns whether this player can still respawn
     * (has lives remaining and is not eliminated).
     *
     * @return true if the player can respawn
     */
    public boolean canRespawn() {
        return !eliminated && lives > 0;
    }

    public Player getPlayer() {
        return player;
    }

    public GameTeam getTeam() {
        return team;
    }

    public void setTeam(GameTeam team) {
        this.team = team;
    }

    public PlayerRole getRole() {
        return role;
    }

    public void setRole(PlayerRole role) {
        this.role = role;
    }

    public Lane getAssignedLane() {
        return assignedLane;
    }

    public void setAssignedLane(Lane lane) {
        this.assignedLane = lane;
    }

    public int getLives() {
        return lives;
    }

    public boolean isEliminated() {
        return eliminated;
    }

    public SpecialGauge getSpecialGauge() {
        return specialGauge;
    }
}
