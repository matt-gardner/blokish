package org.scoutant.blokish.model;

public interface Player {
    public void takeTurn(MoveCallback callback);
    public int getPlayerNum();
}
