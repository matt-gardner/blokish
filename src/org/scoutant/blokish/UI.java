/*
 * Copyright (C) 2011- stephane coutant
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See
 * the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>
 */

package org.scoutant.blokish;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.scoutant.blokish.model.Move;
import org.scoutant.blokish.model.Piece;
import org.scoutant.blokish.model.Square;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

public class UI extends Activity {
    private static final int MENU_ITEM_HISTORY = 99;
    private static final int MENU_ITEM_REPLAY = 101;
    private static final int MENU_ITEM_BACK = 102;
    private static final int MENU_ITEM_NEW = 5;
    private static final int MENU_ITEM_THINK=10;
    private static final int MENU_ITEM_PREFERENCES=-1;
    private static final int MENU_ITEM_HELP = 9;
    private static final int MENU_ITEM_PASS_TURN = 12;
    private static final int MENU_ITEM_FLIP = 15;

    private static String tag = "BLOKISH-UI";
    public GameView game;
    public boolean devmode=false;
    private SharedPreferences prefs;
    private Vibrator vibrator;
    private Resources rs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rs = getResources();
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        newgame();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        source();
        AppRater.app_launched(this);
    }

    private void newgame() {
        game = new GameView(this);
        setContentView(game);
    }

    public void vibrate(int millis) {
        if (vibrator != null) {
            vibrator.vibrate(millis);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        menu.add(Menu.NONE, MENU_ITEM_BACK, Menu.NONE, R.string.undo).setIcon( R.drawable.left_48);
        menu.add(Menu.NONE, MENU_ITEM_NEW, Menu.NONE, R.string.new_game).setIcon( R.drawable.restart_48);

        menu.add(Menu.NONE, MENU_ITEM_HELP, Menu.NONE, R.string.help).setIcon( R.drawable.help_48);
        menu.add(Menu.NONE, MENU_ITEM_PREFERENCES, Menu.NONE, R.string.preferences).setIcon( R.drawable.preferences_48);

        // TODO(matt): fix to check for whether current player is AI.
        if (!prefs.getBoolean("ai", true)) {
            menu.add(Menu.NONE, MENU_ITEM_PASS_TURN, Menu.NONE, R.string.i_pass).setIcon( R.drawable.checkmark_48);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        if (item.getItemId() == MENU_ITEM_HELP) {
            startActivity( new Intent(this, Help.class));
        }
        else if (item.getItemId() == MENU_ITEM_PREFERENCES) {
            startActivity( new Intent(this, Settings.class));
        }
        else if (item.getItemId() == MENU_ITEM_BACK) {
            List<Move> moves = game.getMoves();
            int length = moves.size();
            if (length>=4) {
                length -= 4;
            }
            moves = moves.subList(0, length);
            newgame();
            Log.i(tag, "replay # moves : " + length);
            game.replay(moves);
        }
        else if (item.getItemId() == MENU_ITEM_NEW) {
            new AlertDialog.Builder(this)
                .setMessage( rs.getString( R.string.new_game) + "?")
                .setCancelable(false)
                .setPositiveButton( R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        newgame();
                    }
                })
            .setNegativeButton( R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            })
            .create()
                .show();
        }
        else if (item.getItemId() == MENU_ITEM_PASS_TURN) {
            game.endTurn();
        }
        return false;
    }

    private int findRequestedLevel() {
        String level = prefs.getString("aiLevel", "0");
        return Integer.valueOf(level);
    }

    public int turn = 0;

    public void displayWinnerDialog() {
        game.indicator.hide();
        Log.d(tag, "game over !");
        int winner = game.getWinner();
        int score = game.getScore(winner);
        String message = "";
        boolean redWins = (winner==0 && prefs.getBoolean("ai", true));
        if (redWins) {
            message += rs.getString( R.string.congratulations) + " " + score +".";
            if (findRequestedLevel()<(4-1)) message += "\n" + rs.getString( R.string.try_next);
        } else {
            // message += "Player " + game.game.colors[winner] + " wins with score : " + score;
            message += rs.getString(game.getPlayerNameId(winner));
            message += " " + rs.getString( R.string.wins_with_score) + " : ";
            message += score;
        }
        new EndGameDialog(UI.this, redWins, message, findRequestedLevel()+1, score).show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ( keyCode == KeyEvent.KEYCODE_SEARCH) {
            devmode = !devmode;
            return true;
        }

        if ( keyCode == KeyEvent.KEYCODE_BACK) {
            if (!prefs.getBoolean("popupOnExit", true)) {
                UI.this.finish();
            } else {
                new AlertDialog.Builder(this)
                    .setMessage( R.string.quit)
                    .setCancelable(false)
                    .setPositiveButton( R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            UI.this.finish();
                        }
                    })
                .setNegativeButton( R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                })
                .create()
                    .show();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void save(){
        FileOutputStream fos;
        try {
            fos = openFileOutput("moves.txt", Context.MODE_PRIVATE);
            if (fos==null) return;
            if (!game.isOver()) {
                fos.write( game.getLog().getBytes());
            } // if gave is over we do not save it, so as to open a blank game next time
            fos.close();
        } catch (FileNotFoundException e) {
            Log.e(tag, "not found...", e);
        } catch (IOException e) {
            Log.e(tag, "io...", e);
        }
    }
    /** sources a list of representions like this sample : 18|16|2|I3|0,-1|0,0|0,1 */
    private void source() {
        List<Move> list = new ArrayList<Move>();
        try {
            FileInputStream fis = openFileInput("moves.txt");
            BufferedReader reader = new BufferedReader( new InputStreamReader(fis));
            String line;
            reader.readLine(); // first line give the # of moves...
            while ((line = reader.readLine()) != null)   {
                String[] data = line.split(":");
                int i = Integer.valueOf(data[0]);
                int j = Integer.valueOf(data[1]);
                int color = Integer.valueOf(data[2]);
                Piece piece = game.getBoard(color).findPieceByType(data[3] );
                piece.reset();
                for (int q = 4; q<data.length; q++) {
                    String[] position = data[q].split(",");
                    int x = Integer.valueOf( position[0]);
                    int y = Integer.valueOf( position[1]);
                    piece.add( new Square(x, y ));
                }
                Move move = new Move(piece, i, j);
                // Log.d(tag, "created move : " + move);
                list.add(move);
            }
            newgame();
            game.replay( list);
        } catch (Exception e) {
            Log.e(tag, "yep error is :", e);
        }
    }

    @Override
    protected void onStop() {
        /* TODO(matt): should I worry about putting this back in?
        if (task!=null) {
            task.cancel(true);
            Log.d(tag, "leaving AI, as activity is brough to background");
        }
        */
        save();
        super.onStop();
    }
}
