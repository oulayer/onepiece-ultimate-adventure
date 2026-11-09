package com.onepiece.ultimate.combat;

/** Server-only melee state machine of one player. All times are level game time. */
public final class CombatState {
    // light combo
    public int comboStep;
    public long comboExpire;
    public long recoveryUntil;
    public boolean bufferedLight;

    // heavy
    public boolean heavyCharging;
    public long heavyStart;

    // defence
    public long blockStart = -1000;
    public long iframesUntil;
    public boolean perfectDodgeTriggered = true;
    public long counterUntil;

    // movement
    public long dashReadyAt;
    public boolean airDashUsed;

    // aerial slam
    public boolean slamPending;
    public long slamStart;
    public double slamStartY;
    public long slamLandedAt = -1000;

    // HUD hit counter
    public int comboHits;
    public long comboHitsExpire;

    public long lastCombat = -10000;

    public void markInCombat(long now) {
        lastCombat = now;
    }

    public boolean inCombat(long now) {
        return now - lastCombat < 100;
    }

    public void reset() {
        comboStep = 0;
        bufferedLight = false;
        heavyCharging = false;
        slamPending = false;
        comboHits = 0;
    }
}
