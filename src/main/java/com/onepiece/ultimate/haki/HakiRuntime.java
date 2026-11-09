package com.onepiece.ultimate.haki;

/** Server-only Haki timers. */
public final class HakiRuntime {
    public long kenDodgeReadyAt;
    public long futureSightUntil;
    public int threatTimer;
    public int lastThreatHash;
    /** Haki can't be used until this game time (Gear Fourth aftermath). */
    public long exhaustedUntil;

    public void reset() {
        kenDodgeReadyAt = 0;
        futureSightUntil = 0;
        threatTimer = 0;
        lastThreatHash = 0;
        exhaustedUntil = 0;
    }
}
