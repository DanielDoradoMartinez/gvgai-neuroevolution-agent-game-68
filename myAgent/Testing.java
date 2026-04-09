package myAgent;

import java.util.Random;


import tools.Utils;
import tracks.ArcadeMachine;

/**
 * Created with IntelliJ IDEA. User: Diego Date: 04/10/13 Time: 16:29 This is a
 * Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public class Testing {

    public static void main(String[] args) {



		//Load available games
		String spGamesCollection =  "examples/all_games_sp.csv";
		String[][] games = Utils.readGames(spGamesCollection);

		//Game settings
		boolean visuals = true;
		int seed = new Random().nextInt();
	
		// Game and level to play
		int gameIdx =68;
		int levelIdx = 2; 
		String gameName = games[gameIdx][1];
		String game = games[gameIdx][0];
		String level1 = game.replace(gameName, gameName + "_lvl" + levelIdx);

		String recordActionsFile = null;

		// 2. This plays a game in a level by the controller.
	ArcadeMachine.runOneGame(game, level1, visuals, "myAgent.Agente", recordActionsFile, seed, 0);




    }
}
