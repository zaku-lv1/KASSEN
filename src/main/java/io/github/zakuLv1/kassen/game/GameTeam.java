package io.github.zakuLv1.kassen.game;

import io.github.zakuLv1.kassen.map.Fortress;
import io.github.zakuLv1.kassen.player.KassenPlayer;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents one team in a KASSEN game.
 * Holds team members, round wins, and (for Battle Royale) shared lives.
 */
public class GameTeam {

    public static final int TEAM_A_ID = 0;
    public static final int TEAM_B_ID = 1;

    private final int id;
    private final String displayName;
    private final ChatColor color;

    private final List<KassenPlayer> players = new ArrayList<>();

    /** Rounds won in the current game (SENGOKU mode). */
    private int roundWins;

    /** Shared team lives remaining (Battle Royale mode). */
    private int sharedLives;

    public GameTeam(int id, String displayName, ChatColor color) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
    }

    /**
     * Adds a player to this team.
     */
    public void addPlayer(KassenPlayer player) {
        players.add(player);
        player.setTeam(this);
    }

    /**
     * Removes a player from this team.
     */
    public void removePlayer(KassenPlayer player) {
        players.remove(player);
    }

    /**
     * Returns true if all players on this team are eliminated.
     */
    public boolean isWiped() {
        for (KassenPlayer p : players) {
            if (!p.isEliminated()) {
                return false;
            }
        }
        return !players.isEmpty();
    }

    /**
     * Returns the number of living (not eliminated) players.
     */
    public int getAlivePlayers() {
        int count = 0;
        for (KassenPlayer p : players) {
            if (!p.isEliminated()) count++;
        }
        return count;
    }

    /**
     * Resets all players for a new round.
     *
     * @param maxLives per-player lives to restore
     */
    public void resetForRound(int maxLives) {
        for (KassenPlayer p : players) {
            p.resetForRound(maxLives);
        }
    }

    /**
     * Decrements shared lives by one (used in Battle Royale mode).
     *
     * @return true if the team still has shared lives remaining
     */
    public boolean decrementSharedLives() {
        if (sharedLives > 0) {
            sharedLives--;
        }
        return sharedLives > 0;
    }

    /**
     * Returns true if the team's shared lives are exhausted (Battle Royale mode).
     */
    public boolean hasNoSharedLives() {
        return sharedLives <= 0;
    }

    public void incrementRoundWins() {
        roundWins++;
    }

    public int getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ChatColor getColor() {
        return color;
    }

    public List<KassenPlayer> getPlayers() {
        return players;
    }

    public int getRoundWins() {
        return roundWins;
    }

    public void setRoundWins(int roundWins) {
        this.roundWins = roundWins;
    }

    public int getSharedLives() {
        return sharedLives;
    }

    public void setSharedLives(int sharedLives) {
        this.sharedLives = sharedLives;
    }

    /**
     * Returns the colored display name.
     */
    public String getColoredName() {
        return color + displayName + ChatColor.RESET;
    }
}
