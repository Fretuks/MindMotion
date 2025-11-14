package net.fretux.mindmotion.player;

public class TempoCapability implements ITempo {
    private int tempo = 0;
    private final int maxTempo = 120;

    @Override
    public int getTempo() { return tempo; }
    @Override
    public void setTempo(int tempo) { this.tempo = Math.max(0, Math.min(maxTempo, tempo)); }
    @Override
    public int getMaxTempo() { return maxTempo; }
    @Override
    public void addTempo(int amount) { setTempo(this.tempo + amount); }
    @Override
    public void reduceTempo(int amount) { setTempo(this.tempo - amount); }
}
