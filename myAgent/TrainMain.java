package myAgent;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import core.game.StateObservation;
import tools.Utils;

public class TrainMain {

    public static void main(String[] args) {

        // Archivo con la lista de juegos de GVGAI
        String spGamesCollection = "examples/all_games_sp.csv";
        String[][] games = Utils.readGames(spGamesCollection);

        // Índice del juego que queremos usar
        int gameIdx = 68;

        String gameName = games[gameIdx][1];
        String gameFile = games[gameIdx][0];

        // Niveles que vamos a usar durante el entrenamiento
        int[] levelIndices = {0, 1, 2, 3, 4};

        // Parámetros principales de la evolución
        int generations = 140;
        int populationSize = 60;
        int eliteSize = 3;

        double mutationRate = 0.12;
        double mutationStd = 0.18;

        long globalSeed = System.nanoTime();
        int maxTicksPerEpisode = 700;

        // Configuración de los escenarios de evaluación
        int totalScenarios = 18;
        int recycleEvery = 4;
        double keepPercent = 0.65;

        ArrayList<Scenario> currentScenarios = new ArrayList<>();
        Random globalRnd = new Random(globalSeed);

        // Fichero donde se guarda la élite al final
        String eliteFile = "elite_final.txt";

        // Creamos un estado inicial solo para saber cuántos genes necesita la red
        String firstLevel = buildLevelPath(gameFile, gameName, levelIndices[0]);
        StateObservation initialState = GameFactory.createInitialState(gameFile, firstLevel, 123);

        Brain tempBrain = new Brain(initialState);
        int genomeLength = tempBrain.getGenomeLength();

        // Creamos el algoritmo evolutivo y la población inicial
        Evolution evolution = new Evolution(
            populationSize,
            eliteSize,
            mutationRate,
            mutationStd,
            globalSeed
        );

        ArrayList<Individual> population = evolution.createInitialPopulation(genomeLength);

        // Generamos los primeros escenarios
        for (int i = 0; i < totalScenarios; i++) {
            currentScenarios.add(randomScenario(gameFile, gameName, levelIndices, globalRnd));
        }

        // Variables para controlar si el entrenamiento se estanca
        double globalBestFitness = -Double.MAX_VALUE;
        int stagnantGenerations = 0;

        // Bucle principal de evolución
        for (int gen = 0; gen < generations; gen++) {

            adaptParameters(evolution, gen, generations, stagnantGenerations);

            // Cada cierto número de generaciones cambiamos parte de los escenarios
            if (gen > 0 && gen % recycleEvery == 0) {
                currentScenarios = recycleScenarios(
                    currentScenarios,
                    totalScenarios,
                    keepPercent,
                    gameFile,
                    gameName,
                    levelIndices,
                    globalRnd
                );
            }

            Evaluator evaluator = new Evaluator(currentScenarios, maxTicksPerEpisode);
            evolution.evaluatePopulation(population, evaluator);
            evolution.sort(population);

            Individual best = population.get(0);
            double meanFitness = computeMeanFitness(population);

            if (best.fitness > globalBestFitness) {
                globalBestFitness = best.fitness;
                stagnantGenerations = 0;
            } else {
                stagnantGenerations++;
            }

            System.out.println(
                "Gen " + gen +
                " | best=" + best.fitness +
                " | mean=" + meanFitness +
                " | mutRate=" + evolution.getMutationRate() +
                " | mutStd=" + evolution.getMutationStd() +
                " | immigrants=" + evolution.getImmigrantRate()
            );

            // Creamos la siguiente generación salvo en la última iteración
            if (gen < generations - 1) {
                population = evolution.nextGeneration(population);
            }
        }

        // Ordenamos una última vez y guardamos la élite final
        evolution.sort(population);
        saveElite(population, eliteSize, eliteFile);

        System.out.println("Mejor fitness final: " + population.get(0).fitness);
    }

