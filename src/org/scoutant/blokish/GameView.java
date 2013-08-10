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

package org.scoutant.blokish;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.scoutant.blokish.model.AI;
import org.scoutant.blokish.model.AiPlayer;
import org.scoutant.blokish.model.Board;
import org.scoutant.blokish.model.Game;
import org.scoutant.blokish.model.HumanPlayer;
import org.scoutant.blokish.model.Move;
import org.scoutant.blokish.model.Piece;
import org.scoutant.blokish.model.Player;
import org.scoutant.blokish.model.Square;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * For DnD approach, refer to http://blog.scoutant.org/index.php?post/2011/02/Approche-naturelle-de-Drag-and-Drop-en-Android
 */
public class GameView extends FrameLayout {
    private static String tag = "BLOKISH-GameView";
    private Paint paint = new Paint();
    // TODO(matt): make all of these private
    public int size;
    public ButtonsView buttons;
    public PieceUI selected;
    public int selectedColor;
    public int swipe=0;
    public int gone=0;

    public Game game;
    public static int[] icons = { R.drawable.bol_rood, R.drawable.bol_groen, R.drawable.bol_blauw, R.drawable.bullet_ball_glass_yellow};
    public static int[] labels = { R.id.red, R.id.green, R.id.blue, R.id.orange};

    private Drawable[] dots = new Drawable[4];
    public TextView[] tabs = new TextView[4];

    public UI ui;
    public SharedPreferences prefs;
    public boolean thinking=false;
    public boolean singleline=false;
    public BusyIndicator indicator;
    public PieceUI lasts[] = new PieceUI[4];

    public GameView(Context context) {
        super(context);
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        game = new Game(this);
        ui = (UI) context;
        int level = findLevel();
        for (int i = 0; i<4; i++) {
            if (isAi(i)) {
                game.setPlayerNum(new AiPlayer(i, level, game), i);
            } else {
                game.setPlayerNum(new HumanPlayer(i), i);
            }
        }
        setWillNotDraw(false);
        setLayoutParams( new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, Gravity.TOP));
        paint.setStrokeWidth(1.3f);
        paint.setColor(Color.WHITE);
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        size = display.getWidth()/20;
        Log.d(tag, "size " + size);
        if ( display.getHeight()/32 < display.getWidth()/20) singleline = true;
        for (Board board : game.boards) {
            int i=2;
            for (Piece piece : board.pieces) {
                addView( new PieceUI(context, piece, i, 20+2) );
                i += 4;
            }
            reorderPieces(board.color);
        }
        buttons = new ButtonsView(context);
        addView( buttons);

