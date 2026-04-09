package myAgent;

public class Individual {

    public double[] genome;
    public double fitness;

    public Individual(double[] genome) {
        this.genome = genome;
        this.fitness = -Double.MAX_VALUE;
    }
}