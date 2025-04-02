package game.players;

import java.util.*;
import game.*;
import game.feedbacks.Feedback;
import game.pieces.*;

public class BrendoBritoPlayer implements Player {
    private final String playerName = "brendoEJose";
    private final Map<String, Double> pieceStrength = new HashMap<>();
    private final Map<String, Integer> remainingEnemyPieces = new HashMap<>();
    private final List<MoveHistory> moveHistory = new ArrayList<>();
    private final Random random = new Random();
    private int turnCount = 0;

    private record MoveHistory(int fromX, int fromY, int toX, int toY, String pieceType) {}

    // Novo construtor que aceita nome do jogador
    public BrendoBritoPlayer() {
        initializePieceStrength();
        initializeEnemyPieces();
    }

    private void initializePieceStrength() {
        pieceStrength.put("PS", 0.0);
        pieceStrength.put("AS", 1.0);
        pieceStrength.put("S", 2.0);
        pieceStrength.put("C", 3.0);
        pieceStrength.put("SG", 4.0);
        pieceStrength.put("ST", 5.0);
        pieceStrength.put("T", 6.0);
        pieceStrength.put("CP", 7.0);
        pieceStrength.put("MJ", 8.0);
        pieceStrength.put("CR", 9.0);
        pieceStrength.put("G", 10.0);
        pieceStrength.put("M", -1.0);
    }

    private void initializeEnemyPieces() {
        for (QuantityPerPiece piece : QuantityPerPiece.values()) {
            remainingEnemyPieces.put(piece.getCode(), piece.getQuantity());
        }
    }

    @Override
    public String getPlayerName() {
        return this.playerName;
    }

