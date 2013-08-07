package org.scoutant.blokish.model;

// TODO(matt): remove this ugly dependency
import org.scoutant.blokish.UI;

public class AiPlayer implements Player {
    private int playerNum;
    private UI ui;

    public AiPlayer(int playerNum, UI ui) {
        this.playerNum = playerNum;
        this.ui = ui;
    }

    public void takeTurn() {
        ui.new AITask().execute(playerNum);
    }

    @Override
    public int getPlayerNum() {
        return playerNum;
    }
}
