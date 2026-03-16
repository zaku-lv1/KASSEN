package io.github.zakuLv1.kassen.map;

import io.github.zakuLv1.kassen.game.Lane;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Holds configuration for the KASSEN game map.
 * Tracks the world, lane waypoints, fortress locations, castle locations,
 * and jump pad positions.
 * The map is symmetric: Team A (red) starts on one side, Team B (blue) on the other.
 */
public class KassenMap {

    private World world;

    /** Fortress for each non-Mid lane. */
    private final Map<Lane, Fortress> fortresses = new EnumMap<>(Lane.class);

    /** Lane spawn waypoints: starting positions for each lane per team. */
    private final Map<Lane, List<Location>> laneWaypointsTeamA = new EnumMap<>(Lane.class);
    private final Map<Lane, List<Location>> laneWaypointsTeamB = new EnumMap<>(Lane.class);

    /** Jump pads near each castle. */
    private final List<JumpPad> jumpPads = new ArrayList<>();

    public KassenMap() {
        for (Lane lane : new Lane[]{Lane.TOP, Lane.BOT}) {
            laneWaypointsTeamA.put(lane, new ArrayList<>());
            laneWaypointsTeamB.put(lane, new ArrayList<>());
        }
        laneWaypointsTeamA.put(Lane.MID, new ArrayList<>());
        laneWaypointsTeamB.put(Lane.MID, new ArrayList<>());
    }

    /**
     * Registers a fortress at the specified location for the given lane.
     *
     * @param lane     lane the fortress belongs to (TOP or BOT)
     * @param fortress fortress object to register
     */
    public void setFortress(Lane lane, Fortress fortress) {
        fortresses.put(lane, fortress);
    }

    /**
     * Returns the fortress for the given lane, or null if not set.
     */
    public Fortress getFortress(Lane lane) {
        return fortresses.get(lane);
    }

    /**
     * Returns all registered fortresses.
     */
    public List<Fortress> getAllFortresses() {
        return new ArrayList<>(fortresses.values());
    }

    /**
     * Returns the number of fortresses captured by the given team ID.
     *
     * @param teamId 0 = Team A, 1 = Team B
     */
    public int getCapturedFortressCount(int teamId) {
        int count = 0;
        for (Fortress f : fortresses.values()) {
            Fortress.FortressState target = teamId == 0
                    ? Fortress.FortressState.TEAM_A
                    : Fortress.FortressState.TEAM_B;
            if (f.getState() == target) {
                count++;
            }
        }
        return count;
    }

    /**
     * Adds a lane waypoint for Team A.
     */
    public void addLaneWaypointTeamA(Lane lane, Location loc) {
        laneWaypointsTeamA.computeIfAbsent(lane, k -> new ArrayList<>()).add(loc);
    }

    /**
     * Adds a lane waypoint for Team B.
     */
    public void addLaneWaypointTeamB(Lane lane, Location loc) {
        laneWaypointsTeamB.computeIfAbsent(lane, k -> new ArrayList<>()).add(loc);
    }

    /**
     * Returns the lane waypoints for Team A on the given lane.
     */
    public List<Location> getLaneWaypointsTeamA(Lane lane) {
        return laneWaypointsTeamA.getOrDefault(lane, new ArrayList<>());
    }

    /**
     * Returns the lane waypoints for Team B on the given lane.
     */
    public List<Location> getLaneWaypointsTeamB(Lane lane) {
        return laneWaypointsTeamB.getOrDefault(lane, new ArrayList<>());
    }

    /**
     * Adds a jump pad to the map.
     */
    public void addJumpPad(JumpPad pad) {
        jumpPads.add(pad);
    }

    public List<JumpPad> getJumpPads() {
        return jumpPads;
    }

    public World getWorld() {
        return world;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    /**
     * Resets all fortresses for a new round.
     */
    public void resetFortresses() {
        for (Fortress f : fortresses.values()) {
            f.reset();
        }
    }

    /**
     * Returns true if this map has been configured (world + at least one fortress set).
     */
    public boolean isConfigured() {
        return world != null && !fortresses.isEmpty();
    }
}
