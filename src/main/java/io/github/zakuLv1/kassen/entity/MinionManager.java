package io.github.zakuLv1.kassen.entity;

import io.github.zakuLv1.kassen.KassenPlugin;
import io.github.zakuLv1.kassen.game.GameTeam;
import io.github.zakuLv1.kassen.game.Lane;
import io.github.zakuLv1.kassen.map.Fortress;
import io.github.zakuLv1.kassen.map.KassenMap;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Vindicator;
import org.bukkit.entity.Zombie;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Manages all game entities: minions and mini bosses.
 * Handles spawning, AI ticking, and cleanup.
 */
public class MinionManager {

    /** Metadata key used to identify KASSEN minion entities. */
    public static final String MINION_META_KEY = "kassen_minion";

    /** Metadata key used to identify mini boss entities. */
    public static final String BOSS_META_KEY = "kassen_boss";

    private final KassenPlugin plugin;
    private final List<KassenMinion> minions = new ArrayList<>();
    private final List<MiniBoss> bosses = new ArrayList<>();
    private int spawnTaskId = -1;
    private int tickTaskId = -1;

    public MinionManager(KassenPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Starts the minion spawning and AI tick schedulers.
     *
     * @param map       the game map supplying lane waypoints
     * @param teamA     Team A
     * @param teamB     Team B
     */
    public void start(KassenMap map, GameTeam teamA, GameTeam teamB) {
        int spawnInterval = plugin.getConfig().getInt("minion.spawn-interval", 30) * 20;

        // Spawn minions periodically for all lanes
        spawnTaskId = new BukkitRunnable() {
            @Override
            public void run() {
                for (Lane lane : Lane.values()) {
                    spawnMinionsForLane(map, teamA, teamB, lane);
                }
            }
        }.runTaskTimer(plugin, spawnInterval, spawnInterval).getTaskId();

        // Tick minion AI every second
        tickTaskId = new BukkitRunnable() {
            @Override
            public void run() {
                tickMinions(map, teamA, teamB);
            }
        }.runTaskTimer(plugin, 20L, 20L).getTaskId();
    }

    /**
     * Stops all scheduled tasks and removes all minion entities.
     */
    public void stop() {
        if (spawnTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(spawnTaskId);
            spawnTaskId = -1;
        }
        if (tickTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(tickTaskId);
            tickTaskId = -1;
        }
        removeAll();
    }

    /**
     * Spawns a wave of minions for the given lane (one per team).
     */
    private void spawnMinionsForLane(KassenMap map, GameTeam teamA, GameTeam teamB, Lane lane) {
        int maxPerLane = plugin.getConfig().getInt("minion.max-per-lane", 5);

        // Count existing living minions for each team in this lane
        long countA = minions.stream()
                .filter(m -> m.getTeam() == teamA && m.getLane() == lane && m.isAlive())
                .count();
        long countB = minions.stream()
                .filter(m -> m.getTeam() == teamB && m.getLane() == lane && m.isAlive())
                .count();

        List<Location> waypointsA = map.getLaneWaypointsTeamA(lane);
        List<Location> waypointsB = map.getLaneWaypointsTeamB(lane);

        if (countA < maxPerLane && !waypointsA.isEmpty()) {
            spawnMinion(teamA, lane, waypointsA.get(0));
        }
        if (countB < maxPerLane && !waypointsB.isEmpty()) {
            spawnMinion(teamB, lane, waypointsB.get(0));
        }
    }

    /**
     * Spawns a single minion at the given location.
     */
    private void spawnMinion(GameTeam team, Lane lane, Location spawnLoc) {
        if (spawnLoc.getWorld() == null) return;

        double health = plugin.getConfig().getDouble("minion.health", 20.0);
        double speed = plugin.getConfig().getDouble("minion.speed", 0.25);

        Zombie zombie = (Zombie) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.ZOMBIE);
        zombie.setCustomName(team.getDisplayName() + " ミニオン");
        zombie.setCustomNameVisible(true);
        zombie.setMetadata(MINION_META_KEY, new FixedMetadataValue(plugin, team.getId()));

        AttributeInstance maxHp = zombie.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHp != null) maxHp.setBaseValue(health);
        zombie.setHealth(health);

