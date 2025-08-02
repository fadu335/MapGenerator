package difficulty;

public enum Difficulty {
    EASY,
    MEDIUM,
    HARD;

    public Difficulty next() {
        return values()[(this.ordinal() + 1) % values().length];
    }
}
