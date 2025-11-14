package net.fretux.mindmotion.player;

public interface ISanity {
    float getSanity();
    void setSanity(float sanity);
    float getInsanity();
    void setInsanity(float insanity);
    float getMaxSanity();
    void addSanity(float amount);
    void reduceSanity(float amount);
}