        AttributeInstance speedAttr = zombie.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speedAttr != null) speedAttr.setBaseValue(speed);

        KassenMinion minion = new KassenMinion(team, lane, zombie);
        minions.add(minion);
    }

    /**
     * Spawns a mini boss guarding the given fortress.
     *
     * @param fortress the fortress to guard
     */
    public void spawnBoss(Fortress fortress) {
        Location loc = fortress.getLocation();
        if (loc == null || loc.getWorld() == null) return;

        double health = plugin.getConfig().getDouble("boss.health", 100.0);

        Vindicator vindicator = (Vindicator) loc.getWorld().spawnEntity(loc, EntityType.VINDICATOR);
        vindicator.setCustomName("【中ボス】 " + fortress.getDisplayName());
        vindicator.setCustomNameVisible(true);
        vindicator.setMetadata(BOSS_META_KEY, new FixedMetadataValue(plugin, fortress.getLane().name()));

        AttributeInstance maxHp = vindicator.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHp != null) maxHp.setBaseValue(health);
        vindicator.setHealth(health);

        bosses.add(new MiniBoss(fortress, vindicator));
    }

    /**
     * AI tick: move minions along lane waypoints.
     */
    private void tickMinions(KassenMap map, GameTeam teamA, GameTeam teamB) {
        Iterator<KassenMinion> it = minions.iterator();
        while (it.hasNext()) {
            KassenMinion minion = it.next();
            if (!minion.isAlive()) {
                it.remove();
                continue;
            }

            // Move toward the next waypoint
            List<Location> waypoints = minion.getTeam() == teamA
                    ? map.getLaneWaypointsTeamA(minion.getLane())
                    : map.getLaneWaypointsTeamB(minion.getLane());

            if (waypoints.isEmpty()) continue;

            int idx = Math.min(minion.getWaypointIndex(), waypoints.size() - 1);
            Location target = waypoints.get(idx);

            double dist = minion.getEntity().getLocation().distanceSquared(target);
            if (dist < 4.0) {
                // Reached waypoint, advance to next
                if (minion.getWaypointIndex() < waypoints.size() - 1) {
                    minion.advanceWaypoint();
                }
            } else {
                // Navigate toward waypoint (Zombie pathfinding handles movement naturally)
                minion.getEntity().getPathfinder().moveTo(target);
            }
        }
    }

    /**
     * Finds the MiniBoss wrapper for the given entity, or null.
     */
    public MiniBoss findBoss(Entity entity) {
        for (MiniBoss boss : bosses) {
            if (boss.getEntity().equals(entity)) {
                return boss;
            }
        }
        return null;
    }

    /**
     * Finds the KassenMinion wrapper for the given entity, or null.
     */
    public KassenMinion findMinion(Entity entity) {
        for (KassenMinion minion : minions) {
            if (minion.getEntity().equals(entity)) {
                return minion;
            }
        }
        return null;
    }

    /**
     * Removes all minion and boss entities from the world.
     */
    public void removeAll() {
        for (KassenMinion m : minions) {
            if (m.isAlive()) m.getEntity().remove();
        }
        for (MiniBoss b : bosses) {
            if (b.isAlive()) b.getEntity().remove();
        }
        minions.clear();
        bosses.clear();
    }

    /**
     * Returns a copy of the current minion list.
     */
    public List<KassenMinion> getMinions() {
        return new ArrayList<>(minions);
    }

    /**
     * Returns a copy of the current boss list.
     */
    public List<MiniBoss> getBosses() {
        return new ArrayList<>(bosses);
    }
}
