package myAgent;

import core.game.Game;
import core.game.StateObservation;
import core.vgdl.VGDLFactory;
import core.vgdl.VGDLParser;
import core.vgdl.VGDLRegistry;

public class GameFactory {

    public static StateObservation createInitialState(String gameFile, String levelFile, int seed) {
        VGDLFactory.GetInstance().init();
        VGDLRegistry.GetInstance().init();

        Game game = new VGDLParser().parseGame(gameFile);
        game.buildLevel(levelFile, seed);

        return game.getObservation().copy();
    }
}