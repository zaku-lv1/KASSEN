package io.github.zakuLv1.kassen.command;

import io.github.zakuLv1.kassen.KassenPlugin;
import io.github.zakuLv1.kassen.game.GameManager;
import io.github.zakuLv1.kassen.game.GameMode;
import io.github.zakuLv1.kassen.game.GameState;
import io.github.zakuLv1.kassen.game.GameTeam;
import io.github.zakuLv1.kassen.map.Castle;
import io.github.zakuLv1.kassen.map.Fortress;
import io.github.zakuLv1.kassen.map.JumpPad;
import io.github.zakuLv1.kassen.map.KassenMap;
import io.github.zakuLv1.kassen.game.Lane;
import io.github.zakuLv1.kassen.player.KassenPlayer;
import io.github.zakuLv1.kassen.player.PlayerRole;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Handles all /kassen subcommands.
 *
 * <pre>
 * /kassen help                       - Show help
 * /kassen setup <mode>               - Set game mode (SENGOKU|BATTLE_ROYALE|PVE_SOLO|PVP_1V1)
 * /kassen start                      - Start the game
 * /kassen stop                       - Stop the game
 * /kassen join [a|b]                 - Join a team (or auto-assign)
 * /kassen setcastle <a|b>            - Set castle location at current position
 * /kassen setfortress <top|bot>      - Set fortress location at current position
 * /kassen addjumppad <dx> <dz>       - Add a jump pad at current position
 * /kassen setrole <attacker|tank|support> - Set own role
 * /kassen special                    - Use special attack
 * /kassen status                     - Show game status
 * </pre>
 */
public class KassenCommand implements CommandExecutor, TabCompleter {

    private final KassenPlugin plugin;
    private final GameManager gameManager;

