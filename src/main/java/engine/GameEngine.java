package engine;

import com.google.common.base.Throwables;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import model.Board;
import model.Coordinate;
import model.Piece;
import strategy.MiniMaxTreeStrategy;
import strategy.Strategy;

public class GameEngine {
  private final static String BOARD_PREFIX = "  ";

  private static final ExecutorService executor = Executors.newSingleThreadExecutor();

  private Node boardNode;

  private Strategy strategy;

  private MiniMaxTreeStrategy miniMaxTreeStrategy;

  /**
   * GameEngine without min-max tree strategy
   * @param strategy
   */
  public GameEngine(Strategy strategy) {
    this.boardNode = new Node(Board.newInstance());
    this.strategy = strategy;
  }

  /**
   * GameEngine with min-max tree strategy
   * @param miniMaxTreeStrategy
   */
  public GameEngine(MiniMaxTreeStrategy miniMaxTreeStrategy) {
    this.boardNode = new Node(Board.newInstance());
    this.miniMaxTreeStrategy = miniMaxTreeStrategy;
  }

  /**
   * GameEngine without min-max tree strategy, and load board from local file
   * @param strategy
   * @param boardFilePath
   */
  public GameEngine(Strategy strategy, String boardFilePath) {
    this.boardNode = new Node(Board.newInstance(boardFilePath));
    this.strategy = strategy;
  }

  /**
   * GameEngine with min-max tree strategy, and load board from local file
   * @param miniMaxTreeStrategy
   * @param boardFilePath
   */
  public GameEngine(MiniMaxTreeStrategy miniMaxTreeStrategy, String boardFilePath) {
    this.boardNode = new Node(Board.newInstance(boardFilePath));
    this.miniMaxTreeStrategy = miniMaxTreeStrategy;
  }

  /**
   * Place a given piece with given coordinate on board, and perform robot action to it.
   * @param coordinate
   * @param piece
   */
  public void placePieceByHuman(Coordinate coordinate, Piece piece) {
    if (!this.boardNode.board.isContain(coordinate) || !this.boardNode.board.isValidMove(coordinate,
        piece)) {
      System.err.printf("Invalid coordinate [%s]\n", coordinate);
      return;
    }

    Node tempNode = new Node(this.boardNode);
    tempNode.board = this.boardNode.board.placePiece(coordinate, piece);
    if (isOver()) {
      return;
    }
    this.boardNode = tempNode;

    placePieceByRobot(piece.getOpposite());
    if (isOver()) {
      return;
    }

    while (this.boardNode.board.getValidMoves(piece).isEmpty()) {
      System.out.printf("Skipped %s piece step\n", piece);
      placePieceByRobot(piece.getOpposite());

      if (isOver()) {
        return;
      }
    }
  }

  /**
   * Place a piece on board, which is executed in a separate thread.
   * @param piece
   */
  public void placePieceByRobot(final Piece piece) {

    Future<Void> future = executor.submit(new Callable<Void>() {
      @Override public Void call() throws Exception {
        placePiece(piece);
        return null;
      }
    });

    try {
      future.get(Othello.ROBOT_TIME_LIMIT_SECONDS, TimeUnit.SECONDS);
    } catch (InterruptedException exception) {
      Throwables.propagate(exception);
    } catch (ExecutionException exception) {
      System.out.println(exception);
    } catch (TimeoutException exception) {
      System.err.println("Current strategy is out of time limit");
      future.cancel(true);
    }
  }

  /**
   * Place a given piece on board.
   * @param piece
   */
  private void placePiece(final Piece piece) {
    List<Board> childBoards = this.boardNode.board.getChildBoards(piece);

    if (childBoards.isEmpty()) {
      System.out.printf("Skipped [%s] piece step\n", piece);
      return;
    }

    Node tempNode = new Node(this.boardNode);
    if (this.strategy == null) {
      tempNode.board = this.miniMaxTreeStrategy.getNextBoard(this.boardNode.board, piece);
    } else {
      tempNode.board = Collections.max(childBoards, new Comparator<Board>() {
        @Override public int compare(Board board1, Board board2) {
          return strategy.getBoardHeuristicValue(board1, piece) - strategy.getBoardHeuristicValue(
              board2, piece);
        }
      });
    }
    this.boardNode = tempNode;
  }

  /**
   * Check if the game is over.
   * @return
   */
  public boolean isOver() {
    boolean isGameOver = this.boardNode.board.isOver();
    if (isGameOver) {
      System.out.printf("The winner is [%s]\n", this.boardNode.board.getWinner());
    }
    return isGameOver;
  }

  /**
   * Return current board layout as string.
   * @return
   */
  public String getBoardLayout() {
    return this.boardNode.board.toString();
  }

  /**
   * Print current game state successive boards layout.
   */
  public void logSuccessiveBoards() {
    Node node = this.boardNode;
    String prefix = "";

    while (node != null) {
      System.out.print(node.board.toString(prefix));
      prefix += BOARD_PREFIX;
      node = node.parent;
    }
  }

  /**
   * Search node is used for holding current board and maintaining a pointer to previous node.
   */
  private class Node {
    public Board board;
    public Node parent;

    public Node(Board board) {
      this.board = board;
    }

    public Node(Node parent) {
      this.parent = parent;
    }
  }
}
