package game.players;

import game.Board;
import game.feedbacks.Feedback;
import game.pieces.Piece;
import game.pieces.PieceAction;
import game.pieces.PieceFactory;
import game.pieces.QuantityPerPiece;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RaiAraujoPlayer implements Player {
  private String playerName = "Raí Araujo";
  private int[] prisonerPosition;
  private Piece[][] setup = new Piece[4][10];

  public RaiAraujoPlayer(String name) {
    this.playerName = name;
  }

  @Override
  public String getPlayerName() {
    return this.playerName;
  }

  @Override
  public Piece[][] setup(Board board) {
    List<String> pieces = generatePiecesList();

    placePrisoner(pieces, board);
    placeMinesAroundPrisoner(pieces, board);
    placeAgent(pieces, board);
    placeCorporals(pieces, board);
    placeRemainingPieces(pieces, board);

    return setup;
  }

  private List<String> generatePiecesList() {
    List<String> pieces = new ArrayList<>();
    for (QuantityPerPiece piece : QuantityPerPiece.values()) {
      pieces.addAll(Collections.nCopies(piece.getQuantity(), piece.getCode()));
    }
    return pieces;
  }

  private void placePrisoner(List<String> pieces, Board board) {
    int prisonerRow = 3;
    int prisonerCol = new Random().nextInt(10);

    prisonerPosition = new int[] { prisonerRow, prisonerCol };
    placePiece(pieces, "PS", prisonerRow, prisonerCol, board);
  }

  // Posiciona minas ao redor do prisioneiro
  private void placeMinesAroundPrisoner(List<String> pieces, Board board) {
    int minesPlaced = 0;
    int maxMines = 3;

    for (int[] pos : getAdjacentPositions(prisonerPosition[0], prisonerPosition[1])) {
      if (minesPlaced >= maxMines)
        break;

      if (placePiece(pieces, "M", pos[0], pos[1], board))
        minesPlaced++;
    }
  }

  private void placeAgent(List<String> pieces, Board board) {
    int row = 2;
    int col = new Random().nextInt(10);

    placePiece(pieces, "AS", row, col, board);
    placePiece(pieces, "M", row - 1, col, board); // Coloca uma mina na frente do agente
  }

  private void placeCorporals(List<String> pieces, Board board) {
    List<Integer> columns = new ArrayList<>();
    int row = 3;

    for (int i = 0; i < 10; i++)
      columns.add(i);

    Collections.shuffle(columns); // Embaralha as colunas para posicionamento aleatório

    for (int col : columns)
      placePiece(pieces, "C", row, col, board);
  }

  private void placeRemainingPieces(List<String> pieces, Board board) {
    Collections.shuffle(pieces);

    for (int row = 0; row < 4; row++) {
      for (int col = 0; col < 10; col++) {
        if (pieces.isEmpty())
          break;
        placePiece(pieces, pieces.get(0), row, col, board);
      }
    }
  }

  private boolean placePiece(List<String> pieces, String code, int row, int col, Board board) {
    if (Board.isValidPosition(row, col) && setup[row][col] == null && pieces.contains(code)) {
      setup[row][col] = PieceFactory.createPiece(code, playerName, board);
      pieces.remove(code);
      return true;
    }
    return false;
  }

  private int[][] getAdjacentPositions(int row, int col) {
    return new int[][] {
        { row - 1, col }, // Acima
        { row, col - 1 }, { row, col + 1 }, // Lados
        { row - 1, col - 1 }, { row - 1, col + 1 } // Diagonais superiores
    };
  }

  @Override
  public PieceAction play(Board board, Feedback myLastFeedback, Feedback enemyLastFeedback) {
    // Implementação da jogada seria aqui
    return null;
  }
}