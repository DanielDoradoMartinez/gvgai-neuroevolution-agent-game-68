package myAgent;

import java.util.Random;

import core.game.StateObservation;
import ontology.Types.ACTIONS;

import org.encog.engine.network.activation.ActivationTANH;
import org.encog.ml.data.MLData;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;

public class Brain {

	private Map map;
	private BasicNetwork network;
	private double[] genome;

	private final int width;
	private final int height;

	// Tamaño de la red

	private final int inputSize = 48;
	private final int hidden1Size = 24;
	private final int hidden2Size = 12;
	private final int outputSize = 5;

	private final Random random = new Random();

	// Matriz para contar cuántas veces pasa el avatar por cada celda
	private int[][] visitCounts;

	// Estadísticas del episodio
	private int safeMoves;
	private int dangerousMoves;
	private int idleActions;
	private int repeatedActions;
	private int explorationMoves;
	private int trappedMoves;
	private int uselessBacktracks;
	private int scoreImprovements;
	private int ticksWithoutScoreImprovement;

	private double lastScore;
	private ACTIONS lastAction;

	// Clase auxiliar para guardar información de una dirección concreta
	private static class DirectionData {
		ACTIONS action;
		boolean legal;
		boolean blocked;
		boolean enemy;
		boolean food;
		boolean danger;
		int freeNeighbors;
		int nextVisitCount;
		int nextFoodDistance;
		int nextEnemyDistance;
	}

	public Brain(StateObservation stateObs) {
		this(stateObs, null);
	}

	public Brain(StateObservation stateObs, double[] genome) {
		this.width = stateObs.getObservationGrid().length;
		this.height = stateObs.getObservationGrid()[0].length;

		this.map = new Map(width, height, stateObs);
		this.visitCounts = new int[width][height];

		buildNetwork();

		if (genome == null) {
			this.genome = new double[network.encodedArrayLength()];
			randomizeGenome();
		} else {
			if (genome.length != network.encodedArrayLength()) {
				throw new IllegalArgumentException("Longitud incorrecta del genoma. Esperada: "
						+ network.encodedArrayLength() + ", recibida: " + genome.length);
			}
			this.genome = genome.clone();
		}

		loadGenomeIntoNetwork();
		resetEpisodeStats(stateObs);
	}

	// Reinicia las estadísticas al empezar un episodio
	private void resetEpisodeStats(StateObservation initialState) {
		safeMoves = 0;
		dangerousMoves = 0;
		idleActions = 0;
		repeatedActions = 0;
		explorationMoves = 0;
		trappedMoves = 0;
		uselessBacktracks = 0;
		scoreImprovements = 0;
		ticksWithoutScoreImprovement = 0;
		lastScore = initialState.getGameScore();
		lastAction = null;

		int ax = map.getAvatarX();
		int ay = map.getAvatarY();

		if (map.isInside(ax, ay)) {
			visitCounts[ax][ay]++;
		}
	}

	// actualiza estado, construye entrada, hace forward y decide acción
	public ACTIONS nextAction(StateObservation stateObs) {
		update(stateObs);

		double[] input = buildInputVector(stateObs);
		double[] output = forward(input);

		ACTIONS chosenAction = decodeAction(output, stateObs);
		updateEpisodeStats(stateObs, chosenAction);

		return chosenAction;
	}

	// Actualiza el mapa y registra la posición actual del avatar
	private void update(StateObservation stateObs) {
		map.update(stateObs);

		int ax = map.getAvatarX();
		int ay = map.getAvatarY();

		if (map.isInside(ax, ay)) {
			visitCounts[ax][ay]++;
		}
	}

	// Crea la red neuronal
	private void buildNetwork() {
		network = new BasicNetwork();

		network.addLayer(new BasicLayer(null, true, inputSize));
		network.addLayer(new BasicLayer(new ActivationTANH(), true, hidden1Size));
		network.addLayer(new BasicLayer(new ActivationTANH(), true, hidden2Size));
		network.addLayer(new BasicLayer(new ActivationTANH(), false, outputSize));

		network.getStructure().finalizeStructure();
		network.reset();
	}

	// Carga los pesos del genoma en la red
	private void loadGenomeIntoNetwork() {
		network.decodeFromArray(genome);
	}

