package net.fretux.mindmotion.client;

public class ClientData {
    public static float SANITY = 80f;
    public static float INSANITY = 0f;
    public static int TEMPO = 0;
    public static int VENT_COOLDOWN = 0;
    public static float MADNESS = 0f;
    public static float MAX_MADNESS = 20f;
    public static float MADNESS_DECAY_PER_TICK = 0.01f;
    public static float MADNESS_DECAY_DELAY_TICKS = 0f;
    private static float displayedSanity = 80f;
    private static float displayedInsanity = 0f;
    private static float displayedTempo = 0f;
    private static float displayedMadness = 0f;
    private static float madnessRenderTarget = 0f;
    private static long lastMadnessRenderNanos = System.nanoTime();

    // NEW: synced from server
    public static float MAX_SANITY = 80f;
    public static int MAX_TEMPO = 120;
    public static boolean SANITY_ENABLED = true;
    public static boolean TEMPO_ENABLED = true;

    public static void setMadness(float madness) {
        MADNESS = Math.max(0f, madness);
        madnessRenderTarget = MADNESS;
    }

    public static void updateInterpolatedValues() {
        long now = System.nanoTime();
        float elapsedSeconds = Math.min(0.25f, (now - lastMadnessRenderNanos) / 1_000_000_000f);
        lastMadnessRenderNanos = now;

        float elapsedTicks = elapsedSeconds * 20f;
        float pausedTicks = Math.min(elapsedTicks, MADNESS_DECAY_DELAY_TICKS);
        MADNESS_DECAY_DELAY_TICKS -= pausedTicks;
        float decayTicks = elapsedTicks - pausedTicks;
        if (decayTicks > 0f) {
            madnessRenderTarget = Math.max(0f,
                    madnessRenderTarget - decayTicks * MADNESS_DECAY_PER_TICK);
        }

        float lerp = 1f - (float) Math.exp(-10f * elapsedSeconds);
        displayedSanity += (SANITY - displayedSanity) * lerp;
        displayedInsanity += (INSANITY - displayedInsanity) * lerp;
        displayedTempo += (TEMPO - displayedTempo) * lerp;
        displayedMadness += (madnessRenderTarget - displayedMadness) * lerp;
    }

    public static float getDisplayedSanity() {
        return displayedSanity;
    }

    public static float getDisplayedInsanity() {
        return displayedInsanity;
    }

    public static float getDisplayedTempo() {
        return displayedTempo;
    }

    public static float getDisplayedMadness() {
        return displayedMadness;
    }
}
