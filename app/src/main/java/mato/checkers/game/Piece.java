package  mato.checkers.game;

/**
 *
 */ // data for a single piece
public class Piece {
    private int _color;
    private boolean _isKing;

    Piece(int color) {
        _color = color;
        _isKing = false;
    }

    Piece(int color, boolean king) {
        _color = color;
        _isKing = king;
    }

    public int getColor() {
        return _color;
    }

    public boolean isKing() {
        return _isKing;
    }

    public void makeKing() {
        _isKing = true;
    }
}
