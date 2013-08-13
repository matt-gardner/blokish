/*
* Copyright (C) 2011- stephane coutant
*
* This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
* without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
* See the GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>
*/

package org.scoutant.blokish.model;

import java.util.ArrayList;
import java.util.List;

import org.scoutant.blokish.R;
import org.scoutant.blokish.GameView;

import android.util.Log;

public class Game {
    // We need this so the model can notify the view that things have changed.
    private GameView view;
	public static final String tag = "BLOKISH-Game";
	public List<Board> boards = new ArrayList<Board>();
	public int size = 20;
//	public String[] colors = { "Red", "Green", "Blue", "Orange" };
	public int[] colors = { R.string.Red, R.string.Green, R.string.Blue, R.string.Orange };
        private int currentPlayer;
        private Player[] players;

	public Game(GameView view) {
            this.view = view;
                players = new Player[4];
		reset();
	}
	public void reset() {
		boards.clear();
		for(int k=0; k<4; k++) {
			boards.add(new Board(k));
		}
                currentPlayer = 0;
	}

        public void setPlayer(Player player, int playerNum) {
            players[playerNum] = player;
        }

        public Player getPlayer(int playerNum) {
            return players[playerNum];
        }

        public int getCurrentPlayer() {
            return currentPlayer;
        }
	
	public List<Move> moves = new ArrayList<Move>();
	public void historize(Move move) {
		moves.add(move);
	}
	
	/** @return true if game is over */ 
	public boolean over() {
            for (Board board : boards) {
                if (!board.over) {
                    return false;
                }
            }
            return true;
	}
	
	// TODO(pre-matt): adapt message when equal score?
	/**
	 * on equal score : winner is the last to play.
	 * 
	 */
	public int winner() {
		int highscore = 0;
		for (int p=0; p<4; p++) highscore = Math.max(highscore, boards.get(p).score);
		for (int p=3; p>=0; p--) {
			if (boards.get(p).score == highscore) return p;
		}
		return -1;
	}
	
	// to be called onto a fresh Game...
	public boolean replay(List<Move> moves) {
		for (Move move : moves) {
			Piece piece = move.piece;
			int color = piece.color; 
			Piece p = boards.get(color).findPieceByType( piece.type);
			p.reset(piece);
			move.piece = p;
			boolean status = play(move);
			if (status==false) return false;
		}
		return true;
	}
	
	protected void add( Piece piece, int i, int j) {
		for(int k=0; k<4; k++) {
			boards.get(k).add(piece, i, j);
		}
	}
	public boolean valid( Move move) {
		return valid( move.piece, move.i, move.j);
	}
	public boolean valid( Piece piece, int i, int j) {
		return fits(piece, i, j)
                    && boards.get(piece.color).onseed(piece, i, j)
                    && piece.color == currentPlayer;
	}
	
	public boolean fits( Piece p, int i, int j) {
		return boards.get(0).fits(0,p, i, j) && boards.get(1).fits(1,p, i, j) && boards.get(2).fits(2,p, i, j) && boards.get(3).fits(3,p, i, j);
	}
	
	public boolean play(Move move) {
		if ( ! valid(move)) { 
			Log.e(tag, "not valid! " + move);
			Log.e(tag, "not valid! " + move.piece);
			return false;
		}
		add(move.piece, move.i, move.j);
		Log.d(tag, "played move : " + move);
		historize(move);
                view.notifyMovePlayed(move);
		return true;
	}

        public void endTurn() {
            currentPlayer++;
            currentPlayer %= 4;
            if (over()) {
                view.notifyGameOver();
                return;
            }
            Log.d(tag, "starting next turn");
            if (!hasMove(currentPlayer)) {
                view.notifyHasNoMove(currentPlayer);
                endTurn();
            } else {
                view.notifyStartingTurn(currentPlayer);
                // TODO(matt): human players totally ignore this callback.  Maybe it's worth trying
                // to modify the way the UI works to make this more consistent?
                players[currentPlayer].takeTurn(new MoveCallback() {
                    @Override
                    public void call(Move move) {
                        // TODO(matt): error checking here?
                        play(move);
                        endTurn();
                    }
                });
            }
        }

    public boolean hasMove(int color) {
        // First check to see if we already know the player has no more moves.
        if (boards.get(color).over) {
            return false;
        }
        // Check for an available move, breaking early if we find one.
        Board board = boards.get(color);
        for (Square seed : board.seeds()) {
            for (int p=0; p<board.pieces.size(); p++) {
                Piece piece = board.pieces.get(p);
                // Fixing issue #3, changing order rotate/flip
                for( int f=0; f<piece.flips; f++, piece.flip()) {
                    for (int r=0; r<piece.rotations; r++, piece.rotate(1)) {
                        for (Square s : piece.squares()) {
                            int i = seed.i - s.i;
                            int j = seed.j - s.j;
                            if ( !board.outside(s, i, j) && fits(piece, i, j)) {
                                Log.d(tag, "possible move : " + new Move(piece, i, j));
                                return true;
                            }
                        }
                    }
                }
            }
        }
        // We didn't find an available move, so the player is done.
        boards.get(color).over = true;
        return false;
    }

        public String toString() {
            String msg = "# moves : " + moves.size();
            for (Move move: moves) {
                msg += "\n" + Move.serialize(move);
            }
            return msg;
        }

	public List<Move> deserialize(String msg) {
		List<Move> list = new ArrayList<Move>();
		return list;
	}
	
	
	int[][] ab = new int [20][20];
	/**
	 * @return # of seeds if actually adding enemy @param piece at @param i, @param j on board @param board.
	 */
	private int scoreEnemySeedsIfAdding(Board board, Piece piece, int i, int j) {
		// how many of the board's seeds happen to be under piece?
		int result=0;
		for (int b=0; b<20; b++) for (int a=0; a<20; a++) ab[a][b] = 0;
		for(Square s : board.seeds()) {
			try { ab[s.i][s.j] = 1; } catch (Exception e) {}
		}
		for(Square s : piece.squares()) {
			try { ab[i+s.i][j+s.j] = 0; } catch (Exception e) {}
		}
		for (int b=0; b<20; b++) for (int a=0; a<20; a++) if (ab[a][b]==1) result++;
//		Log.d(tag, "scoreEnemySeedsIfAdding : " + result + ". color : " + board.color);
		return result;
	}
	
	public int scoreEnemySeedsIfAdding(int color, Piece piece, int i, int j) {
		int result =0;
//		for (int c=0; c<4; c++) {
//			if (c!=color) {
//				result += scoreEnemySeedsIfAdding( boards.get(c), piece, i, j );
//			}
//		}
		// try consider only Red as enemy, for machine to compete with human!
		result += scoreEnemySeedsIfAdding( boards.get(0), piece, i, j );
		return result;
	}
	
}
