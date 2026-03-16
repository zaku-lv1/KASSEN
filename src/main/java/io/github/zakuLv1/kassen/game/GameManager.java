package io.github.zakuLv1.kassen.game;

import io.github.zakuLv1.kassen.KassenPlugin;
import io.github.zakuLv1.kassen.entity.KassenMinion;
import io.github.zakuLv1.kassen.entity.MiniBoss;
import io.github.zakuLv1.kassen.entity.MinionManager;
import io.github.zakuLv1.kassen.map.Castle;
import io.github.zakuLv1.kassen.map.Fortress;
import io.github.zakuLv1.kassen.map.KassenMap;
import io.github.zakuLv1.kassen.player.KassenPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Central manager for the KASSEN game.
 * Handles game state transitions, round management, player registration,
 * respawning, fortress capture, and victory condition evaluation.
 */
public class GameManager {

    /** Squared radius (in blocks) for fortress capture detection. */
    private static final double FORTRESS_CAPTURE_RADIUS_SQUARED = 25.0;

    private final KassenPlugin plugin;
    private final MinionManager minionManager;

    private io.github.zakuLv1.kassen.game.GameMode gameMode;
    private GameState state;
    private KassenMap kassenMap;

    private GameTeam teamA;
    private GameTeam teamB;
    private Castle castleA;
    private Castle castleB;

    private final Map<UUID, KassenPlayer> kassenPlayers = new HashMap<>();

    private int currentRound;
    private GameRound activeRound;
    private final int roundsToWin;
    private int startCountdownTaskId = -1;

    public GameManager(KassenPlugin plugin) {
        this.plugin = plugin;
        this.minionManager = new MinionManager(plugin);
        this.state = GameState.LOBBY;
        this.gameMode = io.github.zakuLv1.kassen.game.GameMode.SENGOKU;
        this.roundsToWin = plugin.getConfig().getInt("game.rounds-to-win", 2);
    }

    // ──────────────────────────────────────────────
    // Map / Setup
    // ──────────────────────────────────────────────

    /**
     * Sets the game map to use.
     */
    public void setMap(KassenMap map) {
        this.kassenMap = map;
    }

    /**
     * Sets the castle for the given team.
     */
    public void setCastle(int teamId, Castle castle) {
        if (teamId == GameTeam.TEAM_A_ID) {
            this.castleA = castle;
        } else {
            this.castleB = castle;
        }
    }

    /**
     * Initializes the two teams.
     */
    public void initTeams() {
        teamA = new GameTeam(GameTeam.TEAM_A_ID, "チームA", ChatColor.RED);
        teamB = new GameTeam(GameTeam.TEAM_B_ID, "チームB", ChatColor.BLUE);
    }

    // ──────────────────────────────────────────────
    // Player Management
    // ──────────────────────────────────────────────

    /**
     * Registers a player into the game and assigns them to a team.
     *
     * @param player the Bukkit player
     * @return the created KassenPlayer
     */
    public KassenPlayer registerPlayer(Player player) {
        KassenPlayer kp = new KassenPlayer(player, plugin);
        kassenPlayers.put(player.getUniqueId(), kp);
        assignToTeam(kp);
        return kp;
    }

    /**
     * Removes a player from the game.
     */
    public void unregisterPlayer(Player player) {
        KassenPlayer kp = kassenPlayers.remove(player.getUniqueId());
        if (kp != null && kp.getTeam() != null) {
            kp.getTeam().removePlayer(kp);
        }
    }

    private void assignToTeam(KassenPlayer kp) {
        if (teamA == null) initTeams();
        int sizeA = teamA.getPlayers().size();
        int sizeB = teamB.getPlayers().size();
        if (sizeA <= sizeB) {
            teamA.addPlayer(kp);
        } else {
            teamB.addPlayer(kp);
        }
    }

    /**
     * Returns the KassenPlayer wrapper for the given Bukkit player, or null.
     */
    public KassenPlayer getKassenPlayer(Player player) {
        return kassenPlayers.get(player.getUniqueId());
    }

    // ──────────────────────────────────────────────
    // Game Lifecycle
    // ──────────────────────────────────────────────

