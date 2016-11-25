package strategy;

import model.Board;
import model.Piece;

public class CornerCapturedStrategy implements Strategy {
  @Override public int getBoardHeuristicValue(Board board, Piece piece) {
    int blackCorners = board.getAmountCornerCaptured(Piece.BLACK);
    int whiteCorners = board.getAmountCornerCaptured(Piece.WHITE);

    if (blackCorners + whiteCorners == 0) {
      return 0;
    }

    return 100 * (blackCorners - whiteCorners) / (blackCorners + whiteCorners);
  }
}
