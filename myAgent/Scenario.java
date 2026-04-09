package myAgent;

public class Scenario {

    public String gameFile;
    public String levelFile;
    public int seed;

    public Scenario(String gameFile, String levelFile, int seed) {
        this.gameFile = gameFile;
        this.levelFile = levelFile;
        this.seed = seed;
    }
}