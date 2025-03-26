package game;

import game.feedbacks.*;
import game.pieces.OpponentPiece;
import game.pieces.Piece;
import game.pieces.PieceAction;
import game.pieces.QuantityPerPiece;
import game.players.Player;

import java.util.*;

import static game.feedbacks.Feedback.convertIntToAlfa;

public class Board {
    private static final int MAX_NUMBER_OF_MOVES = 5000;

    private Piece[][] board;
    public static final int ROWS = 10;
    public static final int COLS = 10;
    public Player player1;
    private final Deque<Piece> lastPiecesPlayedByP1 = new ArrayDeque<>(MAX_CONSECUTIVE_MOVES_SAME_PIECE);
    public Player player2;
    private final Deque<Piece> lastPiecesPlayedByP2 = new ArrayDeque<>(MAX_CONSECUTIVE_MOVES_SAME_PIECE);
    public int numberMoves = 0;
    public static final Integer MAX_CONSECUTIVE_MOVES_SAME_PIECE = 3;
    public static final String PLAYER1_COLOR_OPEN = "\u001B[32m";
    public static final String PLAYER2_COLOR_OPEN = "\u001B[31m";
    public static final String LAKE_COLOR_OPEN = "\u001B[34m";
    public static final String COLOR_CLOSE = "\u001B[0m";

    public Board() {
        board = new Piece[ROWS][COLS];
    }

