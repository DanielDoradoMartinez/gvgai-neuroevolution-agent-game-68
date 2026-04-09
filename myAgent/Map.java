package myAgent;

import java.util.ArrayList;

import core.game.Observation;
import core.game.StateObservation;
import tools.Vector2d;

public class Map {

	// Símbolos para representar el contenido de cada casilla del mapa
	public static final String VACIO = ".";
	public static final String AVATAR = "A";
	public static final String MURO = "#";
	public static final String ENEMIGO = "E";
	public static final String COMIDA = "C";
	public static final String PELIGRO = "P";
	public static final String OTRO = "?";

	// Tipos de objetos que vamos a considerar como muros, comida, enemigos y
	// peligro

	private static final int[] WALL_TYPES = { 0 };
	private static final int[] FOOD_TYPES = { 4, 5, 6 };
	private static final int[] ENEMY_TYPES = { 9, 10, 11, 12, 13, 15, 18, 21, 24, 27 };
	private static final int[] DANGER_TYPES = { 7, 8, 14, 16, 17, 19, 20, 22, 23, 25, 26 };

	private final int width;
	private final int height;
	private final int blockSize;

	// mapa
	private String[][] board;

	// Posición actual del avatar
	private int avatarX;
	private int avatarY;

	// Listas auxiliares
	private ArrayList<int[]> walls;
	private ArrayList<int[]> food;
	private ArrayList<int[]> enemies;
	private ArrayList<int[]> dangers;
	private ArrayList<int[]> others;

	public Map(int width, int height, StateObservation stateObs) {
		this.width = width;
		this.height = height;
		this.blockSize = stateObs.getBlockSize();

		this.board = new String[width][height];

		this.walls = new ArrayList<>();
		this.food = new ArrayList<>();
		this.enemies = new ArrayList<>();
		this.dangers = new ArrayList<>();
		this.others = new ArrayList<>();

		update(stateObs);
	}

	// Actualiza toda la información del mapa a partir del estado actual del juego
	public void update(StateObservation stateObs) {
		clearBoard();
		clearLists();

		loadFromObservationGrid(stateObs.getObservationGrid());
		setAvatarPosition(stateObs.getAvatarPosition());
		rebuildDangerField();
	}