	// Hace una propagación hacia delante
	private double[] forward(double[] input) {
		MLData in = new BasicMLData(input);
		MLData out = network.compute(in);

		double[] result = new double[outputSize];

		for (int i = 0; i < outputSize; i++) {
			result[i] = out.getData(i);
		}

		return result;
	}

	// Construye el vector de entrada para la red
	private double[] buildInputVector(StateObservation stateObs) {
		double[] input = new double[inputSize];
		int idx = 0;

		int ax = map.getAvatarX();
		int ay = map.getAvatarY();

		int currentFoodDistance = map.distanceToNearestFood(ax, ay);
		int currentEnemyDistance = map.distanceToNearestEnemy(ax, ay);

		DirectionData[] dirs = new DirectionData[] { analyzeDirection(ax, ay, ACTIONS.ACTION_UP, stateObs),
				analyzeDirection(ax, ay, ACTIONS.ACTION_DOWN, stateObs),
				analyzeDirection(ax, ay, ACTIONS.ACTION_LEFT, stateObs),
				analyzeDirection(ax, ay, ACTIONS.ACTION_RIGHT, stateObs) };

		// Características de las 4 direcciones
		for (DirectionData d : dirs) {
			input[idx++] = d.legal ? 1.0 : -1.0;
			input[idx++] = d.blocked ? 1.0 : -1.0;
			input[idx++] = d.enemy ? 1.0 : -1.0;
			input[idx++] = d.food ? 1.0 : -1.0;
			input[idx++] = d.danger ? 1.0 : -1.0;
			input[idx++] = normalizeCount(d.freeNeighbors, 4);
			input[idx++] = -normalizeCount(d.nextVisitCount, 8);
			input[idx++] = distanceImprovement(currentFoodDistance, d.nextFoodDistance);
			input[idx++] = safetyImprovement(currentEnemyDistance, d.nextEnemyDistance);
		}

		// Datos globales del estado actual
		input[idx++] = normalize(ax, width);
		input[idx++] = normalize(ay, height);
		input[idx++] = normalizeDistance(currentFoodDistance);
		input[idx++] = normalizeDistance(currentEnemyDistance);
		input[idx++] = normalizeCount(map.countAdjacentEnemies(ax, ay), 4);
		input[idx++] = normalizeCount(map.countAdjacentDanger(ax, ay), 4);
		input[idx++] = normalizeCount(stateObs.getAvailableActions().size(), 5);
		input[idx++] = normalizeCount(getVisitedCellsCount(), width * height);
		input[idx++] = normalizeCount(ticksWithoutScoreImprovement, 50);
		input[idx++] = encodeLastAction(lastAction);
		input[idx++] = isCurrentCellRepeated(ax, ay) ? 1.0 : -1.0;
		input[idx++] = normalizeCount(map.countFreeNeighbors(ax, ay), 4);

		return input;
	}

	// Analiza cómo sería moverse en una dirección concreta
	private DirectionData analyzeDirection(int ax, int ay, ACTIONS action, StateObservation stateObs) {
		DirectionData d = new DirectionData();
		d.action = action;
		d.legal = stateObs.getAvailableActions().contains(action);

		int nx = ax;
		int ny = ay;

		switch (action) {
		case ACTION_UP:
			ny--;
			break;
		case ACTION_DOWN:
			ny++;
			break;
		case ACTION_LEFT:
			nx--;
			break;
		case ACTION_RIGHT:
			nx++;
			break;
		default:
			break;
		}

		d.blocked = map.isBlocked(nx, ny);
		d.enemy = map.hasEnemy(nx, ny);
		d.food = map.hasFood(nx, ny);
		d.danger = map.isDanger(nx, ny);
		d.freeNeighbors = d.blocked ? 0 : map.countFreeNeighbors(nx, ny);
		d.nextVisitCount = getVisitCount(nx, ny);
		d.nextFoodDistance = d.blocked ? -1 : map.distanceToNearestFood(nx, ny);
		d.nextEnemyDistance = d.blocked ? -1 : map.distanceToNearestEnemy(nx, ny);

		return d;
	}

	// Normaliza una coordenada entre -1 y 1
	private double normalize(int value, int max) {
		if (max <= 1) {
			return 0.0;
		}
		return ((double) value / (double) (max - 1)) * 2.0 - 1.0;
	}

	// Normaliza un contador entre -1 y 1
	private double normalizeCount(int value, int max) {
		int bounded = Math.max(0, Math.min(value, max));
		return ((double) bounded / (double) Math.max(1, max)) * 2.0 - 1.0;
	}