    // Configura o tabuleiro inicial do jogador
    @Override
    public Piece[][] setup(Board board) {
        Piece[][] setup = new Piece[4][10];
        List<String> pieces = new ArrayList<>();
        Random rand = new Random();

        // Adiciona todas as peças exceto o prisioneiro
        for (QuantityPerPiece piece : QuantityPerPiece.values()) {
            if (!piece.getCode().equals("PS")) {
                for (int i = 0; i < piece.getQuantity(); i++) {
                    pieces.add(piece.getCode());
                }
            }
        }

        // Embaralha as peças
        Collections.shuffle(pieces);

        int prisonerCol = rand.nextInt(10);

        // Preenche o tabuleiro com as peças embaralhadas
        int pieceIndex = 0;
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 10; col++) {
                if (row == 3 && col == prisonerCol) {  // Posição fixa para o prisioneiro
                    setup[row][col] = createPiece("PS", board);
                } else {
                    setup[row][col] = createPiece(pieces.get(pieceIndex++), board);
                }
            }
        }

        return setup;
    }

    private Piece createPiece(String code, Board board) {
        return PieceFactory.createPiece(code, playerName, board);
    }

    //  Metodo principal que decide e executa o movimento do agente.
    //  Avalia todas as possíveis jogadas, calcula seus scores e seleciona a melhor.
    @Override
    public PieceAction play(Board board, Feedback myLastFeedback, Feedback enemyLastFeedback) {
        turnCount++;
        updateKnowledge();

        List<ScoredMove> possibleMoves = new ArrayList<>();

        // Avalia todas as peças do jogador e seus possíveis movimentos
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                Piece piece = board.getPiece(x, y);
                if (piece != null && playerName.equals(piece.getPlayer())) {
                    String type = piece.getRepresentation();
                    if (!type.equals("PS") && !type.equals("M")) {
                        evaluatePossibleMoves(board, possibleMoves, piece, x, y);
                    }
                }
            }
        }

        if (possibleMoves.isEmpty()) {
            return null;
        }

        // Ordena os movimentos pelo score e seleciona o melhor
        possibleMoves.sort((a, b) -> Double.compare(b.score, a.score));
        ScoredMove chosenMove = selectMove(possibleMoves);
        updateMoveHistory(chosenMove);

        return new PieceAction(chosenMove.piece, chosenMove.toX, chosenMove.toY);
    }

    // Avalia todas as direções possíveis para mover uma peça.
    private void evaluatePossibleMoves(Board board, List<ScoredMove> possibleMoves, Piece piece, int x, int y) {
        String pieceType = piece.getRepresentation();
        double piecePower = pieceStrength.get(pieceType);

        // Avalia movimentos nas 4 direções possíveis
        evaluateDirection(board, possibleMoves, piece, x, y, x+1, y, piecePower, pieceType);
        evaluateDirection(board, possibleMoves, piece, x, y, x-1, y, piecePower, pieceType);
        evaluateDirection(board, possibleMoves, piece, x, y, x, y+1, piecePower, pieceType);
        evaluateDirection(board, possibleMoves, piece, x, y, x, y-1, piecePower, pieceType);
    }

    // Avalia um movimento específico em uma direção.
    private void evaluateDirection(Board board, List<ScoredMove> possibleMoves,
                                   Piece piece, int fromX, int fromY,
                                   int toX, int toY, double piecePower, String pieceType) {
        if (!piece.canMove(toX, toY) || !Board.isValidPosition(toX, toY)) {
            return;
        }

        Piece target = board.getPiece(toX, toY);
        double score;

        if (target == null) {
            // Movimento para posição vazia
            score = calculatePositioningScore(fromX, fromY, toX, toY, pieceType);
        } else if (target.getPlayer() == null) {
            // Ataque a peça inimiga
            score = calculateAttackScore(piecePower, pieceType, toX, toY);
        } else {
            return; // Peça aliada - movimento inválido
        }

        // Aplica penalidade por repetição de movimento
        score -= calculateRepetitionPenalty(fromX, fromY, toX, toY);
        possibleMoves.add(new ScoredMove(piece, fromX, fromY, toX, toY, score, pieceType));
    }

    // Calcula o valor estratégico de se mover para uma posição vazia.
    private double calculatePositioningScore(int fromX, int fromY, int toX, int toY, String pieceType) {
        // Incentiva peças mais fracas a avançar
        double advanceScore = (fromX - toX) * (5.0 - pieceStrength.get(pieceType)) * 0.2;
        // Mantém peças fortes mais protegidas
        double protectionScore = pieceStrength.get(pieceType) > 7 ? (toX - fromX) * 0.3 : 0;
        // Penaliza aglomeração de peças
        double dispersionPenalty = moveHistory.stream()
                .filter(move -> Math.hypot(move.toX - toX, move.toY - toY) < 2.0)
                .count() * -0.5;

        return advanceScore + protectionScore + dispersionPenalty;
    }

    // Calcula o valor de atacar uma peça inimiga.
    private double calculateAttackScore(double myPower, String myType, int targetX, int targetY) {
        // Calcula a probabilidade média de vitória contra todas as peças inimigas possíveis
        double totalScore = remainingEnemyPieces.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .mapToDouble(entry -> {
                    double prob = calculateWinProbability(myPower, myType, entry.getKey());
                    return prob * entry.getValue();
                }).sum();

        int totalPossible = remainingEnemyPieces.values().stream()
                .mapToInt(v -> v)
                .sum();

        if (totalPossible == 0) return 0;

        // Score baseado na probabilidade de vitória média
        double baseScore = (totalScore / totalPossible) * 10;
        // Bônus por atacar perto da bandeira inimiga (assumida nas primeiras linhas)
        double positionBonus = targetX <= 2 ? 1.5 : 0;

        return baseScore + positionBonus;
    }

    // Calcula a probabilidade de vitória em um combate específico.
    private double calculateWinProbability(double myPower, String myType, String enemyType) {
        // Casos especiais
        if (myType.equals("AS") && enemyType.equals("G")) return 0.95;  // Agente secreto vs General
        if (myType.equals("S") && enemyType.equals("AS")) return 0.9;  // Soldado vs Agente Secreto
        if (enemyType.equals("M")) return myType.equals("C") ? 0.8 : 0.0;  // Minas só podem ser desarmadas por Cabos

        // Combates normais baseados na diferença de força
        double enemyPower = pieceStrength.get(enemyType);
        double powerDiff = myPower - enemyPower;

        if (powerDiff > 0) {
            return 0.75 + 0.24 * powerDiff / 10.0;  // Chance maior para peças mais fortes
        } else if (powerDiff < 0) {
            return 0.01 + 0.24 * (1 + powerDiff / 10.0);  // Chance menor para peças mais fracas
        }
        return 0.5;  // Empate técnico
    }

    // Calcula penalidade por movimentos repetidos.
    private double calculateRepetitionPenalty(int fromX, int fromY, int toX, int toY) {
        return moveHistory.stream()
                .mapToDouble(move ->
                        move.fromX == fromX && move.fromY == fromY && move.toX == toX && move.toY == toY ? 3.0 :  // Penalidade alta para movimento idêntico
                                move.toX == toX && move.toY == toY ? 1.0 : 0)  // Penalidade menor para destino repetido
                .sum();
    }

    // Seleciona o movimento a ser executado com base nos scores calculados.
    // Tem 80% de chance de escolher o melhor movimento e
    // 30% de escolher aleatoriamente entre os 3 melhores.
    private ScoredMove selectMove(List<ScoredMove> moves) {
        if (moves.size() > 1 && random.nextDouble() < 0.3) {
            int top = Math.min(4, moves.size());
            return moves.get(random.nextInt(top));
        }
        return moves.getFirst();
    }

    // Atualiza o histórico de movimentos, mantendo apenas os últimos 5 movimentos.
    private void updateMoveHistory(ScoredMove move) {
        moveHistory.add(new MoveHistory(
                move.fromX, move.fromY,
                move.toX, move.toY,
                move.pieceType
        ));

        if (moveHistory.size() > 1000000) {
            moveHistory.removeFirst();
        }
    }

    // Atualiza o conhecimento sobre as peças inimigas restantes. A cada 5 turnos, reduz a quantidade estimada de peças inimigas.
    private void updateKnowledge() {
        if (turnCount % 5 == 0) {
            remainingEnemyPieces.replaceAll((k, v) -> Math.max(0, v - (v > 3 ? 1 : 0)));
        }
    }

    // Registro para armazenar um movimento possível com seu score calculado.
    private record ScoredMove(Piece piece, int fromX, int fromY, int toX, int toY, double score, String pieceType) {}
}