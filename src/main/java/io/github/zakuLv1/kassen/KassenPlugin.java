package io.github.zakuLv1.kassen;

import io.github.zakuLv1.kassen.command.KassenCommand;
import io.github.zakuLv1.kassen.game.GameManager;
import io.github.zakuLv1.kassen.listener.EntityListener;
import io.github.zakuLv1.kassen.listener.PlayerListener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * KASSEN – チーム対戦型戦略アクションゲーム Minecraft プラグイン
 *
 * <p>このプラグインはPaper Minecraft上でKASSENゲームを再現します。
 * SENGOKUモード（3v3）とバトルロイヤルモード（7v7）をサポートしており、
 * レーン構造、ミニオン、拠点占領、残機システム、必殺技ゲージを実装しています。</p>
 */
public class KassenPlugin extends JavaPlugin {

    private GameManager gameManager;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        // Initialize game manager
        gameManager = new GameManager(this);

        // Register command
        KassenCommand kassenCommand = new KassenCommand(this, gameManager);
        getCommand("kassen").setExecutor(kassenCommand);
        getCommand("kassen").setTabCompleter(kassenCommand);

        // Register listeners
        getServer().getPluginManager().registerEvents(
                new PlayerListener(this, gameManager), this);
        getServer().getPluginManager().registerEvents(
                new EntityListener(this, gameManager), this);

        // Start fortress capture tick (every second)
        new BukkitRunnable() {
            @Override
            public void run() {
                gameManager.tickFortressCapture();
            }
        }.runTaskTimer(this, 20L, 20L);

        getLogger().info("KASSENプラグインが有効になりました！");
        getLogger().info("ゲームモード: SENGOKU (3v3), バトルロイヤル (7v7)");
        getLogger().info("/kassen help でコマンド一覧を確認できます。");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.stopGame();
        }
        getLogger().info("KASSENプラグインが無効になりました。");
    }

    /**
     * Returns the central game manager.
     */
    public GameManager getGameManager() {
        return gameManager;
    }
}
