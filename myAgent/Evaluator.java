package myAgent;

import java.util.ArrayList;

import core.game.StateObservation;
import ontology.Types;
import ontology.Types.ACTIONS;

public class Evaluator {

    private final ArrayList<Scenario> scenarios;
    private final int maxTicksPerEpisode;

    public Evaluator(ArrayList<Scenario> scenarios, int maxTicksPerEpisode) {
        this.scenarios = scenarios;
        this.maxTicksPerEpisode = maxTicksPerEpisode;
    }

    // Evalúa un genoma en todos los escenarios y devuelve la media
    public double evaluate(double[] genome) {
        double totalFitness = 0.0;

        for (Scenario scenario : scenarios) {
            totalFitness += evaluateOneScenario(genome, scenario);
        }

        return totalFitness / Math.max(1, scenarios.size());
    }

    // Evalúa un genoma en un escenario concreto
    private double evaluateOneScenario(double[] genome, Scenario scenario) {
        StateObservation state = GameFactory.createInitialState(
            scenario.gameFile,
            scenario.levelFile,
            scenario.seed
        );

        Brain brain = new Brain(state, genome);

        double initialScore = state.getGameScore();
        double bestScoreSeen = initialScore;
        int ticks = 0;

        // Simula la partida hasta que termine o se llegue al máximo de ticks
        while (!state.isGameOver() && ticks < maxTicksPerEpisode) {
            ACTIONS action = brain.nextAction(state);
            state.advance(action);

            if (state.getGameScore() > bestScoreSeen) {
                bestScoreSeen = state.getGameScore();
            }

            ticks++;
        }

        return calculateFitness(state, initialScore, bestScoreSeen, ticks, brain);
    }

    // Calcula la fitness final del episodio
    private double calculateFitness(
        StateObservation finalState,
        double initialScore,
        double bestScoreSeen,
        int ticks,
        Brain brain
    ) {
        double fitness = 0.0;

        double finalScore = finalState.getGameScore();
        double progress = bestScoreSeen - initialScore;

        // Parte principal de la fitness: score final y progreso conseguido
        fitness += finalScore * 120.0;
        fitness += progress * 180.0;

        // Recompensas secundarias por sobrevivir, explorar y moverse por el mapa
        fitness += ticks * 1.5;
        fitness += brain.getVisitedRatio() * 150.0;
        fitness += brain.getExplorationMoves() * 3.0;

        // Pequeñas recompensas y castigos por el comportamiento local
        fitness += brain.getSafeMoves() * 8.0;
        fitness -= brain.getDangerousMoves() * 14.0;
        fitness += brain.getScoreImprovements() * 25.0;

        // Penalizaciones por quedarse parado, repetir demasiado o atascarse
        fitness -= brain.getIdleActions() * 4.0;
        fitness -= brain.getRepeatedActions() * 1.5;
        fitness -= brain.getUselessBacktracks() * 2.0;
        fitness -= brain.getTrappedMoves() * 5.0;
        fitness -= Math.min(brain.getTicksWithoutScoreImprovement(), 100) * 0.8;

        // Bonus o castigo fuerte según el resultado final de la partida
        if (finalState.isGameOver()) {
            if (finalState.getGameWinner() == Types.WINNER.PLAYER_WINS) {
                fitness += 15000.0;
            } else if (finalState.getGameWinner() == Types.WINNER.PLAYER_LOSES) {
                fitness -= 6000.0;
            }
        }

        return fitness;
    }

    public ArrayList<Scenario> getScenarios() {
        return scenarios;
    }

    public int getMaxTicksPerEpisode() {
        return maxTicksPerEpisode;
    }
}