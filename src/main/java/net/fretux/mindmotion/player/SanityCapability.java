package net.fretux.mindmotion.player;

public class SanityCapability implements ISanity {
    private float sanity = 80f;
    private float insanity = 0f;
    private final float maxSanity = 80f;

    @Override
    public float getSanity() { return sanity; }
    @Override
    public void setSanity(float sanity) { this.sanity = Math.max(0, Math.min(maxSanity, sanity)); }

    @Override
    public float getInsanity() { return insanity; }
    @Override
    public void setInsanity(float insanity) { this.insanity = Math.max(0, Math.min(maxSanity, insanity)); }

    @Override
    public float getMaxSanity() { return maxSanity; }

    @Override
    public void addSanity(float amount) { setSanity(this.sanity + amount); }
    @Override
    public void reduceSanity(float amount) { setSanity(this.sanity - amount); }
}
