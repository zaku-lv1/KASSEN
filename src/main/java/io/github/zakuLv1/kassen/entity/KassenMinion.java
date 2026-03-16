package io.github.zakuLv1.kassen.entity;

import io.github.zakuLv1.kassen.game.GameTeam;
import io.github.zakuLv1.kassen.game.Lane;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Zombie;

/**
 * Represents a minion (ミニオン) – an AI-controlled foot soldier
 * that marches along a lane and attacks enemies.
 * Minions are implemented as Zombies with custom attributes.
 */
public class KassenMinion {

    private final GameTeam team;
    private final Lane lane;
    private final Zombie entity;
    private int waypointIndex;

    public KassenMinion(GameTeam team, Lane lane, Zombie entity) {
        this.team = team;
        this.lane = lane;
        this.entity = entity;
        this.waypointIndex = 0;
    }

    /**
     * Returns whether this minion's underlying entity is still alive.
     */
    public boolean isAlive() {
        return entity != null && !entity.isDead();
    }

    /**
     * Returns the minion's current target waypoint index along the lane.
     */
    public int getWaypointIndex() {
        return waypointIndex;
    }

    /**
     * Advances to the next waypoint along the lane.
     */
    public void advanceWaypoint() {
        waypointIndex++;
    }

    public GameTeam getTeam() {
        return team;
    }

    public Lane getLane() {
        return lane;
    }

    public Zombie getEntity() {
        return entity;
    }
}
