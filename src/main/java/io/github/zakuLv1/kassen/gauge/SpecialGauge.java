package io.github.zakuLv1.kassen.gauge;

import io.github.zakuLv1.kassen.KassenPlugin;

/**
 * Manages a player's special attack gauge (必殺技ゲージ).
 * The gauge fills by killing minions, players, and bosses.
 * When full, the player can use their special attack.
 */
public class SpecialGauge {

    private final KassenPlugin plugin;
    private int current;
    private final int max;

    public SpecialGauge(KassenPlugin plugin) {
        this.plugin = plugin;
        this.current = 0;
        this.max = plugin.getConfig().getInt("player.gauge-max", 100);
    }

    /**
     * Adds gauge amount for killing a minion.
     */
    public void addMinionKill() {
        add(plugin.getConfig().getInt("minion.gauge-per-kill", 10));
    }

    /**
     * Adds gauge amount for killing a player.
     */
    public void addPlayerKill() {
        add(plugin.getConfig().getInt("player.gauge-per-player-kill", 20));
    }

    /**
     * Adds gauge amount for killing a boss.
     */
    public void addBossKill() {
        add(plugin.getConfig().getInt("boss.gauge-per-kill", 30));
    }

    /**
     * Adds a specific amount to the gauge.
     *
     * @param amount amount to add
     */
    public void add(int amount) {
        current = Math.min(current + amount, max);
    }

    /**
     * Returns whether the gauge is full and the special can be used.
     *
     * @return true if the gauge is at max
     */
    public boolean isFull() {
        return current >= max;
    }

    /**
     * Uses the special attack, consuming the full gauge.
     *
     * @return true if the special was used successfully
     */
    public boolean use() {
        if (!isFull()) {
            return false;
        }
        current = 0;
        return true;
    }

    /**
     * Resets the gauge to zero.
     */
    public void reset() {
        current = 0;
    }

    public int getCurrent() {
        return current;
    }

    public int getMax() {
        return max;
    }

    /**
     * Returns the gauge fill percentage (0-100).
     */
    public int getPercent() {
        return (int) ((current / (double) max) * 100);
    }
}