    public Board(Board original) {
        player1 = original.player1;
        player2 = original.player2;
        numberMoves = original.numberMoves;
        board = new Piece[ROWS][COLS];
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                Piece originalPiece = original.getPiece(i, j);
                this.board[i][j] = (originalPiece != null) ? originalPiece.copy(this) : null;
            }
        }
    }

    public Piece getPiece(int x, int y) {
        if (x < 0 || x >= ROWS || y < 0 || y >= COLS) {
            return null;
        }
        return board[x][y];
    }

    public void setPiece(int x, int y, Piece piece) {
        board[x][y] = piece;
        if (piece != null) {
            piece.setPosition(x, y);
        }
    }

    public static boolean isValidPosition(int x, int y) {
        return x >= 0 && x < 10 && y >= 0 && y < 10 && !Board.isLake(x, y);
    }

    public static boolean isLake(int x, int y) {
        return ((((x == 4 && y == 2) || (x == 5 && y == 2)) || ((x == 4 && y == 3) || (x == 5 && y == 3))) ||
                (((x == 4 && y == 6) || (x == 5 && y == 6)) || ((x == 4 && y == 7) || (x == 5 && y == 7))));
    }

    public boolean isValidSetup(Piece[][] playerSetup) {
        if (playerSetup.length != 4 || playerSetup[0].length != 10) {
            System.out.println("Erro: Matriz com peças de tamanho incorreto. Esperado 4x10, encontrado " + playerSetup.length +"x"+playerSetup[0].length);
            return false;
        }

        Map<String, Integer> actualCounts = new HashMap<>();

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 10; j++) {
                Piece piece = playerSetup[i][j];
                if (piece != null) {
                    String code = piece.getRepresentation();
                    actualCounts.put(code, actualCounts.getOrDefault(code, 0) + 1);
                }
            }
        }

        for (QuantityPerPiece qpp : QuantityPerPiece.values()) {
            String code = qpp.getCode();
            int expected = qpp.getQuantity();
            int actual = actualCounts.getOrDefault(code, 0);

            if (actual != expected) {
                System.out.println("Erro: Esperado " + expected + " peças de " + code + ", mas encontrado " + actual);
                return false;
            }
        }

        return true;
    }

    /**
    * Adiciona posicionamento inicial do jogador ao tabuleiro
    */
    public boolean addPlayerSetup(Piece[][] playerSetup, int player) {
        if (!isValidSetup(playerSetup)) {
            return false;
        }

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 10; j++) {
                int row = (player == 1) ? (3 - i) : (i + 6);
                int col = (player == 1) ? (9 - j) : j;
                this.setPiece(row, col, playerSetup[i][j]);
            }
        }

        return true;
    }

    /**
     * Retorna uma representação em String do estado atual do tabuleiro.
     */
    public String getFeedback() {
        StringBuilder sb = new StringBuilder();

        sb.append("   ");
        for (int j = 0; j < COLS; j++) {
            sb.append(String.format(" %-2d ", j + 1));
        }
        sb.append("\n");

        for (int i = 0; i < ROWS; i++) {
            sb.append(String.format("%-2s ", (char) ('A' + i)));

            for (int j = 0; j < COLS; j++) {
                if (Board.isLake(i, j)) {
                    sb.append(String.format("[%s%-2s%s]", LAKE_COLOR_OPEN, "XX", COLOR_CLOSE));
                } else if (board[i][j] == null) {
                    sb.append(String.format("[%-2s]", " "));
                } else {
                    Piece piece = board[i][j];
                    String playerColor = piece.getPlayer() != null && piece.getPlayer().equals(player1.getPlayerName()) ? PLAYER1_COLOR_OPEN : PLAYER2_COLOR_OPEN;
                    sb.append(String.format("[%s%-2s%s]", playerColor, piece.getRepresentation(), COLOR_CLOSE));
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public final Board getHiddenView(String player) {
        Board boardCopy = this.deepCopy();
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                Piece piece = boardCopy.getPiece(i, j);

                if (piece != null && !piece.getPlayer().equals(player)) {
                    boardCopy.setPiece(i, j, new OpponentPiece(this));
                }
            }
        }
        return boardCopy;
    }

    private Board deepCopy() {
        return new Board(this);
    }


    /**
     * Retorna a indicação se o jogo acabou por um dos motivos:
     *  - Algum player sem peças móveis
     *  - Número máximo de jogadas
     */
    public Feedback isGameFinished() {
        Player p = somePlayerHasMove();
        if (p != null) {
            return new PlayerWithoutPiecesFeedback(p);
        }

        if (numberMoves >= MAX_NUMBER_OF_MOVES) {
            return new MaxNumberOfMovesFeedback();
        }

        return null;
    }

    private Player somePlayerHasMove() {
        boolean player1HasMovablePiece = false;
        boolean player2HasMovablePiece = false;

        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                if (board[i][j] != null) {
                    Piece p = board[i][j];
                    if (p.getPlayer().equals(player1.getPlayerName())) {
                        if (isMovablePiece(p)) {
                            player1HasMovablePiece = true;
                        }
                    } else if (p.getPlayer().equals(player2.getPlayerName())) {
                        if (isMovablePiece(p)) {
                            player2HasMovablePiece = true;
                        }
                    }
                }
            }
        }

        if (player1HasMovablePiece && player2HasMovablePiece) {
            return null;
        } else if (!player1HasMovablePiece) {
            return player1;
        } else {
            return player2;
        }
    }

    private boolean isMovablePiece(Piece piece) {
        if (piece == null) {
            return false;
        }

        return !piece.getRepresentation().equals("M") && !piece.getRepresentation().equals("PS");
    }

    public Feedback executeAction(PieceAction action) {
        numberMoves++;

        if (action == null || action.getPiece() == null) {
            return new InvalidMoveFeedback("Não há peça selecionada");
        }

        try {
            Piece piece = action.getPiece();
            Piece pieceOriginal = board[piece.getPosX()][piece.getPosY()];
            int newPosX = action.getNewPosX();
            int newPosY = action.getNewPosY();

            if (addLastPiecesPlayed(pieceOriginal)) {
                return pieceOriginal.move(newPosX, newPosY, this);
            }

            return new InvalidMoveFeedback(String.format("%s moveu a mesma peça mais de 3 vezes, %s em [%s, %d]", pieceOriginal.getPlayer(), pieceOriginal.getRepresentation(), convertIntToAlfa(pieceOriginal.getPosX()), pieceOriginal.getPosY()));
        } catch (Exception e) {
            return new InvalidMoveFeedback(e.getMessage());
        }
    }

    public boolean addLastPiecesPlayed(Piece piece) {
        var lastPiecesPlayed = player1.getPlayerName().equals(piece.getPlayer()) ? lastPiecesPlayedByP1 : lastPiecesPlayedByP2;

        var result = true;
        if (lastPiecesPlayed.size() == MAX_CONSECUTIVE_MOVES_SAME_PIECE) {
            if (lastPiecesPlayed.stream().allMatch(p -> p.equals(piece))) {
                result = false;
            }
        }

        if (lastPiecesPlayed.size() == MAX_CONSECUTIVE_MOVES_SAME_PIECE) {
            lastPiecesPlayed.removeLast();
        }
        lastPiecesPlayed.push(piece);

        return result;
    }
}