	// Normaliza una distancia entre -1 y 1
	private double normalizeDistance(int dist) {
		if (dist < 0) {
			return 1.0;
		}

		int cap = Math.max(width, height);
		int bounded = Math.min(dist, cap);

		return ((double) bounded / (double) Math.max(1, cap)) * 2.0 - 1.0;
	}

	// Mide si una acción mejora la distancia a la comida
	private double distanceImprovement(int currentDistance, int nextDistance) {
		if (currentDistance < 0 || nextDistance < 0) {
			return 0.0;
		}

		int diff = currentDistance - nextDistance;
		return clamp(diff / 5.0, -1.0, 1.0);
	}

	// Mide si una acción mejora la seguridad respecto a los enemigos
	private double safetyImprovement(int currentDistance, int nextDistance) {
		if (currentDistance < 0 || nextDistance < 0) {
			return 0.0;
		}

		int diff = nextDistance - currentDistance;
		return clamp(diff / 5.0, -1.0, 1.0);
	}

	// Codifica la última acción como un valor numérico
	private double encodeLastAction(ACTIONS action) {
		if (action == null) {
			return 0.0;
		}

		switch (action) {
		case ACTION_UP:
			return -0.50;
		case ACTION_DOWN:
			return -0.10;
		case ACTION_LEFT:
			return 0.25;
		case ACTION_RIGHT:
			return 0.60;
		case ACTION_NIL:
		default:
			return 1.0;
		}
	}

	// Convierte la salida de la red en una acción del juego
	private ACTIONS decodeAction(double[] output, StateObservation stateObs) {
		ACTIONS[] orderedActions = new ACTIONS[] { ACTIONS.ACTION_NIL, ACTIONS.ACTION_UP, ACTIONS.ACTION_DOWN,
				ACTIONS.ACTION_LEFT, ACTIONS.ACTION_RIGHT };

		int ax = map.getAvatarX();
		int ay = map.getAvatarY();

		int bestIndex = 0;
		double bestValue = -Double.MAX_VALUE;

		for (int i = 0; i < output.length; i++) {
			ACTIONS candidate = orderedActions[i];

			if (candidate != ACTIONS.ACTION_NIL && !stateObs.getAvailableActions().contains(candidate)) {
				continue;
			}

			double value = output[i] + actionBias(candidate, ax, ay);

			if (value > bestValue) {
				bestValue = value;
				bestIndex = i;
			}
		}

		return orderedActions[bestIndex];
	}

	// Pequeño sesgo heurístico para evitar acciones muy malas y romper empates
	private double actionBias(ACTIONS action, int ax, int ay) {
		if (action == ACTIONS.ACTION_NIL) {
			return -0.35;
		}

		int nx = ax;
		int ny = ay;

		switch (action) {
		case ACTION_UP:
			ny--;
			break;
		case ACTION_DOWN:
			ny++;
			break;
		case ACTION_LEFT:
			nx--;
			break;
		case ACTION_RIGHT:
			nx++;
			break;
		default:
			break;
		}

		if (map.isBlocked(nx, ny)) {
			return -2.0;
		}

		double bias = 0.0;

		if (map.hasFood(nx, ny)) {
			bias += 0.35;
		}

		if (map.hasEnemy(nx, ny)) {
			bias -= 1.20;
		}

		if (map.isDanger(nx, ny)) {
			bias -= 0.85;
		}

		int currentEnemyDistance = map.distanceToNearestEnemy(ax, ay);
		int nextEnemyDistance = map.distanceToNearestEnemy(nx, ny);
		if (currentEnemyDistance >= 0 && nextEnemyDistance >= 0) {
			bias += 0.08 * (nextEnemyDistance - currentEnemyDistance);
		}

		int currentFoodDistance = map.distanceToNearestFood(ax, ay);
		int nextFoodDistance = map.distanceToNearestFood(nx, ny);
		if (currentFoodDistance >= 0 && nextFoodDistance >= 0) {
			bias += 0.05 * (currentFoodDistance - nextFoodDistance);
		}

		bias += 0.04 * map.countFreeNeighbors(nx, ny);
		bias -= 0.08 * normalizeVisitPenalty(getVisitCount(nx, ny));

		return bias;
	}

	private double normalizeVisitPenalty(int visits) {
		return Math.min(visits, 10) / 10.0;
	}

