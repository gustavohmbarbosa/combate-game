package game;

import game.feedbacks.ConvertFeedbackToEnemy;
import game.feedbacks.Feedback;
import game.feedbacks.PrisonerFeedback;
import game.pieces.Piece;
import game.pieces.PieceAction;
import game.players.Player;

import java.util.Random;

public class Game {
    private final Board board;
    private final Player player1;
    private final Player player2;
    private int round = 0;

    public Game(Player player1, Player player2) {
        board = new Board();
        board.player1 = player1;
        board.player2 = player2;
        this.player1 = player1;
        this.player2 = player2;
    }

    private void increaseRound() {
        this.round++;
    }

    private int getRound() { return round; }

    /**
     * Inicia o jogo.
     */
    public int start() {
        Piece[][] player1Setup = player1.setup(this.board);
        var player1SetupIsValid = this.board.addPlayerSetup(player1Setup, 1);
        Piece[][] player2Setup = player2.setup(this.board);
        var player2SetupIsValid = this.board.addPlayerSetup(player2Setup, 2);

        if (!player1SetupIsValid && !player2SetupIsValid) {
            System.out.println("Jogo concluído por setup inválido de ambos jogadores!");
            System.out.println("Jogo empatado!");
            return 0;
        } else if (!player1SetupIsValid) {
            System.out.println("Jogo concluído por setup inválido!");
            System.out.println("Jogador " + player2.getPlayerName() + " venceu o jogo!");
            return 2;
        } else if (!player2SetupIsValid) {
            System.out.println("Jogo concluído por setup inválido!");
            System.out.println("Jogador " + player1.getPlayerName() + " venceu o jogo!");
            return 1;
        }

        System.out.println("Estado inicial do tabuleiro:");
        System.out.println(board.getFeedback());

        Random rand = new Random();
        boolean actualPlayer = rand.nextBoolean();

        Feedback roundFeedback = null;
        Feedback lastPlayer1Feedback = null;
        Feedback lastPlayer2Feedback = null;

        game:
        while (true) {
            System.out.println("Rodada " + this.getRound() + ":");

            for (int i = 0; i < 2; i++) {
                if (actualPlayer) {
                    // Jogada do Player1
                    PieceAction action = player1.play(
                            board.getHiddenView(player1.getPlayerName()),
                            lastPlayer1Feedback,
                            ConvertFeedbackToEnemy.convert(lastPlayer2Feedback)
                    );
                    roundFeedback = board.executeAction(action);
                    lastPlayer1Feedback  = roundFeedback;
                    System.out.println("Player1: " + roundFeedback.getMessage());
                    System.out.println(board.getFeedback());
                } else {
                    // Jogada do Player2
                    PieceAction action = player2.play(
                            board.getHiddenView(player2.getPlayerName()),
                            lastPlayer2Feedback,
                            ConvertFeedbackToEnemy.convert(lastPlayer1Feedback)
                    );
                    roundFeedback = board.executeAction(action);
                    lastPlayer2Feedback  = roundFeedback;
                    System.out.println("Player2: " + roundFeedback.getMessage());
                    System.out.println(board.getFeedback());
                }

                if (roundFeedback instanceof PrisonerFeedback) {
                    String playerName = actualPlayer ? player1.getPlayerName() : player2.getPlayerName();

                    System.out.println("Jogo concluído com sucesso!!!");
                    System.out.println("Parabéns ao jogador " + playerName + "!!!");
                    return actualPlayer ? 1 : 2;
                }

                actualPlayer = !actualPlayer;
                Feedback actualState = board.isGameFinished();
                if (actualState != null){
                    System.out.println(actualState.getMessage());
                    return 0;
                }
            }
            this.increaseRound();
        }
    }
}