    // Ajusta los parámetros evolutivos según el progreso del entrenamiento
    private static void adaptParameters(Evolution evolution, int gen, int generations, int stagnantGenerations) {
        double progress = (double) gen / (double) Math.max(1, generations - 1);

        if (progress < 0.33) {
            evolution.setMutationRate(0.16);
            evolution.setMutationStd(0.22);
            evolution.setTournamentSize(3);
            evolution.setImmigrantRate(0.10);
        } else if (progress < 0.75) {
            evolution.setMutationRate(0.10);
            evolution.setMutationStd(0.14);
            evolution.setTournamentSize(3);
            evolution.setImmigrantRate(0.08);
        } else {
            evolution.setMutationRate(0.06);
            evolution.setMutationStd(0.08);
            evolution.setTournamentSize(4);
            evolution.setImmigrantRate(0.05);
        }

        // Si llevamos varias generaciones sin mejorar, aumentamos exploración
        if (stagnantGenerations >= 4) {
            evolution.setMutationRate(Math.min(0.22, evolution.getMutationRate() + 0.04));
            evolution.setMutationStd(Math.min(0.30, evolution.getMutationStd() + 0.05));
            evolution.setImmigrantRate(Math.min(0.18, evolution.getImmigrantRate() + 0.04));
        }
    }

    // Calcula la fitness media de toda la población
    private static double computeMeanFitness(ArrayList<Individual> population) {
        double mean = 0.0;

        for (Individual ind : population) {
            mean += ind.fitness;
        }

        return mean / Math.max(1, population.size());
    }

    // Mantiene una parte de los escenarios y sustituye el resto por otros nuevos
    private static ArrayList<Scenario> recycleScenarios(
        ArrayList<Scenario> currentScenarios,
        int totalScenarios,
        double keepPercent,
        String gameFile,
        String gameName,
        int[] levelIndices,
        Random rnd
    ) {
        int keepCount = (int) Math.round(totalScenarios * keepPercent);
        int replaceCount = totalScenarios - keepCount;

        Collections.shuffle(currentScenarios, rnd);

        ArrayList<Scenario> newScenarios = new ArrayList<>();

        for (int i = 0; i < keepCount && i < currentScenarios.size(); i++) {
            newScenarios.add(currentScenarios.get(i));
        }

        for (int i = 0; i < replaceCount; i++) {
            newScenarios.add(randomScenario(gameFile, gameName, levelIndices, rnd));
        }

        return newScenarios;
    }

    // Construye la ruta del nivel a partir del nombre del juego y el índice
    private static String buildLevelPath(String gameFile, String gameName, int levelIdx) {
        return gameFile.replace(gameName, gameName + "_lvl" + levelIdx);
    }

    // Genera un escenario aleatorio escogiendo nivel y seed
    private static Scenario randomScenario(String gameFile, String gameName, int[] levelIndices, Random rnd) {
        int chosenLevel = levelIndices[rnd.nextInt(levelIndices.length)];
        String levelFile = buildLevelPath(gameFile, gameName, chosenLevel);
        int seed = rnd.nextInt();

        return new Scenario(gameFile, levelFile, seed);
    }

    // Guarda los mejores individuos en un fichero de texto
    private static void saveElite(ArrayList<Individual> population, int eliteSize, String fileName) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            int realEliteSize = Math.min(eliteSize, population.size());

            writer.write("ELITE FINAL\n");
            writer.write("Tamano elite: " + realEliteSize + "\n\n");

            for (int i = 0; i < realEliteSize; i++) {
                Individual ind = population.get(i);

                writer.write("INDIVIDUO " + i + "\n");
                writer.write("fitness=" + ind.fitness + "\n");
                writer.write("genomeLength=" + ind.genome.length + "\n");
                writer.write("genome=");

                for (int g = 0; g < ind.genome.length; g++) {
                    writer.write(Double.toString(ind.genome[g]));
                    if (g < ind.genome.length - 1) {
                        writer.write(",");
                    }
                }

                writer.write("\n\n");
            }

        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar la elite", e);
        }
    }
}