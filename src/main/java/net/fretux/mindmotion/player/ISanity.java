package net.fretux.mindmotion.player;

public interface ISanity {
    float getSanity();
    void setSanity(float sanity);
    float getInsanity();
    void setInsanity(float insanity);
    float getMaxSanity();
    float getMadness();
    void setMadness(float madness);
    float getMaxMadness();
    float getMadnessDecayPerTick();
    int getMadnessDecayDelayTicks();
    void setMadnessDecayDelayTicks(int ticks);
    int getMadnessStunTicks();
    void setMadnessStunTicks(int ticks);
    boolean isMadnessBacklashPending();
    void setMadnessBacklashPending(boolean pending);
    void addSanity(float amount);
    void reduceSanity(float amount);
}
