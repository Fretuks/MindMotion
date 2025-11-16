package net.fretux.mindmotion.client.shader;

public class VentShaderHandler {

    private static long shockwaveEnd = 0;

    public static void triggerVentShockwave() {
        shockwaveEnd = System.currentTimeMillis() + 400; // 0.4 seconds
    }

    public static boolean isActive() {
        return System.currentTimeMillis() < shockwaveEnd;
    }

    public static float getStrength() {
        float remain = (shockwaveEnd - System.currentTimeMillis()) / 400f;
        return Math.max(0f, Math.min(1f, 1f - remain));
    }
}