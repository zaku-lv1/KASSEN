package io.github.zakuLv1.kassen.map;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Represents a jump pad (ジャンプ台) that propels players toward the front line.
 * Jump pads are placed near castles and allow rapid deployment to the battlefield.
 */
public class JumpPad {

    /** Default launch velocity multiplier. */
    private static final double DEFAULT_POWER = 2.0;

    private final Location location;
    private final Vector launchVector;
    private final double triggerRadius;

    public JumpPad(Location location, Vector launchVector, double triggerRadius) {
        this.location = location;
        this.launchVector = launchVector;
        this.triggerRadius = triggerRadius;
    }

    /**
     * Creates a jump pad with default settings that launches the player
     * in the given direction, specified as separate X and Z components.
     *
     * @param location position of the jump pad
     * @param dirX     X component of the launch direction
     * @param dirZ     Z component of the launch direction
     */
    public JumpPad(Location location, double dirX, double dirZ) {
        this(location, new Vector(dirX * DEFAULT_POWER, 1.2, dirZ * DEFAULT_POWER), 1.5);
    }

    /**
     * Launches the player if they are standing on this jump pad.
     *
     * @param player player to check and potentially launch
     * @return true if the player was launched
     */
    public boolean tryLaunch(Player player) {
        if (player.getLocation().distanceSquared(location) <= triggerRadius * triggerRadius) {
            player.setVelocity(launchVector.clone());
            return true;
        }
        return false;
    }

    /**
     * Returns whether a location is within trigger range of this jump pad.
     */
    public boolean isInRange(Location loc) {
        if (!loc.getWorld().equals(location.getWorld())) {
            return false;
        }
        return loc.distanceSquared(location) <= triggerRadius * triggerRadius;
    }

    public Location getLocation() {
        return location;
    }

    public Vector getLaunchVector() {
        return launchVector;
    }

    public double getTriggerRadius() {
        return triggerRadius;
    }
}
