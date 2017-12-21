package mato.checkers;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import mato.checkers.game.CheckersGame;
import mato.checkers.game.Board;
import mato.checkers.game.Piece;
import mato.checkers.game.Position;
import mato.checkers.game.Move;

public class MyCheckersActivity extends AppCompatActivity {
    private CheckersGame gamelogic;
    private CheckersLayout checkersView;
    public TextView statusText;

    private String prefDifficulty;
    private boolean prefAllowAnyMove;

    private static final String DIFFICULTY = "pref_difficulty";
    private static final String ANY_MOVE = "pref_any_move";


    @Override
    protected void onCreate(Bundle saved)
    {
        super.onCreate(saved);
        createGameBoard();
        // portrait only
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        //
        ////PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        //

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferencesChangeListener);
        prefDifficulty = sharedPreferences.getString(DIFFICULTY, null);
        prefAllowAnyMove = sharedPreferences.getBoolean(ANY_MOVE, false);

    }

    @Override
    protected void onResume() {
        super.onResume();
        prepTurn();
    }

    // show menu with settings icon
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    // display SettingsActivity
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.settings_menu_icon:
                Intent preferencesIntent = new Intent(this, SettingsActivity.class);
                startActivity(preferencesIntent);
                break;
            case R.id.new_menu_icon:
                Toast.makeText(this,
                        "New Game",
                        Toast.LENGTH_SHORT).show();
                restart();
                break;
            case R.id.about_menu_icon:
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setMessage("Checker by Mato\n Projekt na predmet TAMZ2.")
                        .setCancelable(false)
                        .setPositiveButton("Okay", null)
                        .create();
                dialog.show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void createGameBoard()
    {
        gamelogic = new CheckersGame(prefAllowAnyMove);

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);

        TextView topText = new TextView(this);
        topText.setText("Play Checkers");

        statusText = new TextView(this);
        statusText.setText("status");

        checkersView = new CheckersLayout(gamelogic, this);
        checkersView.refresh();

        rootLayout.addView(topText);
        rootLayout.addView(checkersView);
        rootLayout.addView(statusText);

        setContentView(rootLayout);
    }

    public void clearComputerTask() {
        if (computerTask != null) {
            if (computerTask.getStatus() != AsyncTask.Status.FINISHED)
            {
                computerTask.cancel(true);
            }
            computerTask = null;
        }
    }

    // restart game
    private void restart() {
        clearComputerTask();
        gamelogic.restart();
        checkersView.refresh();
        prepTurn();
    }

    Piece selectedPiece;
    Position selectedPosition;
    Piece selectablePieces[];
    Position moveOptions[];

    ComputerTurn computerTask = null;

    // prepare a human or computer turn
    public void prepTurn() {
        Board board = gamelogic.getBoard();

        final MediaPlayer tick=MediaPlayer.create(this,R.raw.tick);
        final MediaPlayer tack=MediaPlayer.create(this,R.raw.wosh);
        final MediaPlayer lost=MediaPlayer.create(this,R.raw.sad);
        final MediaPlayer win=MediaPlayer.create(this,R.raw.win);

        selectedPiece = null;
        selectedPosition = null;
        selectablePieces = null;
        moveOptions = null;

        clearComputerTask();

        int turn = gamelogic.whoseTurn();

        if (turn == CheckersGame.RED) {
            statusText.setText("Red's (computer's) turn. Difficulty: "+prefDifficulty);
            tack.start();
            // run the CPU AI on another thread
            computerTask = new ComputerTurn(this, gamelogic, prefDifficulty, prefAllowAnyMove);
            computerTask.execute();

        } else if (turn == CheckersGame.BLACK) {

            statusText.setText("Black's (player's) turn.");
            tick.start();
            // prep for human player turn
            ArrayList<Piece> selectablePieces = new ArrayList<>();
            Move moves[] = gamelogic.getMoves();

            // find pieces which can be moved
            for (Move move : moves) {
                Piece newPiece = board.getPiece(move.start());
                if (!selectablePieces.contains(newPiece)) {
                    selectablePieces.add(newPiece);
                }
            }

            // convert to array
            this.selectablePieces = selectablePieces.toArray(
                    new Piece[selectablePieces.size()]
            );

            if (selectablePieces.size() == 0) {
                lost.start();
                statusText.setText("You lost!");
            }
        }

        checkersView.refresh();
    }

    // difficulty easy: randomly pick a move
    private void makeComputerTurn(){
    final MediaPlayer sound=MediaPlayer.create(this,R.raw.win);

        if (gamelogic.whoseTurn() == CheckersGame.RED) {
            Move moves[] = gamelogic.getMoves();
            if (moves.length > 0) {
                Move choice;
                if (prefDifficulty.equals("Easy")) {
                    int num = (int)(moves.length * Math.random());
                    choice = moves[num];
                } else {
                    // find best move by capture amount vs king
                    choice = moves[0];
                    int curScore = -1;
                    for (Move option : moves) {
                        int score = option.captures.size();
                        Piece startPiece = gamelogic.getBoard().getPiece(option.start());
                        if (option.kings && !startPiece.isKing())
                        {
                            score += 2;
                        }
                        if (score > curScore) {
                            choice = option;
                            curScore = score;
                        }
                    }
                }
                gamelogic.makeMove(choice);
                prepTurn();
            } else {
                // sound.start();player wins
               // sound.start();
                //wind.start();

                statusText.setText("You won!");
            }
        }
    }

    // check which piece is selected
    public boolean isSelected(Piece piece) {
        return (piece != null && piece == selectedPiece);
    }

    // check which squares are options
    public boolean isOption(Position checkPosition) {
        if (moveOptions == null) {
            return false;
        }
        for (Position position : moveOptions) {
            if (position.equals(checkPosition)) {
                return true;
            }
        }
        return false;
    }

    public void selectPiece(Piece piece, Position location)
    {
        selectedPiece = null;
        selectedPosition = null;
        moveOptions = null;

        if (piece != null && selectablePieces != null
                && piece.getColor() == gamelogic.whoseTurn())
        {
            boolean isSelectable = false;
            for (Piece selectablePiece : selectablePieces) {
                if (selectablePiece == piece) {
                    isSelectable = true;
                }
            }

            if (isSelectable) {
                selectedPiece = piece;
                selectedPosition = location;

                // fill move options

                ArrayList<Position> moveOptionsArr = new ArrayList<>();

                Move allMoves[] = gamelogic.getMoves();

                // iterate through moves
                for (Move checkMove : allMoves) {
                    Position start = checkMove.start();
                    Position end = checkMove.end();

                    if (start.equals(location)) {
                        if (!moveOptionsArr.contains(end)) {
                            moveOptionsArr.add(end);
                        }
                    }
                }

                // save list results
                moveOptions = moveOptionsArr.toArray(new Position[moveOptionsArr.size()]);
            }
        }

        checkersView.refresh();
    }

    // player made a move
    public void makeMove(Position destination)
    {
        // make longest move available
        Move move = gamelogic.getLongestMove(selectedPosition, destination);
        if (move != null) {
            gamelogic.makeMove(move);
            prepTurn();
        }
    }

    // player makes a click
    public void onClick(int x, int y) {
        // check if its player's turn
        if (gamelogic.whoseTurn() != CheckersGame.BLACK) {
            return;
        }

        Position location = new Position(x, y);
        Piece targetPiece = gamelogic.getBoard().getPiece(x, y);

        // attempting to make a move
        if (selectedPiece != null && selectedPosition != null && targetPiece == null) {
            makeMove(location);
        }
        else
        {
            selectPiece(targetPiece, location);
        }
    }

    private SharedPreferences.OnSharedPreferenceChangeListener preferencesChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                    // update preferences
                    prefDifficulty = sharedPreferences.getString(DIFFICULTY, null);
                    prefAllowAnyMove = sharedPreferences.getBoolean(ANY_MOVE, false);

                    gamelogic.setAnyMove(prefAllowAnyMove);

                    prepTurn();
                }
            };
}
