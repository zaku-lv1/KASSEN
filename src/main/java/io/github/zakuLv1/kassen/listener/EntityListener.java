package io.github.zakuLv1.kassen.listener;

import io.github.zakuLv1.kassen.KassenPlugin;
import io.github.zakuLv1.kassen.entity.KassenMinion;
import io.github.zakuLv1.kassen.entity.MiniBoss;
import io.github.zakuLv1.kassen.entity.MinionManager;
import io.github.zakuLv1.kassen.game.GameManager;
import io.github.zakuLv1.kassen.game.GameState;
import io.github.zakuLv1.kassen.map.Castle;
import io.github.zakuLv1.kassen.player.KassenPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Listens for entity events (minion/boss deaths, castle attacks) and
 * delegates to GameManager.
 */
public class EntityListener implements Listener {

    /** Squared radius (in blocks) within which a player can attack the enemy castle. */
    private static final double CASTLE_ATTACK_RADIUS_SQUARED = 25.0;

    private final KassenPlugin plugin;
    private final GameManager gameManager;

    public EntityListener(KassenPlugin plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    /**
     * Handles entity death events for minions and mini bosses.
     */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (gameManager.getState() != GameState.PLAYING) return;

        Entity entity = event.getEntity();
        MinionManager mm = gameManager.getMinionManager();

        // Check if a minion died
        KassenMinion minion = mm.findMinion(entity);
        if (minion != null) {
            event.getDrops().clear();
            event.setDroppedExp(0);
            Player killer = event.getEntity().getKiller();
            gameManager.onMinionDeath(killer, minion);
            return;
        }

        // Check if a mini boss died
        MiniBoss boss = mm.findBoss(entity);
        if (boss != null) {
            event.getDrops().clear();
            event.setDroppedExp(0);
            Player killer = event.getEntity().getKiller();
            gameManager.onBossDeath(killer, boss);
        }
    }

    /**
     * Handles damage dealt to castle regions.
     * Castles are represented as blocks; we treat any projectile/explosion
     * damage near the castle location as castle damage.
     * For simplicity in this implementation, castle damage is triggered
     * when a player attacks within a small radius of the castle location.
     */
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (gameManager.getState() != GameState.PLAYING) return;

        // Only allow players to damage the castle
        if (!(event.getDamager() instanceof Player)) return;

        Player attacker = (Player) event.getDamager();
        KassenPlayer kp = gameManager.getKassenPlayer(attacker);
        if (kp == null) return;

        // Check if the attack is aimed at the enemy castle
        Castle castleA = gameManager.getCastleA();
        Castle castleB = gameManager.getCastleB();

        Castle targetCastle = null;
        if (kp.getTeam() == gameManager.getTeamB() && castleA != null
                && castleA.getLocation() != null
                && attacker.getLocation().distanceSquared(castleA.getLocation()) <= CASTLE_ATTACK_RADIUS_SQUARED) {
            targetCastle = castleA;
        } else if (kp.getTeam() == gameManager.getTeamA() && castleB != null
                && castleB.getLocation() != null
                && attacker.getLocation().distanceSquared(castleB.getLocation()) <= CASTLE_ATTACK_RADIUS_SQUARED) {
            targetCastle = castleB;
        }

        if (targetCastle != null) {
            event.setCancelled(true);
            gameManager.onCastleDamage(targetCastle, event.getDamage());
        }
    }
}
