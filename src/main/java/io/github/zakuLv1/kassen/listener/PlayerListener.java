package io.github.zakuLv1.kassen.listener;

import io.github.zakuLv1.kassen.KassenPlugin;
import io.github.zakuLv1.kassen.game.GameManager;
import io.github.zakuLv1.kassen.game.GameState;
import io.github.zakuLv1.kassen.player.KassenPlayer;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Listens for player-related events and delegates to GameManager.
 */
public class PlayerListener implements Listener {

    private final KassenPlugin plugin;
    private final GameManager gameManager;

    public PlayerListener(KassenPlugin plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    /**
     * Intercepts player death during a game.
     * Cancels the vanilla death screen and handles it internally.
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (gameManager.getState() != GameState.PLAYING) return;
        KassenPlayer kp = gameManager.getKassenPlayer(event.getEntity());
        if (kp == null) return;

        // Suppress the default death and respawn messages
        event.setDeathMessage(null);
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.getDrops().clear();

        gameManager.onPlayerDeath(event.getEntity(), event.getEntity().getKiller());
    }

    /**
     * Suppresses the vanilla respawn screen during a game.
     * (actual respawn is managed by GameManager's scheduler)
     */
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (gameManager.getState() != GameState.PLAYING) return;
        KassenPlayer kp = gameManager.getKassenPlayer(event.getPlayer());
        if (kp == null) return;
        // Respawn location is handled by GameManager; nothing to do here.
    }

    /**
     * Handles player movement to trigger jump pads.
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (gameManager.getState() != GameState.PLAYING) return;
        if (!event.hasChangedBlock()) return;
        gameManager.checkJumpPad(event.getPlayer());
    }

    /**
     * Removes a player from the game when they disconnect.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (gameManager.getState() == GameState.LOBBY) return;
        KassenPlayer kp = gameManager.getKassenPlayer(event.getPlayer());
        if (kp == null) return;
        gameManager.unregisterPlayer(event.getPlayer());
    }
}