    /**
     * Starts the pre-game countdown, then begins the first round.
     */
    public void startGame(io.github.zakuLv1.kassen.game.GameMode mode) {
        if (state != GameState.LOBBY) {
            return;
        }
        this.gameMode = mode;
        this.state = GameState.STARTING;
        this.currentRound = 0;

        if (teamA == null) initTeams();

        int countdown = plugin.getConfig().getInt("game.start-countdown", 10);
        broadcastMessage(ChatColor.GOLD + "KASSENゲームが " + countdown + " 秒後に開始します！");

        startCountdownTaskId = new BukkitRunnable() {
            int remaining = countdown;

            @Override
            public void run() {
                if (remaining <= 0) {
                    startRound();
                    cancel();
                    return;
                }
                if (remaining <= 5 || remaining == countdown) {
                    broadcastMessage("" + ChatColor.YELLOW + remaining + " 秒後にスタート！");
                }
                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L).getTaskId();
    }

    /**
     * Starts a new round.
     */
    private void startRound() {
        currentRound++;
        state = GameState.PLAYING;
        activeRound = new GameRound(currentRound);

        // Reset map fortresses
        if (kassenMap != null) {
            kassenMap.resetFortresses();
            // Spawn mini bosses on fortresses
            for (Fortress f : kassenMap.getAllFortresses()) {
                minionManager.spawnBoss(f);
            }
        }

        // Reset castles
        if (castleA != null) castleA.reset();
        if (castleB != null) castleB.reset();

        // Reset player lives
        int sengokuLives = plugin.getConfig().getInt("game.sengoku-lives", 3);
        int brLives = plugin.getConfig().getInt("game.battle-royale-lives", 3);
        if (gameMode == io.github.zakuLv1.kassen.game.GameMode.BATTLE_ROYALE) {
            teamA.resetForRound(Integer.MAX_VALUE);
            teamB.resetForRound(Integer.MAX_VALUE);
            teamA.setSharedLives(brLives);
            teamB.setSharedLives(brLives);
        } else {
            teamA.resetForRound(sengokuLives);
            teamB.resetForRound(sengokuLives);
        }

        // Teleport players to spawn and restore health
        teleportToSpawn(teamA);
        teleportToSpawn(teamB);

        // Start minion spawning
        if (kassenMap != null) {
            minionManager.start(kassenMap, teamA, teamB);
        }

        broadcastMessage(ChatColor.GREEN + "ラウンド " + currentRound + " 開始！");
    }

    private void teleportToSpawn(GameTeam team) {
        Castle castle = (team == teamA) ? castleA : castleB;
        Location spawnLoc = (castle != null && castle.getLocation() != null)
                ? castle.getLocation()
                : (kassenMap != null && kassenMap.getWorld() != null
                   ? kassenMap.getWorld().getSpawnLocation()
                   : null);

        for (KassenPlayer kp : team.getPlayers()) {
            Player p = kp.getPlayer();
            if (p == null || !p.isOnline()) continue;
            p.setHealth(p.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null
                    ? p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() : 20.0);
            p.setFoodLevel(20);
            p.setGameMode(GameMode.SURVIVAL);
            if (spawnLoc != null) {
                p.teleport(spawnLoc);
            }
        }
    }

    /**
     * Stops the game and cleans up all resources.
     */
    public void stopGame() {
        state = GameState.LOBBY;
        minionManager.stop();
        if (startCountdownTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(startCountdownTaskId);
            startCountdownTaskId = -1;
        }
        broadcastMessage(ChatColor.RED + "ゲームが終了しました。");
        kassenPlayers.clear();
        if (teamA != null) teamA.getPlayers().clear();
        if (teamB != null) teamB.getPlayers().clear();
        currentRound = 0;
    }

    // ──────────────────────────────────────────────
    // Player Death & Respawn
    // ──────────────────────────────────────────────

    /**
     * Handles a player death event during the game.
     *
     * @param player the player who died
     * @param killer the entity that killed them (may be null)
     */
    public void onPlayerDeath(Player player, org.bukkit.entity.Entity killer) {
        KassenPlayer kp = getKassenPlayer(player);
        if (kp == null || state != GameState.PLAYING) return;

        // Award gauge to killer if a player
        if (killer instanceof Player) {
            KassenPlayer killerKp = getKassenPlayer((Player) killer);
            if (killerKp != null) {
                killerKp.getSpecialGauge().addPlayerKill();
                sendMessage((Player) killer, ChatColor.GOLD + "プレイヤーを撃破！必殺技ゲージ +" +
                        plugin.getConfig().getInt("player.gauge-per-player-kill", 20));
            }
        }

        boolean canRespawn;
        if (gameMode == io.github.zakuLv1.kassen.game.GameMode.BATTLE_ROYALE) {
            GameTeam team = kp.getTeam();
            canRespawn = !team.hasNoSharedLives();
            if (!canRespawn) {
                kp.onDeath(); // mark as eliminated
            } else {
                team.decrementSharedLives();
            }
        } else {
            canRespawn = kp.onDeath();
        }

        if (canRespawn) {
            sendMessage(player, ChatColor.YELLOW + "残機: " + kp.getLives() + "  " +
                    "リスポーン中...");
            scheduleRespawn(kp);
        } else {
            sendMessage(player, ChatColor.RED + "残機がなくなりました！観戦モードになります。");
            player.setGameMode(GameMode.SPECTATOR);
            checkElimination();
        }
    }

    /**
     * Handles death of a minion entity.
     *
     * @param killer the player who killed the minion (may be null)
     * @param minion the killed minion
     */
    public void onMinionDeath(Player killer, KassenMinion minion) {
        if (killer == null) return;
        KassenPlayer kp = getKassenPlayer(killer);
        if (kp == null) return;
        kp.getSpecialGauge().addMinionKill();
        sendMessage(killer, ChatColor.AQUA + "ミニオン撃破！必殺技ゲージ +" +
                plugin.getConfig().getInt("minion.gauge-per-kill", 10));
    }

    /**
     * Handles death of a mini boss entity.
     *
     * @param killer the player who killed the boss (may be null)
     * @param boss   the killed boss
     */
    public void onBossDeath(Player killer, MiniBoss boss) {
        boss.onDeath();
        Fortress fortress = boss.getFortress();
        broadcastMessage(ChatColor.YELLOW + fortress.getDisplayName() + " の中ボスが討伐されました！"
                + ChatColor.WHITE + "今すぐ占領できます！");

        if (killer == null) return;
        KassenPlayer kp = getKassenPlayer(killer);
        if (kp != null) {
            kp.getSpecialGauge().addBossKill();
            sendMessage(killer, ChatColor.GOLD + "中ボス撃破！必殺技ゲージ +" +
                    plugin.getConfig().getInt("boss.gauge-per-kill", 30));
        }
    }

    private void scheduleRespawn(KassenPlayer kp) {
        int delay = plugin.getConfig().getInt("game.respawn-delay", 5);
        Player player = kp.getPlayer();
        player.setGameMode(GameMode.SPECTATOR);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || state != GameState.PLAYING) return;
                if (kp.isEliminated()) return;
                respawnPlayer(kp);
            }
        }.runTaskLater(plugin, delay * 20L);
    }

    private void respawnPlayer(KassenPlayer kp) {
        Location respawnLoc = findRespawnLocation(kp);
        Player player = kp.getPlayer();

        player.setGameMode(GameMode.SURVIVAL);
        if (respawnLoc != null) {
            player.teleport(respawnLoc);
        }
        if (player.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
        }
        player.setFoodLevel(20);
        sendMessage(player, ChatColor.GREEN + "リスポーンしました！");
    }

    /**
     * Finds the best respawn location for a player:
     * 1) A captured fortress belonging to their team
     * 2) Their team's castle
     */
    private Location findRespawnLocation(KassenPlayer kp) {
        GameTeam team = kp.getTeam();
        if (kassenMap != null) {
            for (Fortress f : kassenMap.getAllFortresses()) {
                if (f.isCapturedBy(team) && f.getLocation() != null) {
                    return f.getLocation();
                }
            }
        }
        Castle castle = (team == teamA) ? castleA : castleB;
        if (castle != null && castle.getLocation() != null) {
            return castle.getLocation();
        }
        return kassenMap != null && kassenMap.getWorld() != null
                ? kassenMap.getWorld().getSpawnLocation() : null;
    }

    // ──────────────────────────────────────────────
    // Fortress Capture Tick
    // ──────────────────────────────────────────────

    /**
     * Called every second to tick fortress captures for players standing inside.
     * Also triggers the jump pad mechanics.
     */
    public void tickFortressCapture() {
        if (state != GameState.PLAYING || kassenMap == null) return;

        for (Fortress fortress : kassenMap.getAllFortresses()) {
            if (!fortress.canBeCaptured()) continue;

            // Find a player standing near the fortress
            Location fl = fortress.getLocation();
            if (fl == null) continue;

            KassenPlayer capturer = null;
            for (KassenPlayer kp : kassenPlayers.values()) {
                Player p = kp.getPlayer();
                if (!p.isOnline() || kp.isEliminated()) continue;
                if (p.getWorld().equals(fl.getWorld())
                        && p.getLocation().distanceSquared(fl) <= FORTRESS_CAPTURE_RADIUS_SQUARED) {
                    capturer = kp;
                    break;
                }
            }

            if (capturer != null) {
                boolean captured = fortress.tickCapture(capturer.getTeam());
                if (captured) {
                    GameTeam team = capturer.getTeam();
                    broadcastMessage(ChatColor.GOLD + team.getColoredName()
                            + ChatColor.GOLD + " が " + fortress.getDisplayName() + " を占領しました！");
                    checkRoundWinCondition();
                } else {
                    // Progress notification
                    int pct = (fortress.getCaptureProgress() * 100) / fortress.getCaptureTime();
                    sendMessage(capturer.getPlayer(),
                            ChatColor.YELLOW + fortress.getDisplayName() + " 占領中... " + pct + "%");
                }
            } else {
                fortress.interruptCapture();
            }
        }
    }

    // ──────────────────────────────────────────────
    // Victory Conditions
    // ──────────────────────────────────────────────

    /**
     * Called when a castle is attacked (from EntityListener).
     * If the castle is destroyed, declares round over.
     *
     * @param castle      the castle that was attacked
     * @param damage      damage dealt
     */
    public void onCastleDamage(Castle castle, double damage) {
        if (state != GameState.PLAYING) return;
        boolean destroyed = castle.takeDamage(damage);
        GameTeam defender = castle.getOwnerTeam();
        broadcastMessage(ChatColor.RED + defender.getColoredName() + ChatColor.RED +
                " の天守閣が攻撃を受けました！ HP: " + castle.getHpPercent() + "%");
        if (destroyed) {
            onCastleDestroyed(castle);
        }
    }

    private void onCastleDestroyed(Castle castle) {
        GameTeam loser = castle.getOwnerTeam();
        GameTeam winner = (loser == teamA) ? teamB : teamA;

        broadcastMessage(ChatColor.RED + loser.getColoredName() +
                ChatColor.RED + " の天守閣が破壊されました！");

        if (activeRound != null) {
            activeRound.declareCastleDestroyed(winner);
        }
        endRound(winner);
    }

    /**
     * Checks whether either team has met fortress-based round win conditions.
     */
    private void checkRoundWinCondition() {
        if (activeRound == null || state != GameState.PLAYING) return;
        GameTeam roundWinner = activeRound.checkRoundWin(kassenMap, teamA, teamB);
        if (roundWinner != null) {
            endRound(roundWinner);
        }
    }

    /**
     * Checks whether any team has been fully eliminated (all players out of lives).
     */
    private void checkElimination() {
        if (state != GameState.PLAYING) return;

        boolean aWiped = teamA.isWiped();
        boolean bWiped = teamB.isWiped();

        if (gameMode == io.github.zakuLv1.kassen.game.GameMode.SENGOKU) {
            // SENGOKU: wipe + no lives → opponent wins the round
            if (aWiped && teamA.getPlayers().stream().allMatch(p -> p.getLives() == 0)) {
                endRound(teamB);
            } else if (bWiped && teamB.getPlayers().stream().allMatch(p -> p.getLives() == 0)) {
                endRound(teamA);
            }
        } else if (gameMode == io.github.zakuLv1.kassen.game.GameMode.BATTLE_ROYALE) {
            // BR: wipe + no shared lives → opponent wins
            if (aWiped && teamA.hasNoSharedLives()) {
                endRound(teamB);
            } else if (bWiped && teamB.hasNoSharedLives()) {
                endRound(teamA);
            }
        }
    }

    private void endRound(GameTeam winner) {
        if (state != GameState.PLAYING) return;
        state = GameState.ROUND_END;
        minionManager.stop();

        winner.incrementRoundWins();
        broadcastMessage(ChatColor.GOLD + "ラウンド " + currentRound + " 終了！"
                + winner.getColoredName() + ChatColor.GOLD + " の勝利！");

        // Check overall game win
        if (gameMode == io.github.zakuLv1.kassen.game.GameMode.SENGOKU
                && winner.getRoundWins() >= roundsToWin) {
            endGame(winner);
            return;
        }

        // Start next round after a delay
        new BukkitRunnable() {
            @Override
            public void run() {
                if (state == GameState.ROUND_END) {
                    startRound();
                }
            }
        }.runTaskLater(plugin, 100L); // 5 seconds
    }

    private void endGame(GameTeam winner) {
        state = GameState.GAME_END;
        broadcastMessage(ChatColor.GOLD + "=== ゲーム終了 === " + winner.getColoredName()
                + ChatColor.GOLD + " の優勝！");
        broadcastMessage(ChatColor.YELLOW + "スコア: " + teamA.getColoredName() + ChatColor.YELLOW
                + " " + teamA.getRoundWins() + " - " + teamB.getRoundWins()
                + " " + teamB.getColoredName());

        new BukkitRunnable() {
            @Override
            public void run() {
                stopGame();
            }
        }.runTaskLater(plugin, 200L); // 10 seconds
    }

    // ──────────────────────────────────────────────
    // Special Attack
    // ──────────────────────────────────────────────

    /**
     * Activates a player's special attack if their gauge is full.
     * Deals area-of-effect damage to nearby enemies.
     *
     * @param player the player using their special
     * @return true if the special was activated
     */
    public boolean useSpecial(Player player) {
        KassenPlayer kp = getKassenPlayer(player);
        if (kp == null || state != GameState.PLAYING) return false;
        if (!kp.getSpecialGauge().use()) {
            sendMessage(player, ChatColor.RED + "必殺技ゲージがまだ満タンではありません！");
            return false;
        }

        double damage = plugin.getConfig().getDouble("special.damage", 15.0);
        double radius = plugin.getConfig().getDouble("special.radius", 5.0);
        double radiusSquared = radius * radius;
        GameTeam enemyTeam = (kp.getTeam() == teamA) ? teamB : teamA;

        // Damage nearby enemies
        for (KassenPlayer enemy : enemyTeam.getPlayers()) {
            Player ep = enemy.getPlayer();
            if (!ep.isOnline() || enemy.isEliminated()) continue;
            if (ep.getLocation().distanceSquared(player.getLocation()) <= radiusSquared) {
                ep.damage(damage);
            }
        }

        // Visual effect
        player.getWorld().createExplosion(player.getLocation(), 0F, false, false);
        broadcastMessage(kp.getTeam().getColoredName()
                + ChatColor.GOLD + " の " + player.getName() + " が必殺技を使用！");
        return true;
    }

    // ──────────────────────────────────────────────
    // Jump Pad
    // ──────────────────────────────────────────────

    /**
     * Checks if a player is standing on a jump pad and launches them if so.
     */
    public void checkJumpPad(Player player) {
        if (kassenMap == null || state != GameState.PLAYING) return;
        for (io.github.zakuLv1.kassen.map.JumpPad pad : kassenMap.getJumpPads()) {
            if (pad.tryLaunch(player)) {
                sendMessage(player, ChatColor.AQUA + "ジャンプ台！前線へ向かいます！");
                break;
            }
        }
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private void broadcastMessage(String msg) {
        Bukkit.broadcastMessage(ChatColor.DARK_GRAY + "[KASSEN] " + ChatColor.RESET + msg);
    }

    private void sendMessage(Player player, String msg) {
        player.sendMessage(ChatColor.DARK_GRAY + "[KASSEN] " + ChatColor.RESET + msg);
    }

    // ──────────────────────────────────────────────
    // Getters
    // ──────────────────────────────────────────────

    public GameState getState() {
        return state;
    }

    public io.github.zakuLv1.kassen.game.GameMode getGameMode() {
        return gameMode;
    }

    public void setGameMode(io.github.zakuLv1.kassen.game.GameMode mode) {
        this.gameMode = mode;
    }

    public KassenMap getKassenMap() {
        return kassenMap;
    }

    public GameTeam getTeamA() {
        return teamA;
    }

    public GameTeam getTeamB() {
        return teamB;
    }

    public Castle getCastleA() {
        return castleA;
    }

    public Castle getCastleB() {
        return castleB;
    }

    public MinionManager getMinionManager() {
        return minionManager;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public Map<UUID, KassenPlayer> getKassenPlayers() {
        return Collections.unmodifiableMap(kassenPlayers);
    }
}
