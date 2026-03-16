package io.github.zakuLv1.kassen.map;

import io.github.zakuLv1.kassen.game.GameTeam;
import org.bukkit.Location;

/**
 * Represents a castle (天守閣) – the final stronghold of each team.
 * When a castle's HP reaches zero, that team loses the round.
 * Castles also serve as the primary respawn location.
 */
public class Castle {

    private final GameTeam ownerTeam;
    private Location location;
    private double maxHp;
    private double currentHp;
    private boolean destroyed;

    public Castle(GameTeam ownerTeam, double maxHp) {
        this.ownerTeam = ownerTeam;
        this.maxHp = maxHp;
        this.currentHp = maxHp;
        this.destroyed = false;
    }

    /**
     * Applies damage to the castle.
     *
     * @param damage amount of damage
     * @return true if the castle was destroyed by this damage
     */
    public boolean takeDamage(double damage) {
        if (destroyed) {
            return false;
        }
        currentHp -= damage;
        if (currentHp <= 0) {
            currentHp = 0;
            destroyed = true;
            return true;
        }
        return false;
    }

    /**
     * Resets the castle for a new round.
     */
    public void reset() {
        currentHp = maxHp;
        destroyed = false;
    }

    /**
     * Returns the remaining HP as a percentage (0–100).
     */
    public int getHpPercent() {
        if (maxHp <= 0) return 0;
        return (int) ((currentHp / maxHp) * 100);
    }

    public GameTeam getOwnerTeam() {
        return ownerTeam;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public double getCurrentHp() {
        return currentHp;
    }

    public double getMaxHp() {
        return maxHp;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    /**
     * Returns the display name of this castle.
     */
    public String getDisplayName() {
        return ownerTeam.getDisplayName() + "天守閣";
    }
}