        LayoutInflater inflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.tabs, this);

        showPieces(0);
        for (int color=0; color<4; color++) {
            dots[color] = context.getResources().getDrawable(icons[color]);
            dots[color].setAlpha(191);
            tabs[color] = (TextView) findViewById( labels[color]);
            // let put the listener on the parent view group
            ViewGroup tab  =  (ViewGroup) tabs[color].getParent();
            if (tab!=null) tab.setOnClickListener(new ShowPiecesListener(color));
        }

        // progress indicator
        View iView = new View(context);
        iView.setLayoutParams(new FrameLayout.LayoutParams(150, 150, Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL));
        addView(iView);
        indicator = new BusyIndicator(context, iView);
    }

    public void notifyHasNoMove(int player) {
        indicator.hide();
        Log.d(tag, player + " over!");
        new AlertDialog.Builder(ui)
            // TODO(matt): fix this string, probably only show it once.
            .setMessage( R.string.red_ko)
            .setCancelable(false)
            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    game.boards.get(0).over = true;
                    Log.d(tag, "ok!");
                }
            })
        .create()
            .show();
    }

    private int findLevel() {
        String level = prefs.getString("aiLevel", "0");
        int l = Integer.parseInt(level);
        if (l<0 || l>3) l = 1;
        return l;
    }

    private class ShowPiecesListener implements OnClickListener {
        private int color;
        protected ShowPiecesListener(int color) {
            this.color = color;
        }
        public void onClick(View v) {
            GameView.this.showPieces(color);
            GameView.this.invalidate();
        }
    }

    public float downX;
    public float downY;

    @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (selected!=null) return false;
            doTouch(event);
            return true;
        }

    public int getCurrentPlayer() {
        return game.getCurrentPlayer();
    }

    public boolean isAi(int playerNum) {
        String aiSetting = "ai" + playerNum;
        return prefs.getBoolean(aiSetting, true);
    }

    /**
     * Tell the game that a human player has ended his/her turn.  AI players have a separate
     * mechanism for telling the game that their turn is ended, and this logic could also
     * reasonably be placed somewhere else.
     */
    public void endTurn() {
        int player = getCurrentPlayer();
        reorderPieces(player);
        Log.d(tag, "ending human turn");
        // This either starts a background task or passes, depending on whether the player is
        // an AI or not.  So we can do some extra logic here without worrying about it.
        game.endTurn();
    }

    /**
     * Notify the game view that a new player is starting a turn.  Note that this only gets called
     * when the player has a valid move.
     */
    public void notifyStartingTurn(int player) {
        showPieces(player);
        if (isAi(player)) {
            Log.d(tag, "player is AI");
            thinking=true;
            indicator.show();
        } else {
            ui.vibrate(15);
            Log.d(tag, "player is human");
            thinking = false;
            indicator.hide();
        }
    }

    public void doTouch(MotionEvent event) {
        int action = event.getAction();
        if (action==MotionEvent.ACTION_DOWN) {
            downX=event.getRawX();
            downY=event.getRawY();
        }
        if (action==MotionEvent.ACTION_MOVE ) {
            swipePieces( selectedColor, - (swipe + Float.valueOf( event.getRawX()-downX).intValue()));
        }
        if (action==MotionEvent.ACTION_UP ) {
            swipe += Float.valueOf( event.getRawX()-downX).intValue();
            downX=0;
            swipePieces( selectedColor, -swipe);
        }
    }

    @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            for (int i = 0; i < 20; i++) {
                canvas.drawLine(i*size, 0, i*size, 20*size, paint);
            }
            canvas.drawLine(20*size-1, 0, 20*size-1, 20*size, paint);
            for (int j = 0; j <= 20; j++) {
                canvas.drawLine(0, j*size+1, 20*size, j*size+1, paint);
            }
            if (prefs.getBoolean("displaySeeds", true)) {
                for (Square s : game.boards.get(selectedColor).seeds()) {
                    dots[selectedColor].setBounds( s.i*size+size/4, s.j*size+size/4, s.i*size+3*size/4, s.j*size+3*size/4);
                    dots[selectedColor].draw(canvas);
                }
            }
        }

    public PieceUI findPiece(int color, String type) {
        PieceUI found=null;
        for (int i=0; i<getChildCount(); i++) {
            View v = getChildAt(i);
            if (v instanceof PieceUI) {
                found = (PieceUI) v;
                if (found.piece.color == color && found.piece.type == type) return found;
            }
        }
        return null;
    }

    public PieceUI findPiece(Piece piece) {
        return findPiece(piece.color, piece.type);
    }

    public void notifyMovePlayed(Move move) {
        if (move==null) return;
        int player = move.piece.color;
        boolean animate = isAi(player);
        PieceUI ui = findPiece( move.piece);
        lasts[ui.piece.color] = ui;
        ui.place(move.i, move.j, animate);
        tabs[move.piece.color].setText( ""+game.boards.get(move.piece.color).score);
        invalidate();
    }

    public void notifyGameOver() {
        ui.displayWinnerDialog();
    }

    public void showPieces(int color){
        selectedColor = color;
        for (PieceUI piece : piecesInStore()) piece.setVisibility( piece.piece.color == color ? VISIBLE : INVISIBLE);
    }

    public void swipePieces( int color, int x) {
        for (PieceUI piece : piecesInStore(color)) piece.swipe(x);
    }


    public void reorderPieces() {
        for (int i=0; i<4; i++)
            reorderPieces(i);
    }

    public void reorderPieces( int color) {
        Log.d(tag, "reordering pieces for player " + color);
        List<PieceUI> pieces = piecesInStore(color);
        Collections.sort(pieces);
        Collections.reverse(pieces);
        for (int p=0; p<pieces.size(); p++) {
            PieceUI piece = pieces.get(p);
            if (singleline) {
                piece.j0 = 22;
                if (p<1) {
                    if (piece.piece.type.equals("I5")) piece.i0 = 1;
                    else piece.i0 = 2;
                } else {
                    piece.i0 = pieces.get(p-1).i0 + pieces.get(p-1).piece.size+1;
                }
            } else {
                piece.j0 = 22 + ((p%2) > 0 ? 5 : 0 ) ;
                if (p<2) {
                    if (piece.piece.type.equals("I5")) piece.i0 = 1;
                    else piece.i0 = 2;
                } else {
                    piece.i0 = pieces.get(p-2).i0 + pieces.get(p-2).piece.size+1;
                }
            }
            piece.replace();
        }
        swipe = 0;
        swipePieces(color, 0);
    }

    private List<PieceUI> piecesInStore(){
        List<PieceUI> list = new ArrayList<PieceUI>();
        for (int k=0; k<this.getChildCount(); k++) {
            if (this.getChildAt(k) instanceof PieceUI) {
                PieceUI piece = (PieceUI) this.getChildAt(k);
                if (piece!=null && piece.movable) {
                    list.add(piece);
                }
            }
        }
        return list;
    }
    private List<PieceUI> piecesInStore(int color){
        List<PieceUI> list = new ArrayList<PieceUI>();
        for (PieceUI piece : piecesInStore()) {
            if (piece.piece.color == color) list.add(piece);
        }
        return list;
    }

    public boolean replay(List<Move> moves) {
        for (Move move : moves) {
            Piece piece = move.piece;
            PieceUI ui = findPiece(piece);
            ui.piece.reset(piece);
            game.play(move);
        }
        return true;
    }
}
