package net.fretux.mindmotion.player;

public class SanityCapability implements ISanity {
    private float sanity = 80f;
    private float insanity = 0f;
    private float baseMaxSanity = 80f;
    private float bonusMaxSanity = 0f;
    private float madness = 0f;
    private float baseMaxMadness = 20f;
    private float bonusMaxMadness = 0f;
    private float bonusMadnessDecayPerTick = 0f;
    private int madnessDecayDelayTicks = 0;
    private int madnessStunTicks = 0;
    private boolean madnessBacklashPending = false;

    @Override
    public float getSanity() {
        return sanity;
    }

    @Override
    public void setSanity(float sanity) {
        float max = getMaxSanity();
        float clampedSanity = Math.max(0, Math.min(max, sanity));
        if (clampedSanity > this.sanity && this.insanity > 0f) {
            return;
        }
        this.sanity = clampedSanity;
    }

    @Override
    public float getInsanity() {
        return insanity;
    }

    @Override
    public void setInsanity(float insanity) {
        float max = getMaxSanity();
        float clampedInsanity = Math.max(0, Math.min(max, insanity));
        if (clampedInsanity > this.insanity && this.sanity > 0f) {
            return;
        }
        this.insanity = clampedInsanity;
    }

    @Override
    public float getMaxSanity() {
        return baseMaxSanity + bonusMaxSanity;
    }

    @Override
    public float getMadness() {
        return madness;
    }

    @Override
    public void setMadness(float madness) {
        this.madness = Math.max(0f, Math.min(getMaxMadness(), madness));
    }

    @Override
    public float getMaxMadness() {
        return baseMaxMadness + bonusMaxMadness;
    }

    @Override
    public float getMadnessDecayPerTick() {
        return 0.01f + bonusMadnessDecayPerTick;
    }

    @Override
    public int getMadnessDecayDelayTicks() {
        return madnessDecayDelayTicks;
    }

    @Override
    public void setMadnessDecayDelayTicks(int ticks) {
        this.madnessDecayDelayTicks = Math.max(0, ticks);
    }

    @Override
    public int getMadnessStunTicks() {
        return madnessStunTicks;
    }

    @Override
    public void setMadnessStunTicks(int ticks) {
        this.madnessStunTicks = Math.max(0, ticks);
    }

    @Override
    public boolean isMadnessBacklashPending() {
        return madnessBacklashPending;
    }

    @Override
    public void setMadnessBacklashPending(boolean pending) {
        this.madnessBacklashPending = pending;
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

    public float getBaseMaxSanity() {
        return baseMaxSanity;
    }

    public void setBonusMaxSanity(float bonusMaxSanity) {
        this.bonusMaxSanity = Math.max(0f, bonusMaxSanity);
        clampToMax();
    }

    public float getBonusMaxSanity() {
        return bonusMaxSanity;
    }

    public void setBaseMaxMadness(float baseMaxMadness) {
        this.baseMaxMadness = Math.max(1f, baseMaxMadness);
        setMadness(this.madness);
    }

    public float getBaseMaxMadness() {
        return baseMaxMadness;
    }

    public void setBonusMaxMadness(float bonusMaxMadness) {
        this.bonusMaxMadness = Math.max(0f, bonusMaxMadness);
        setMadness(this.madness);
    }

    public float getBonusMaxMadness() {
        return bonusMaxMadness;
    }

    public void setBonusMadnessDecayPerTick(float bonusMadnessDecayPerTick) {
        this.bonusMadnessDecayPerTick = Math.max(0f, bonusMadnessDecayPerTick);
    }

    public float getBonusMadnessDecayPerTick() {
        return bonusMadnessDecayPerTick;
    }

    private void clampToMax() {
        float max = getMaxSanity();
        this.sanity = Math.max(0, Math.min(max, this.sanity));
        this.insanity = Math.max(0, Math.min(max, this.insanity));
    }
}