	// Actualiza las estadísticas después de elegir una acción
	private void updateEpisodeStats(StateObservation stateObs, ACTIONS action) {
		int ax = map.getAvatarX();
		int ay = map.getAvatarY();

		if (action == ACTIONS.ACTION_NIL) {
			idleActions++;
		}

		if (lastAction != null && lastAction == action) {
			repeatedActions++;
		}

		int nx = ax;
		int ny = ay;

		switch (action) {
		case ACTION_UP:
			ny--;
			break;
		case ACTION_DOWN:
			ny++;
			break;
		case ACTION_LEFT:
			nx--;
			break;
		case ACTION_RIGHT:
			nx++;
			break;
		default:
			break;
		}

		if (action != ACTIONS.ACTION_NIL && map.isInside(nx, ny) && !map.isBlocked(nx, ny)) {
			int nextVisits = getVisitCount(nx, ny);

			if (nextVisits == 0) {
				explorationMoves++;
			} else if (nextVisits >= 2) {
				uselessBacktracks++;
			}

			if (map.countFreeNeighbors(nx, ny) <= 1) {
				trappedMoves++;
			}

			int currentEnemyDistance = map.distanceToNearestEnemy(ax, ay);
			int nextEnemyDistance = map.distanceToNearestEnemy(nx, ny);

			if (map.countAdjacentEnemies(ax, ay) > 0 || map.countAdjacentDanger(ax, ay) > 0) {
				if (currentEnemyDistance < 0 || nextEnemyDistance > currentEnemyDistance) {
					safeMoves++;
				} else {
					dangerousMoves++;
				}
			}
		}

		double score = stateObs.getGameScore();

		if (score > lastScore) {
			scoreImprovements++;
			ticksWithoutScoreImprovement = 0;
		} else {
			ticksWithoutScoreImprovement++;
		}

		lastScore = score;
		lastAction = action;
	}

	// Inicializa el genoma con valores aleatorios
	private void randomizeGenome() {
		for (int i = 0; i < genome.length; i++) {
			genome[i] = random.nextGaussian() * 0.20;
		}
	}

	public double[] getGenome() {
		return genome.clone();
	}

	public void setGenome(double[] newGenome) {
		if (newGenome == null || newGenome.length != network.encodedArrayLength()) {
			throw new IllegalArgumentException("Genoma inválido");
		}

		this.genome = newGenome.clone();
		loadGenomeIntoNetwork();
	}

	public int getGenomeLength() {
		return network.encodedArrayLength();
	}

	public int getSafeMoves() {
		return safeMoves;
	}

	public int getDangerousMoves() {
		return dangerousMoves;
	}

	public int getIdleActions() {
		return idleActions;
	}

	public int getRepeatedActions() {
		return repeatedActions;
	}

	public int getExplorationMoves() {
		return explorationMoves;
	}

	public int getTrappedMoves() {
		return trappedMoves;
	}

	public int getUselessBacktracks() {
		return uselessBacktracks;
	}

	public int getScoreImprovements() {
		return scoreImprovements;
	}

	public int getTicksWithoutScoreImprovement() {
		return ticksWithoutScoreImprovement;
	}

	public double getVisitedRatio() {
		return (double) getVisitedCellsCount() / (double) Math.max(1, width * height);
	}

	// Devuelve cuántas veces se ha visitado una casilla
	private int getVisitCount(int x, int y) {
		if (!map.isInside(x, y)) {
			return Integer.MAX_VALUE / 4;
		}
		return visitCounts[x][y];
	}

	// Cuenta cuántas casillas distintas ha visitado el avatar
	private int getVisitedCellsCount() {
		int count = 0;

		for (int x = 0; x < visitCounts.length; x++) {
			for (int y = 0; y < visitCounts[x].length; y++) {
				if (visitCounts[x][y] > 0) {
					count++;
				}
			}
		}

		return count;
	}

	// Comprueba si la casilla actual se está repitiendo demasiado
	private boolean isCurrentCellRepeated(int x, int y) {
		return getVisitCount(x, y) >= 3;
	}

	private double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}

	public Map getMap() {
		return map;
	}

	public BasicNetwork getNetwork() {
		return network;
	}

	public int getInputSize() {
		return inputSize;
	}

	public int getHidden1Size() {
		return hidden1Size;
	}

	public int getHidden2Size() {
		return hidden2Size;
	}

	public int getOutputSize() {
		return outputSize;
	}
}