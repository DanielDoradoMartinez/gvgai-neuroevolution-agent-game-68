package myAgent;

import java.util.ArrayList;
import java.util.Random;

public class Evolution {

    private final int populationSize;
    private final int eliteSize;

    private double mutationRate;
    private double mutationStd;
    private int tournamentSize;
    private double immigrantRate;

    private final Random rnd;

    public Evolution(int populationSize, int eliteSize, double mutationRate, double mutationStd, long seed) {
        this.populationSize = populationSize;
        this.eliteSize = eliteSize;
        this.mutationRate = mutationRate;
        this.mutationStd = mutationStd;
        this.tournamentSize = 3;
        this.immigrantRate = 0.08;
        this.rnd = new Random(seed);
    }

    // Crea la población inicial con genomas aleatorios
    public ArrayList<Individual> createInitialPopulation(int genomeLength) {
        ArrayList<Individual> population = new ArrayList<>();

        for (int i = 0; i < populationSize; i++) {
            double[] genome = randomGenome(genomeLength);
            population.add(new Individual(genome));
        }

        return population;
    }

    // Evalúa todos los individuos de la población
    public void evaluatePopulation(ArrayList<Individual> population, Evaluator evaluator) {
        for (Individual ind : population) {
            ind.fitness = evaluator.evaluate(ind.genome);
        }
    }

    // Ordena la población de mayor fitness a menor
    public void sort(ArrayList<Individual> population) {
        population.sort((a, b) -> Double.compare(b.fitness, a.fitness));
    }

    // Genera la siguiente generación a partir de la actual
    public ArrayList<Individual> nextGeneration(ArrayList<Individual> population) {
        sort(population);

        ArrayList<Individual> newPopulation = new ArrayList<>();
        int genomeLength = population.get(0).genome.length;
        int realEliteSize = Math.min(eliteSize, population.size());

        // Guardamos directamente los mejores individuos
        for (int i = 0; i < realEliteSize; i++) {
            newPopulation.add(new Individual(population.get(i).genome.clone()));
        }

        // Añadimos algunos individuos completamente aleatorios para no perder diversidad
        int immigrants = (int) Math.round(populationSize * immigrantRate);

        for (int i = 0; i < immigrants && newPopulation.size() < populationSize; i++) {
            newPopulation.add(new Individual(randomGenome(genomeLength)));
        }

        // El resto de la población se genera a partir de cruces y mutaciones
        while (newPopulation.size() < populationSize) {
            Individual parent1 = tournamentSelection(population, tournamentSize);
            Individual parent2 = tournamentSelection(population, tournamentSize);

            double[] childGenome;

            if (rnd.nextDouble() < 0.70) {
                childGenome = arithmeticCrossover(parent1.genome, parent2.genome);
            } else {
                childGenome = uniformCrossover(parent1.genome, parent2.genome);
            }

            mutate(childGenome);
            newPopulation.add(new Individual(childGenome));
        }

        return newPopulation;
    }

    // Selección por torneo: se escogen varios al azar y nos quedamos con el mejor
    private Individual tournamentSelection(ArrayList<Individual> population, int tournamentSize) {
        Individual best = null;

        for (int i = 0; i < tournamentSize; i++) {
            Individual candidate = population.get(rnd.nextInt(population.size()));

            if (best == null || candidate.fitness > best.fitness) {
                best = candidate;
            }
        }

        return best;
    }

    // Cruce aritmético: mezcla los genes usando una proporción aleatoria
    private double[] arithmeticCrossover(double[] parentA, double[] parentB) {
        double[] child = new double[parentA.length];

        for (int i = 0; i < parentA.length; i++) {
            double alpha = rnd.nextDouble();
            child[i] = alpha * parentA[i] + (1.0 - alpha) * parentB[i];
        }

        return child;
    }

    // Cruce uniforme: para cada gen se elige el de uno de los dos padres
    private double[] uniformCrossover(double[] parentA, double[] parentB) {
        double[] child = new double[parentA.length];

        for (int i = 0; i < parentA.length; i++) {
            if (rnd.nextBoolean()) {
                child[i] = parentA[i];
            } else {
                child[i] = parentB[i];
            }
        }

        return child;
    }

    // Aplica mutación a cada gen con cierta probabilidad
    private void mutate(double[] genome) {
        for (int i = 0; i < genome.length; i++) {
            if (rnd.nextDouble() < mutationRate) {
                genome[i] += rnd.nextGaussian() * mutationStd;
                genome[i] = clamp(genome[i], -3.0, 3.0);
            }
        }
    }

    // Genera un genoma aleatorio
    private double[] randomGenome(int genomeLength) {
        double[] genome = new double[genomeLength];

        for (int i = 0; i < genomeLength; i++) {
            genome[i] = rnd.nextGaussian() * 0.20;
        }

        return genome;
    }

    // Limita un valor entre un mínimo y un máximo
    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public int getPopulationSize() {
        return populationSize;
    }

    public int getEliteSize() {
        return eliteSize;
    }

    public double getMutationRate() {
        return mutationRate;
    }

    public double getMutationStd() {
        return mutationStd;
    }

    public int getTournamentSize() {
        return tournamentSize;
    }

    public double getImmigrantRate() {
        return immigrantRate;
    }

    public void setMutationRate(double mutationRate) {
        this.mutationRate = mutationRate;
    }

    public void setMutationStd(double mutationStd) {
        this.mutationStd = mutationStd;
    }

    public void setTournamentSize(int tournamentSize) {
        this.tournamentSize = Math.max(2, tournamentSize);
    }

    public void setImmigrantRate(double immigrantRate) {
        this.immigrantRate = Math.max(0.0, Math.min(0.30, immigrantRate));
    }
}