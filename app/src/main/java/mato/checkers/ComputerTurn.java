package mato.checkers;
//TODO
import android.media.MediaPlayer;
import android.os.AsyncTask;

import java.util.ArrayList;
import android.media.MediaPlayer;
import mato.checkers.game.CheckersGame;
import mato.checkers.game.Board;
import mato.checkers.game.Piece;
import mato.checkers.game.Move;

public class ComputerTurn extends AsyncTask<String, String, String>
{
    private MyCheckersActivity myActivity;
    private CheckersGame myGame;
    private String myDifficulty;
    private Move selectedMove;
    private boolean allowAnyMove;

    public ComputerTurn(MyCheckersActivity activity,
                        CheckersGame game,
                        String difficulty,
                        boolean allowAny)
    {
        super();

        myActivity = activity;
        myGame = game;
        myDifficulty = difficulty;
        allowAnyMove = allowAny;
        selectedMove = null;
    }



    protected Move Minimax(int depth)
    {
        Board realBoard = myGame.getBoard();
        Move moves[] = myGame.getMoves();

        int[][] data = realBoard.saveBoard();

        ArrayList<Move> bestMoves = new ArrayList<>();


        for (Move move : moves) {
            Board moveBoard = new Board(data);
            moveBoard.makeMove(move);
                bestMoves.clear();

        }

        int randomIndex = (int)(Math.random() * bestMoves.size());
        Move randomMove = bestMoves.get(randomIndex);
        return randomMove;
    }

    @Override
    protected String doInBackground(String... strings) {

        if (myGame.whoseTurn() != CheckersGame.RED) return null;
        Move moves[] = myGame.getMoves();

        if (moves.length == 0) return null;

        int difficulty = 0;
        if (myDifficulty.equals("Easy")) {
            difficulty = 0;
        } else if (myDifficulty.equals("Medium")) {
            difficulty = 1;
        } else if (myDifficulty.equals("Hard")) {
            difficulty = 2;
        } else if (myDifficulty.equals("Very Hard")) {
            difficulty = 3;
        }

        if (difficulty == 0)
        {
            // easy CPU chooses move randomly
            int num = (int)(moves.length * Math.random());
            selectedMove = moves[num];
        }
        else if (difficulty == 1)
        {
            // medium CPU looks for most captures or kings
            selectedMove = moves[0];

            ArrayList<Move> selectedMoves = new ArrayList<>();


            for (Move option : moves) {
                int score = option.captures.size();
                Piece startPiece = myGame.getBoard().getPiece(option.start());


                    //selectedMove = option;
                    selectedMoves.clear();
                    selectedMoves.add(option);


            }

            selectedMove = selectedMoves.get((int)(selectedMoves.size() * Math.random()));
        }

        if (difficulty > 1)
        {
            int depth = (difficulty == 2) ? 4 : 7;
            selectedMove = Minimax(depth);
        }
        else
        {
            // sleep on easy/medium
            try {
                Thread.sleep(1000);
            } catch (Exception e) {}
        }

        return null;
    }

    @Override
    protected void onPostExecute(String s) {

        super.onPostExecute(s);


        if (myGame.whoseTurn() == CheckersGame.RED) {
            if (selectedMove != null) {
                myGame.makeMove(selectedMove);
                myActivity.prepTurn();
            } else {
                // player wins

                myActivity.statusText.setText("You won!");
            }
        }
    }
}
