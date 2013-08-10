package org.scoutant.blokish.model;

public class HumanPlayer implements Player {
    private int playerNum;

    public HumanPlayer(int playerNum) {
        this.playerNum = playerNum;
    }

    @Override
    public void takeTurn(MoveCallback callback) {
        // Just do nothing here, because that means we'll wait on an action from the UI.
    }

    @Override
    public int getPlayerNum() {
        return playerNum;
    }
}
