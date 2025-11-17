package net.fretux.mindmotion.player;

public class TempoCapability implements ITempo {
    private int tempo = 0;
    private int baseMaxTempo = 120;
    private float bonusMaxTempo = 0f;
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
        int oldMax = getMaxTempo();
        this.bonusMaxTempo = Math.max(0f, bonusMaxTempo);
        int newMax = getMaxTempo();
        if (oldMax > 0) {
            float pct = (float) tempo / (float) oldMax;
            tempo = Math.round(pct * newMax);
        }
        setTempo(tempo);
    }

    public float getBonusMaxTempo() {
        return bonusMaxTempo;
    }
}