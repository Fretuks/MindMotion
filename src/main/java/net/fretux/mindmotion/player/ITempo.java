package net.fretux.mindmotion.player;

public interface ITempo {
    int getTempo();

    void setTempo(int tempo);

    int getMaxTempo();

    void addTempo(int amount);

    void reduceTempo(int amount);

    int getVentCooldown();

    void setVentCooldown(int ticks);
}
