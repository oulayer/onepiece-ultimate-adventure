package com.onepiece.ultimate.client;

/** Small client-side UI state (combo counter pop animation etc.). */
public final class ClientState {
    public static int comboDisplay;
    public static int comboPopTicks;
    public static int comboFadeTicks;
    /** Entity ids with hostile intent, sensed by Observation Haki. */
    public static int[] threats = new int[0];

    public static void onComboHit(int hits) {
        comboDisplay = hits;
        comboPopTicks = 6;
        comboFadeTicks = 60;
    }

    public static void tick() {
        if (comboPopTicks > 0) comboPopTicks--;
        if (comboFadeTicks > 0) comboFadeTicks--;
    }

    public static void reset() {
        comboDisplay = comboPopTicks = comboFadeTicks = 0;
        threats = new int[0];
    }

    private ClientState() {}
}
