package io.github.zakuLv1.kassen.entity;

import io.github.zakuLv1.kassen.map.Fortress;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Vindicator;

/**
 * Represents a mini boss (中ボス) that guards a fortress (櫓).
 * The fortress cannot be captured until this boss is defeated.
 * Mini bosses are implemented as Vindicators with boosted attributes.
 */
public class MiniBoss {

    private final Fortress fortress;
    private final Vindicator entity;

    public MiniBoss(Fortress fortress, Vindicator entity) {
        this.fortress = fortress;
        this.entity = entity;
    }

    /**
     * Returns whether this boss's underlying entity is still alive.
     */
    public boolean isAlive() {
        return entity != null && !entity.isDead();
    }

    /**
     * Called when the boss is killed. Notifies the associated fortress.
     */
    public void onDeath() {
        fortress.onBossDefeated();
    }

    public Fortress getFortress() {
        return fortress;
    }

    public Vindicator getEntity() {
        return entity;
    }
}
