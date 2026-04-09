package myAgent;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;

public class Agente extends AbstractPlayer {

    private Brain brain;

    // Fichero donde se guarda el mejor genoma entrenado
    private static final String ELITE_FILE = "elite_final.txt";

    public Agente(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        double[] bestGenome = loadBestGenomeFromFile(ELITE_FILE);

        // Si se puede cargar el genoma, usamos ese.
        // Si no, se crea un cerebro aleatorio.
        if (bestGenome != null) {
            this.brain = new Brain(stateObs, bestGenome);
        } else {
            this.brain = new Brain(stateObs);
        }
    }

    public Agente(StateObservation stateObs, ElapsedCpuTimer elapsedTimer, double[] genome) {
        this.brain = new Brain(stateObs, genome);
    }

    @Override
    public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        return brain.nextAction(stateObs);
    }

    public Brain getBrain() {
        return brain;
    }

    // Lee del fichero el primer genoma guardado
    private double[] loadBestGenomeFromFile(String fileName) {
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;

            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("genome=")) {
                    continue;
                }

                String genomeText = line.substring("genome=".length()).trim();
                String[] parts = genomeText.split(",");
                double[] genome = new double[parts.length];

                for (int i = 0; i < parts.length; i++) {
                    genome[i] = Double.parseDouble(parts[i]);
                }

                return genome;
            }

        } catch (IOException | NumberFormatException e) {
            // Si hay algún problema leyendo o parseando el fichero,
            // devolvemos null y luego se usará un brain aleatorio.
        }

        return null;
    }
}