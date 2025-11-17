package net.fretux.mindmotion.player;

public class SanityCapability implements ISanity {
    private float sanity = 80f;
    private float insanity = 0f;
    private float baseMaxSanity = 80f;
    private float bonusMaxSanity = 0f;

    @Override
    public float getSanity() {
        return sanity;
    }

    @Override
    public void setSanity(float sanity) {
        float max = getMaxSanity();
        this.sanity = Math.max(0, Math.min(max, sanity));
    }

    @Override
    public float getInsanity() {
        return insanity;
    }

    @Override
    public void setInsanity(float insanity) {
        float max = getMaxSanity();
        this.insanity = Math.max(0, Math.min(max, insanity));
    }

    @Override
    public float getMaxSanity() {
        return baseMaxSanity + bonusMaxSanity;
    }

    @Override
    public void addSanity(float amount) {
        setSanity(this.sanity + amount);
    }

    @Override
    public void reduceSanity(float amount) {
        setSanity(this.sanity - amount);
    }

    public void setBaseMaxSanity(float baseMaxSanity) {
        this.baseMaxSanity = baseMaxSanity;
        setSanity(this.sanity);
        setInsanity(this.insanity);
    }

    public void setBonusMaxSanity(float bonusMaxSanity) {
        float oldMax = getMaxSanity();
        this.bonusMaxSanity = Math.max(0f, bonusMaxSanity);
        float newMax = getMaxSanity();
        if (oldMax > 0) {
            float pct = sanity / oldMax;
            sanity = pct * newMax;
        }
        setSanity(sanity);
        setInsanity(insanity);
    }

    public float getBonusMaxSanity() {
        return bonusMaxSanity;
    }
}