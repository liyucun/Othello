package strategy;

import engine.GameEngine;
import model.Board;
import model.Piece;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GreedyStrategyTest {

  @Test public void testGetBoardHeuristicValue() {
    Board board = Board.newInstance("strategy/greedy_game_board.txt");
    Strategy strategy = new GreedyStrategy();
    int actual = strategy.getBoardHeuristicValue(board, Piece.BLACK);
    int expected = 7;

    assertEquals(expected, actual);
  }
}
