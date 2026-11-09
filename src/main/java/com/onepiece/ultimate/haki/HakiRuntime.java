package com.onepiece.ultimate.haki;

/** Server-only Haki timers. */
public final class HakiRuntime {
    public long kenDodgeReadyAt;
    public long futureSightUntil;
    public int threatTimer;
    public int lastThreatHash;

    public void reset() {
        kenDodgeReadyAt = 0;
        futureSightUntil = 0;
        threatTimer = 0;
        lastThreatHash = 0;
    }
}
