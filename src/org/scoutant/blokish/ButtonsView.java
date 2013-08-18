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

import org.scoutant.blokish.model.Move;

import android.content.Context;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView.ScaleType;

public class ButtonsView extends FrameLayout {

    protected static final String tag = "BLOKISH-ui";

    private Context context;
    private ImageButton cancel;
    private ImageButton ok;

    private GameView gameView;

    public ButtonsView(Context context) {
        super(context);
        this.context = context;
        setVisibility(INVISIBLE);
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int h = display.getHeight() - display.getWidth();
        setLayoutParams( new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT, h, Gravity.BOTTOM));
        cancel = button(R.drawable.cancel, doCancel, 0);
        addView(cancel );
        ok = button(R.drawable.checkmark, doOk, 1);
        addView(ok);
        setOkState( false);
    }


    private ImageButton button(int src, OnClickListener l, int position) {
        ImageButton btn = new ImageButton(context);
        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL);
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int margin = Math.min( (display.getWidth() - 3*128)/3, 80);
        params.leftMargin = margin;
        params.rightMargin = margin;
        if (position==0) params.gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
        if (position==1) params.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
        btn.setLayoutParams(params);
        btn.setImageDrawable(context.getResources().getDrawable( src));
        btn.setScaleType(ScaleType.CENTER_INSIDE);
        btn.setBackgroundColor(Color.TRANSPARENT);
        btn.setOnClickListener(l);
        return btn;

    }

    protected void setState( ImageButton btn, boolean state) {
        btn.setEnabled( state);
        btn.setAlpha( state ? 200 : 50 );
    }

    public void setOkState(boolean state) {
        setState(ok, state);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        gameView = (GameView) getParent();
    }

    // TODO(matt): Why is this here?  What does it do?  Oh, it's the green check box?  Is that
    // right?  Can we move the functionality somewhere more sane?  Like, maybe to the HumanPlayer
    // code?  Ne vem...
    private OnClickListener doOk = new OnClickListener() {
        public void onClick(View v) {
            Log.d(tag, "ok...");
            PieceUI piece = gameView.getSelectedPiece();
            if (piece==null) {
                Log.e(tag, "cannot retrieve piece!");
                return;
            }
            Move move = new Move(piece.piece, piece.i, piece.j);
            boolean possible = gameView.isValid(move);
            int player = move.piece.color;
            if (possible) {
                // TODO(pre-matt): refactor with place()
                piece.movable=false;
                piece.setLongClickable(false);
                piece.setClickable(false);
                gameView.lasts[player] = piece;
                piece.invalidate();
                setVisibility(INVISIBLE);
                gameView.play(move);
                ((GameView)getParent()).tabs[player].setText("" + gameView.getScore(player));
                gameView.setSelectedPiece(null);
                gameView.endTurn();
            }
        }
    };
    private OnClickListener doCancel = new OnClickListener() {
        public void onClick(View v) {
            Log.d(tag, "cancel...");
            gameView.getSelectedPiece().replace();
            gameView.setSelectedPiece(null);
            ButtonsView.this.setVisibility(INVISIBLE);
        }
    };

}
