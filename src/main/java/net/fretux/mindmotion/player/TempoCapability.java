package net.fretux.mindmotion.player;

public class TempoCapability implements ITempo {
    private int tempo = 0;

    private int baseMaxTempo = 120;
    private float bonusMaxTempo = 0f; // from Ascend willpower

    private int ventCooldown = 0;

    @Override
    public int getTempo() {
        return tempo;
    }

    @Override
    public void setTempo(int tempo) {
        this.tempo = Math.max(0, Math.min(getMaxTempo(), tempo));
    }

    @Override
    public int getMaxTempo() {
        return Math.max(0, Math.round(baseMaxTempo + bonusMaxTempo));
    }

    @Override
    public void addTempo(int amount) {
        setTempo(this.tempo + amount);
    }

    @Override
    public void reduceTempo(int amount) {
        setTempo(this.tempo - amount);
    }

    @Override
    public int getVentCooldown() {
        return ventCooldown;
    }

    @Override
    public void setVentCooldown(int ticks) {
        ventCooldown = Math.max(0, ticks);
    }

    public void setBaseMaxTempo(int baseMaxTempo) {
        this.baseMaxTempo = baseMaxTempo;
        setTempo(this.tempo);
    }

    public void setBonusMaxTempo(float bonusMaxTempo) {
        this.bonusMaxTempo = Math.max(0f, bonusMaxTempo);
        setTempo(this.tempo);
    }

    public float getBonusMaxTempo() {
        return bonusMaxTempo;
    }
}