	// Rellena todo el tablero con casillas vacías
	private void clearBoard() {
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				board[x][y] = VACIO;
			}
		}
	}

	// Vacía las listas de objetos
	private void clearLists() {
		walls.clear();
		food.clear();
		enemies.clear();
		dangers.clear();
		others.clear();
	}

	// Recorre la observationGrid del framework y añade cada observación al mapa
	// interno
	private void loadFromObservationGrid(ArrayList<Observation>[][] grid) {
		if (grid == null) {
			return;
		}

		for (int x = 0; x < grid.length; x++) {
			for (int y = 0; y < grid[x].length; y++) {
				ArrayList<Observation> cell = grid[x][y];

				if (cell == null || cell.isEmpty()) {
					continue;
				}

				for (Observation obs : cell) {
					addObservation(obs, x, y);
				}
			}
		}
	}

	// Clasifica una observación según su tipo y la guarda en la estructura
	// correspondiente
	private void addObservation(Observation obs, int x, int y) {
		if (!isInside(x, y)) {
			return;
		}

		if (isType(obs.itype, WALL_TYPES)) {
			walls.add(new int[] { x, y, obs.itype, obs.category });
			board[x][y] = MURO;
			return;
		}

		if (isType(obs.itype, ENEMY_TYPES)) {
			enemies.add(new int[] { x, y, obs.itype, obs.category });
			board[x][y] = ENEMIGO;
			return;
		}

		if (isType(obs.itype, FOOD_TYPES)) {
			food.add(new int[] { x, y, obs.itype, obs.category });

			// Si en la misma casilla hay enemigo y comida, dejamos enemigo
			if (!ENEMIGO.equals(board[x][y])) {
				board[x][y] = COMIDA;
			}
			return;
		}

		if (isType(obs.itype, DANGER_TYPES)) {
			dangers.add(new int[] { x, y, obs.itype, obs.category });

			if (VACIO.equals(board[x][y])) {
				board[x][y] = PELIGRO;
			}
			return;
		}

		// En este caso ignoramos el tipo 2, como en tu versión original
		if (obs.itype != 2) {
			others.add(new int[] { x, y, obs.itype, obs.category });

			if (VACIO.equals(board[x][y])) {
				board[x][y] = OTRO;
			}
		}
	}

	// Marca como peligrosas las casillas alrededor de cada enemigo

	private void rebuildDangerField() {
		ArrayList<int[]> inferredDangers = new ArrayList<>();

		for (int[] enemy : enemies) {
			int ex = enemy[0];
			int ey = enemy[1];

			int[][] deltas = { { 0, 0 }, { 0, -1 }, { 0, 1 }, { -1, 0 }, { 1, 0 } };

			for (int[] d : deltas) {
				int nx = ex + d[0];
				int ny = ey + d[1];

				if (!isInside(nx, ny) || isBlocked(nx, ny)) {
					continue;
				}

				if (ENEMIGO.equals(board[nx][ny])) {
					continue;
				}

				board[nx][ny] = PELIGRO;
				inferredDangers.add(new int[] { nx, ny, -1, -1 });
			}
		}

		dangers.addAll(inferredDangers);
	}

	// Coloca al avatar en la posición actual dentro del tablero
	private void setAvatarPosition(Vector2d pos) {
		if (pos == null) {
			return;
		}

		int x = toGridX(pos.x);
		int y = toGridY(pos.y);

		if (!isInside(x, y)) {
			return;
		}

		avatarX = x;
		avatarY = y;
		board[x][y] = AVATAR;
	}

	// Convierte coordenadas reales del juego a coordenadas de la rejilla
	private int toGridX(double x) {
		return (int) (x / blockSize);
	}

	private int toGridY(double y) {
		return (int) (y / blockSize);
	}

	// Comprueba si un tipo pertenece a un conjunto de tipos
	private boolean isType(int value, int[] allowedValues) {
		for (int v : allowedValues) {
			if (v == value) {
				return true;
			}
		}
		return false;
	}

	// Comprueba si una posición está dentro del mapa
	public boolean isInside(int x, int y) {
		return x >= 0 && x < width && y >= 0 && y < height;
	}

	// Una casilla se considera bloqueada si está fuera del mapa o si es un muro
	public boolean isBlocked(int x, int y) {
		if (!isInside(x, y)) {
			return true;
		}

		return MURO.equals(board[x][y]);
	}

	public boolean hasEnemy(int x, int y) {
		return isInside(x, y) && ENEMIGO.equals(board[x][y]);
	}

	public boolean hasFood(int x, int y) {
		return isInside(x, y) && COMIDA.equals(board[x][y]);
	}

	public boolean isDanger(int x, int y) {
		return isInside(x, y) && PELIGRO.equals(board[x][y]);
	}

	// Cuenta enemigos en las cuatro casillas vecinas
	public int countAdjacentEnemies(int x, int y) {
		int count = 0;

		if (hasEnemy(x, y - 1))
			count++;
		if (hasEnemy(x, y + 1))
			count++;
		if (hasEnemy(x - 1, y))
			count++;
		if (hasEnemy(x + 1, y))
			count++;

		return count;
	}

	// Cuenta peligros en las cuatro casillas vecinas
	public int countAdjacentDanger(int x, int y) {
		int count = 0;

		if (isDanger(x, y - 1))
			count++;
		if (isDanger(x, y + 1))
			count++;
		if (isDanger(x - 1, y))
			count++;
		if (isDanger(x + 1, y))
			count++;

		return count;
	}

	// Cuenta cuántas casillas vecinas son transitables
	public int countFreeNeighbors(int x, int y) {
		if (!isInside(x, y) || isBlocked(x, y)) {
			return 0;
		}

		int count = 0;

		if (isInside(x, y - 1) && !isBlocked(x, y - 1))
			count++;
		if (isInside(x, y + 1) && !isBlocked(x, y + 1))
			count++;
		if (isInside(x - 1, y) && !isBlocked(x - 1, y))
			count++;
		if (isInside(x + 1, y) && !isBlocked(x + 1, y))
			count++;

		return count;
	}

	public int distanceToNearestFood(int x, int y) {
		return distanceToNearest(food, x, y);
	}

	public int distanceToNearestEnemy(int x, int y) {
		return distanceToNearest(enemies, x, y);
	}

	// Calcula la distancia Manhattan hasta el objeto más cercano de una lista
	private int distanceToNearest(ArrayList<int[]> positions, int x, int y) {
		if (!isInside(x, y) || positions.isEmpty()) {
			return -1;
		}

		int best = Integer.MAX_VALUE;

		for (int[] p : positions) {
			int dist = Math.abs(p[0] - x) + Math.abs(p[1] - y);
			if (dist < best) {
				best = dist;
			}
		}

		return best == Integer.MAX_VALUE ? -1 : best;
	}

	public int getAvatarX() {
		return avatarX;
	}

	public int getAvatarY() {
		return avatarY;
	}

	public ArrayList<int[]> getWalls() {
		return walls;
	}

	public ArrayList<int[]> getFood() {
		return food;
	}

	public ArrayList<int[]> getEnemies() {
		return enemies;
	}

	public ArrayList<int[]> getDangers() {
		return dangers;
	}

	public ArrayList<int[]> getOthers() {
		return others;
	}

	public String getCell(int x, int y) {
		if (!isInside(x, y)) {
			return null;
		}

		return board[x][y];
	}
}