    public KassenCommand(KassenPlugin plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help" -> sendHelp(sender);
            case "setup" -> handleSetup(sender, args);
            case "start" -> handleStart(sender, args);
            case "stop" -> handleStop(sender);
            case "join" -> handleJoin(sender, args);
            case "setcastle" -> handleSetCastle(sender, args);
            case "setfortress" -> handleSetFortress(sender, args);
            case "addjumppad" -> handleAddJumpPad(sender, args);
            case "setrole" -> handleSetRole(sender, args);
            case "special" -> handleSpecial(sender);
            case "status" -> handleStatus(sender);
            default -> {
                sender.sendMessage(ChatColor.RED + "不明なサブコマンドです。/kassen help を参照してください。");
            }
        }
        return true;
    }

    // ──────────────────────────────────────────────
    // Sub-command handlers
    // ──────────────────────────────────────────────

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== KASSEN コマンドヘルプ ===");
        sender.sendMessage(ChatColor.YELLOW + "/kassen setup <mode>"
                + ChatColor.WHITE + " - ゲームモード設定 (SENGOKU/BATTLE_ROYALE/PVE_SOLO/PVP_1V1)");
        sender.sendMessage(ChatColor.YELLOW + "/kassen start [mode]"
                + ChatColor.WHITE + " - ゲーム開始");
        sender.sendMessage(ChatColor.YELLOW + "/kassen stop"
                + ChatColor.WHITE + " - ゲーム停止");
        sender.sendMessage(ChatColor.YELLOW + "/kassen join [a|b]"
                + ChatColor.WHITE + " - チームに参加");
        sender.sendMessage(ChatColor.YELLOW + "/kassen setcastle <a|b>"
                + ChatColor.WHITE + " - 天守閣の位置を設定");
        sender.sendMessage(ChatColor.YELLOW + "/kassen setfortress <top|bot>"
                + ChatColor.WHITE + " - 櫓の位置を設定");
        sender.sendMessage(ChatColor.YELLOW + "/kassen addjumppad <dx> <dz>"
                + ChatColor.WHITE + " - ジャンプ台を追加");
        sender.sendMessage(ChatColor.YELLOW + "/kassen setrole <attacker|tank|support>"
                + ChatColor.WHITE + " - 役割設定");
        sender.sendMessage(ChatColor.YELLOW + "/kassen special"
                + ChatColor.WHITE + " - 必殺技使用");
        sender.sendMessage(ChatColor.YELLOW + "/kassen status"
                + ChatColor.WHITE + " - ゲーム状態表示");
    }

    private void handleSetup(CommandSender sender, String[] args) {
        if (!checkOp(sender)) return;
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "使用法: /kassen setup <SENGOKU|BATTLE_ROYALE|PVE_SOLO|PVP_1V1>");
            return;
        }
        GameMode mode;
        try {
            mode = GameMode.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "不明なモードです: " + args[1]);
            return;
        }
        gameManager.setGameMode(mode);
        sender.sendMessage(ChatColor.GREEN + "ゲームモードを " + mode.getDisplayName() + " に設定しました。");
    }

    private void handleStart(CommandSender sender, String[] args) {
        if (!checkOp(sender)) return;
        if (gameManager.getState() != GameState.LOBBY) {
            sender.sendMessage(ChatColor.RED + "ゲームは既に進行中です。");
            return;
        }
        GameMode mode = gameManager.getGameMode();
        if (args.length >= 2) {
            try {
                mode = GameMode.valueOf(args[1].toUpperCase());
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + "不明なモードです: " + args[1]);
                return;
            }
        }
        // Ensure map exists
        if (gameManager.getKassenMap() == null) {
            KassenMap map = new KassenMap();
            if (sender instanceof Player) {
                map.setWorld(((Player) sender).getWorld());
            }
            gameManager.setMap(map);
        }
        gameManager.startGame(mode);
        sender.sendMessage(ChatColor.GREEN + "ゲームを開始します！");
    }

    private void handleStop(CommandSender sender) {
        if (!checkOp(sender)) return;
        gameManager.stopGame();
        sender.sendMessage(ChatColor.GREEN + "ゲームを停止しました。");
    }

    private void handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "プレイヤーのみ実行できます。");
            return;
        }
        Player player = (Player) sender;
        KassenPlayer kp = gameManager.registerPlayer(player);

        if (args.length >= 2) {
            GameTeam targetTeam = null;
            if (args[1].equalsIgnoreCase("a")) {
                targetTeam = gameManager.getTeamA();
            } else if (args[1].equalsIgnoreCase("b")) {
                targetTeam = gameManager.getTeamB();
            }
            if (targetTeam != null && kp.getTeam() != targetTeam) {
                // Re-assign to specified team
                if (kp.getTeam() != null) kp.getTeam().removePlayer(kp);
                targetTeam.addPlayer(kp);
            }
        }

        String teamName = kp.getTeam() != null ? kp.getTeam().getColoredName() : "未割り当て";
        player.sendMessage(ChatColor.GREEN + teamName + ChatColor.GREEN + " に参加しました！");
    }

    private void handleSetCastle(CommandSender sender, String[] args) {
        if (!checkOp(sender)) return;
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "プレイヤーのみ実行できます。");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "使用法: /kassen setcastle <a|b>");
            return;
        }
        Player player = (Player) sender;
        int teamId = args[1].equalsIgnoreCase("a") ? GameTeam.TEAM_A_ID : GameTeam.TEAM_B_ID;
        double castleHp = plugin.getConfig().getDouble("castle.health", 500.0);

        ensureMapExists(player.getWorld());
        GameTeam team = teamId == GameTeam.TEAM_A_ID ? gameManager.getTeamA() : gameManager.getTeamB();
        if (team == null) {
            gameManager.initTeams();
            team = teamId == GameTeam.TEAM_A_ID ? gameManager.getTeamA() : gameManager.getTeamB();
        }

        Castle castle = new Castle(team, castleHp);
        castle.setLocation(player.getLocation());
        gameManager.setCastle(teamId, castle);
        sender.sendMessage(ChatColor.GREEN + "チーム" + (teamId == 0 ? "A" : "B")
                + " の天守閣を " + formatLoc(player.getLocation()) + " に設定しました。");
    }

    private void handleSetFortress(CommandSender sender, String[] args) {
        if (!checkOp(sender)) return;
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "プレイヤーのみ実行できます。");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "使用法: /kassen setfortress <top|bot>");
            return;
        }
        Player player = (Player) sender;
        Lane lane;
        try {
            lane = Lane.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "不明なレーンです (top/bot)。");
            return;
        }
        if (lane == Lane.MID) {
            sender.sendMessage(ChatColor.RED + "Midレーンには櫓がありません (top/bot のみ)。");
            return;
        }

        ensureMapExists(player.getWorld());
        int captureTime = plugin.getConfig().getInt("fortress.capture-time", 10);
        Fortress fortress = new Fortress(lane, captureTime);
        fortress.setLocation(player.getLocation());
        gameManager.getKassenMap().setFortress(lane, fortress);
        sender.sendMessage(ChatColor.GREEN + fortress.getDisplayName() + " を "
                + formatLoc(player.getLocation()) + " に設定しました。");
    }

    private void handleAddJumpPad(CommandSender sender, String[] args) {
        if (!checkOp(sender)) return;
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "プレイヤーのみ実行できます。");
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "使用法: /kassen addjumppad <dx> <dz>");
            return;
        }
        double dx, dz;
        try {
            dx = Double.parseDouble(args[1]);
            dz = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "dx/dz には数値を入力してください。");
            return;
        }
        Player player = (Player) sender;
        ensureMapExists(player.getWorld());
        JumpPad pad = new JumpPad(player.getLocation(), dx, dz);
        gameManager.getKassenMap().addJumpPad(pad);
        sender.sendMessage(ChatColor.GREEN + "ジャンプ台を " + formatLoc(player.getLocation()) + " に追加しました。");
    }

    private void handleSetRole(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "プレイヤーのみ実行できます。");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "使用法: /kassen setrole <attacker|tank|support>");
            return;
        }
        Player player = (Player) sender;
        KassenPlayer kp = gameManager.getKassenPlayer(player);
        if (kp == null) {
            sender.sendMessage(ChatColor.RED + "まずゲームに参加してください (/kassen join)。");
            return;
        }
        PlayerRole role;
        try {
            role = PlayerRole.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "不明な役割です (attacker/tank/support)。");
            return;
        }
        kp.setRole(role);
        sender.sendMessage(ChatColor.GREEN + "役割を " + role.getDisplayName()
                + " (" + role.getDescription() + ") に設定しました。");
    }

    private void handleSpecial(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "プレイヤーのみ実行できます。");
            return;
        }
        gameManager.useSpecial((Player) sender);
    }

    private void handleStatus(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== KASSEN ステータス ===");
        sender.sendMessage(ChatColor.YELLOW + "状態: " + ChatColor.WHITE + gameManager.getState().name());
        sender.sendMessage(ChatColor.YELLOW + "モード: " + ChatColor.WHITE
                + gameManager.getGameMode().getDisplayName());
        sender.sendMessage(ChatColor.YELLOW + "ラウンド: " + ChatColor.WHITE
                + gameManager.getCurrentRound());

        GameTeam a = gameManager.getTeamA();
        GameTeam b = gameManager.getTeamB();
        if (a != null) {
            sender.sendMessage(a.getColoredName() + ChatColor.WHITE
                    + " - プレイヤー: " + a.getPlayers().size()
                    + " ラウンド勝利: " + a.getRoundWins());
        }
        if (b != null) {
            sender.sendMessage(b.getColoredName() + ChatColor.WHITE
                    + " - プレイヤー: " + b.getPlayers().size()
                    + " ラウンド勝利: " + b.getRoundWins());
        }
        if (gameManager.getKassenMap() != null) {
            sender.sendMessage(ChatColor.YELLOW + "マップ設定済み: " + ChatColor.WHITE
                    + gameManager.getKassenMap().isConfigured());
        }
    }

    // ──────────────────────────────────────────────
    // Tab completion
    // ──────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("help", "setup", "start", "stop", "join",
                    "setcastle", "setfortress", "addjumppad", "setrole", "special", "status");
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "setup", "start" -> Arrays.asList("SENGOKU", "BATTLE_ROYALE", "PVE_SOLO", "PVP_1V1");
                case "join", "setcastle" -> Arrays.asList("a", "b");
                case "setfortress" -> Arrays.asList("top", "bot");
                case "setrole" -> Arrays.asList("attacker", "tank", "support");
                default -> Collections.emptyList();
            };
        }
        return Collections.emptyList();
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private boolean checkOp(CommandSender sender) {
        if (!sender.isOp() && !sender.hasPermission("kassen.admin")) {
            sender.sendMessage(ChatColor.RED + "このコマンドを実行する権限がありません。");
            return false;
        }
        return true;
    }

    private void ensureMapExists(World world) {
        if (gameManager.getKassenMap() == null) {
            KassenMap map = new KassenMap();
            map.setWorld(world);
            gameManager.setMap(map);
            gameManager.initTeams();
        } else if (gameManager.getKassenMap().getWorld() == null) {
            gameManager.getKassenMap().setWorld(world);
        }
        if (gameManager.getTeamA() == null) {
            gameManager.initTeams();
        }
    }

    private String formatLoc(Location loc) {
        return String.format("(%.0f, %.0f, %.0f)", loc.getX(), loc.getY(), loc.getZ());
    }
}
