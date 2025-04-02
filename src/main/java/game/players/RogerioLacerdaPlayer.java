package game.players;

import game.Board;
import game.feedbacks.*;
import game.pieces.Piece;
import game.pieces.PieceAction;
import game.pieces.PieceFactory;
import game.pieces.QuantityPerPiece;

import java.util.*;

public class RogerioLacerdaPlayer implements Player {
    private String nomeJogador = "Rogerio Lacerda";
    private Board tabuleiroOcultoAnterior;
    private Map<String, Map<String, Double>> estadoCrença;
    private Map<String, String> knownOpponentPieces;
    private Map<String, Integer> contagemImobilidade;
    private Map<String, Integer> revealedCounts;
    private static final double IMPULSO_IMOBILIDADE = 1.1;
    private static final double IMPULSO_MOVIMENTO_LONGO = 2.0;
    private Deque<Piece> lastMovedPieces = new ArrayDeque<>(3); 
    private Piece lastAttemptedPiece;
    private int attackCount = 0;  
    private int turnCount = 0; 
    private static final int MAX_TURNS = 2500; 

    public RogerioLacerdaPlayer() {
        this.estadoCrença = new HashMap<>();
        this.knownOpponentPieces = new HashMap<>();
        this.contagemImobilidade = new HashMap<>();
        this.revealedCounts = new HashMap<>();
        this.lastAttemptedPiece = null;
    }

    @Override
    public String getPlayerName() {
        return nomeJogador;
    }

    @Override
    public Piece[][] setup(Board tabuleiro) {
        Piece[][] configuracao = new Piece[4][10];
        List<String> pecasRestantes = new ArrayList<>();
    
        
        for (QuantityPerPiece qpp : QuantityPerPiece.values()) {
            for (int i = 0; i < qpp.getQuantity(); i++) {
                pecasRestantes.add(qpp.getCode());
            }
        }
        Collections.shuffle(pecasRestantes);
    
        
        int linhaPrisioneiro;
        int colunaPrisioneiro = 0; 
        if (nomeJogador.equals(tabuleiro.player1.getPlayerName())) {
            linhaPrisioneiro = 3; 
        } else { 
            linhaPrisioneiro = 3; 
        }
    
        
        configuracao[linhaPrisioneiro][colunaPrisioneiro] = PieceFactory.createPiece("PS", nomeJogador, tabuleiro);
        pecasRestantes.remove("PS");
    
        
        int[][] posBombas = new int[2][2];
        if (nomeJogador.equals(tabuleiro.player1.getPlayerName())) {
            posBombas[0] = new int[]{linhaPrisioneiro, colunaPrisioneiro + 1}; 
            posBombas[1] = new int[]{linhaPrisioneiro + 1, colunaPrisioneiro}; 
        } else { 
            posBombas[0] = new int[]{linhaPrisioneiro, colunaPrisioneiro + 1}; 
            posBombas[1] = new int[]{linhaPrisioneiro - 1, colunaPrisioneiro}; 
        }
    
        for (int[] pos : posBombas) {
            int linha = pos[0];
            int coluna = pos[1];
            if (linha >= 0 && linha < 4 && coluna >= 0 && coluna < 10 && pecasRestantes.contains("M")) {
                configuracao[linha][coluna] = PieceFactory.createPiece("M", nomeJogador, tabuleiro);
                pecasRestantes.remove("M");
            }
        }
    
        
        List<String> altaPatente = Arrays.asList("AS", "G", "CR");
        for (int j = 0; j < 10; j++) {
            if (configuracao[0][j] == null && !pecasRestantes.isEmpty()) {
                String peca = altaPatente.get(new Random().nextInt(altaPatente.size()));
                while (!pecasRestantes.contains(peca)) {
                    peca = pecasRestantes.get(new Random().nextInt(pecasRestantes.size()));
                }
                configuracao[0][j] = PieceFactory.createPiece(peca, nomeJogador, tabuleiro);
                pecasRestantes.remove(peca);
            }
        }
    
        
        for (int i = 1; i < 4 && !pecasRestantes.isEmpty(); i++) {
            for (int j = 0; j < 10 && !pecasRestantes.isEmpty(); j++) {
                if (configuracao[i][j] == null) {
                    String peca = pecasRestantes.remove(0);
                    configuracao[i][j] = PieceFactory.createPiece(peca, nomeJogador, tabuleiro);
                }
            }
        }
    
        return configuracao;
    }

    @Override
    public PieceAction play(Board tabuleiro, Feedback meuUltimoFeedback, Feedback ultimoFeedbackInimigo) {
        if (estadoCrença.isEmpty()) {
            inicializarEstadoCrença(tabuleiro);
        } else {
            atualizarEstadoCrençaComFeedback(tabuleiro, meuUltimoFeedback, ultimoFeedbackInimigo);
        }
    
        
        if (meuUltimoFeedback != null && (
            meuUltimoFeedback instanceof AttackFeedback ||
            meuUltimoFeedback instanceof DefeatFeedback ||
            meuUltimoFeedback instanceof EqualStrengthFeedback ||
            meuUltimoFeedback instanceof LandmineFeedback ||
            meuUltimoFeedback instanceof LandMineDeactivationFeedback)) {
            attackCount++;
        }
    
        PieceAction melhorAcao = selecionarMelhorMovimento(tabuleiro);
        if (melhorAcao != null) {
            Piece attemptedPiece = melhorAcao.getPiece();
            if (lastMovedPieces.size() >= 3) {
                lastMovedPieces.removeLast();
            }
            lastMovedPieces.push(attemptedPiece);
            lastAttemptedPiece = attemptedPiece;
        } else {
            lastAttemptedPiece = null;
        }
        tabuleiroOcultoAnterior = new Board(tabuleiro);
        turnCount++;
        return melhorAcao;
    }
    /*Inicializa o estado de crença para as peças do oponente, atribuindo probabilidades iniciais iguais para 
    cada tipo de peça e resetando contadores de imobilidade.*/
    private void inicializarEstadoCrença(Board tabuleiro) {
        estadoCrença.clear();
        knownOpponentPieces.clear();
        contagemImobilidade.clear();
        revealedCounts.clear();
        for (QuantityPerPiece qpp : QuantityPerPiece.values()) {
            revealedCounts.put(qpp.getCode(), 0);
        }
        for (int i = 0; i < Board.ROWS; i++) {
            for (int j = 0; j < Board.COLS; j++) {
                Piece piece = tabuleiro.getPiece(i, j);
                if (piece != null && piece.getPlayer() != null && !piece.getPlayer().equals(nomeJogador)) {
                    String pos = posParaString(i, j);
                    Map<String, Double> likelihoods = new HashMap<>();
                    for (QuantityPerPiece qpp : QuantityPerPiece.values()) {
                        likelihoods.put(qpp.getCode(), 1.0);
                    }
                    estadoCrença.put(pos, likelihoods);
                    contagemImobilidade.put(pos, 0);
                }
            }
        }
    }
    /*Atualiza o estado de crença com base nos feedbacks recebidos após cada jogada, ajustando as
     probabilidades das peças do oponente e monitorando a imobilidade.*/
    private void atualizarEstadoCrençaComFeedback(Board tabuleiro, Feedback meuFeedback, Feedback feedbackInimigo) {
        
        if (feedbackInimigo != null) {
            if (feedbackInimigo instanceof MoveFeedback) {
                String fromPos = null;
                String toPos = null;
                for (int i = 0; i < Board.ROWS; i++) {
                    for (int j = 0; j < Board.COLS; j++) {
                        Piece prevPiece = tabuleiroOcultoAnterior.getPiece(i, j);
                        Piece currPiece = tabuleiro.getPiece(i, j);
                        if (prevPiece != null && prevPiece.getPlayer() == null && currPiece == null) {
                            fromPos = posParaString(i, j);
                        }
                        if (prevPiece == null && currPiece != null && currPiece.getPlayer() == null) {
                            toPos = posParaString(i, j);
                        }
                    }
                }
                if (fromPos != null && toPos != null && estadoCrença.containsKey(fromPos)) {
                    Map<String, Double> likelihoods = estadoCrença.remove(fromPos);
                    estadoCrença.put(toPos, likelihoods);
                    int fromX = parseX(fromPos);
                    int fromY = parseY(fromPos);
                    int toX = parseX(toPos);
                    int toY = parseY(toPos);
                    int dx = Math.abs(toX - fromX);
                    int dy = Math.abs(toY - fromY);
                    if ((dx > 1 && dy == 0) || (dx == 0 && dy > 1)) {
                        likelihoods.put("S", likelihoods.getOrDefault("S", 0.0) * IMPULSO_MOVIMENTO_LONGO);
                        likelihoods.put("ST", likelihoods.getOrDefault("ST", 0.0) * IMPULSO_MOVIMENTO_LONGO);
                    }
                }
            } else if (feedbackInimigo instanceof AttackFeedback) {
                AttackFeedback af = (AttackFeedback) feedbackInimigo;
                String type = af.attacker.getRepresentation();
                String toPos = posParaString(af.toX, af.toY);
                knownOpponentPieces.put(toPos, type);
                estadoCrença.remove(toPos);
                revealedCounts.put(type, revealedCounts.getOrDefault(type, 0) + 1);
            } else if (feedbackInimigo instanceof DefeatFeedback) {
                DefeatFeedback df = (DefeatFeedback) feedbackInimigo;
                String type = df.attacker.getRepresentation();
                String fromPos = posParaString(df.attacker.getPosX(), df.attacker.getPosY());
                estadoCrença.remove(fromPos);
                revealedCounts.put(type, revealedCounts.getOrDefault(type, 0) + 1);
            } else if (feedbackInimigo instanceof EqualStrengthFeedback) {
                EqualStrengthFeedback ef = (EqualStrengthFeedback) feedbackInimigo;
                String attackerType = ef.attacker.getRepresentation();
                String attackerPos = posParaString(ef.attacker.getPosX(), ef.attacker.getPosY());
                estadoCrença.remove(attackerPos);
                revealedCounts.put(attackerType, revealedCounts.getOrDefault(attackerType, 0) + 1);
            } else if (feedbackInimigo instanceof LandmineFeedback) {
                LandmineFeedback lf = (LandmineFeedback) feedbackInimigo;
                String victimType = lf.getVictim().getRepresentation();
                String victimPos = posParaString(lf.getVictim().getPosX(), lf.getVictim().getPosY());
                estadoCrença.remove(victimPos);
                revealedCounts.put(victimType, revealedCounts.getOrDefault(victimType, 0) + 1);
            } else if (feedbackInimigo instanceof LandMineDeactivationFeedback) {
                LandMineDeactivationFeedback ldf = (LandMineDeactivationFeedback) feedbackInimigo;
                String caboPos = posParaString(ldf.getPiece().getPosX(), ldf.getPiece().getPosY());
                knownOpponentPieces.put(caboPos, "C");
                estadoCrença.remove(caboPos);
                revealedCounts.put("C", revealedCounts.getOrDefault("C", 0) + 1);
            }
        }

        if (meuFeedback != null) {
            if (meuFeedback instanceof AttackFeedback) {
                AttackFeedback af = (AttackFeedback) meuFeedback;
                String defenderType = af.defender.getRepresentation();
                String defenderPos = posParaString(af.toX, af.toY);
                estadoCrença.remove(defenderPos);
                revealedCounts.put(defenderType, revealedCounts.getOrDefault(defenderType, 0) + 1);
            } else if (meuFeedback instanceof DefeatFeedback) {
                DefeatFeedback df = (DefeatFeedback) meuFeedback;
                String defenderPos = posParaString(df.toX, df.toY);
                if (estadoCrença.containsKey(defenderPos)) {
                    Map<String, Double> likelihoods = estadoCrença.get(defenderPos);
                    String attackerType = df.attacker.getRepresentation();
                    for (String type : new ArrayList<>(likelihoods.keySet())) {
                        if (compararPatentes(type, attackerType) <= 0) {
                            likelihoods.put(type, 0.0);
                        }
                    }
                }
            } else if (meuFeedback instanceof EqualStrengthFeedback) {
                EqualStrengthFeedback ef = (EqualStrengthFeedback) meuFeedback;
                String defenderType = ef.defender.getRepresentation();
                String defenderPos = posParaString(ef.defender.getPosX(), ef.defender.getPosY());
                estadoCrença.remove(defenderPos);
                revealedCounts.put(defenderType, revealedCounts.getOrDefault(defenderType, 0) + 1);
            } else if (meuFeedback instanceof LandmineFeedback) {
                LandmineFeedback lf = (LandmineFeedback) meuFeedback;
                String landminePos = posParaString(lf.getPiece().getPosX(), lf.getPiece().getPosY());
                knownOpponentPieces.put(landminePos, "M");
                estadoCrença.remove(landminePos);
            } else if (meuFeedback instanceof LandMineDeactivationFeedback) {
                LandMineDeactivationFeedback ldf = (LandMineDeactivationFeedback) meuFeedback;
                String landminePos = posParaString(ldf.getPiece().getPosX(), ldf.getPiece().getPosY());
                revealedCounts.put("M", revealedCounts.getOrDefault("M", 0) + 1);
                estadoCrença.remove(landminePos);
            }
        }

        atualizarImobilidade(tabuleiro);
    }

    private void atualizarImobilidade(Board tabuleiro) {
        for (String pos : new ArrayList<>(estadoCrença.keySet())) {
            int x = parseX(pos);
            int y = parseY(pos);
            Piece prevPiece = tabuleiroOcultoAnterior.getPiece(x, y);
            Piece currPiece = tabuleiro.getPiece(x, y);
            if (prevPiece != null && currPiece != null && prevPiece.getPlayer() == null && currPiece.getPlayer() == null) {
                contagemImobilidade.put(pos, contagemImobilidade.getOrDefault(pos, 0) + 1);
                if (contagemImobilidade.get(pos) > 3) {
                    Map<String, Double> likelihoods = estadoCrença.get(pos);
                    likelihoods.put("PS", likelihoods.getOrDefault("PS", 0.0) * IMPULSO_IMOBILIDADE);
                    likelihoods.put("M", likelihoods.getOrDefault("M", 0.0) * IMPULSO_IMOBILIDADE);
                }
            } else {
                contagemImobilidade.put(pos, 0);
            }
        }
    } /*Gera todos os movimentos possíveis, prioriza ataques no início e no final do jogo,
     e seleciona o movimento com a melhor pontuação com base em uma avaliação heurística.*/
    private PieceAction selecionarMelhorMovimento(Board tabuleiro) {   
        List<PieceAction> movimentosPossiveis = new ArrayList<>();
        List<PieceAction> attackMoves = new ArrayList<>();
        int[][] direcoes = {{1, 0}, {-1, 0}, {0, -1}, {0, 1}};

        for (int i = 0; i < Board.ROWS; i++) {
            for (int j = 0; j < Board.COLS; j++) {
                Piece peca = tabuleiro.getPiece(i, j);
                if (peca != null && peca.getPlayer() != null && peca.getPlayer().equals(nomeJogador) && ehMovivel(peca)) {
                    boolean canMove = lastMovedPieces.size() < 3 || !lastMovedPieces.stream().allMatch(p -> p.equals(peca));
                    if (canMove) {
                        for (int[] dir : direcoes) {
                            if (peca.getRepresentation().equals("S")) {
                                int adjX = i + dir[0];
                                int adjY = j + dir[1];
                                if (Board.isValidPosition(adjX, adjY)) {
                                    Piece target = tabuleiro.getPiece(adjX, adjY);
                                    if (target == null) {
                                        movimentosPossiveis.add(new PieceAction(peca, adjX, adjY));
                                    } else if (target.getPlayer() == null) {
                                        attackMoves.add(new PieceAction(peca, adjX, adjY));
                                    }
                                }
                                int d = 2;
                                while (true) {
                                    int novoX = i + dir[0] * d;
                                    int novoY = j + dir[1] * d;
                                    if (!Board.isValidPosition(novoX, novoY)) break;
                                    Piece target = tabuleiro.getPiece(novoX, novoY);
                                    if (target != null) break;
                                    boolean pathClear = true;
                                    for (int k = 1; k < d; k++) {
                                        int interX = i + dir[0] * k;
                                        int interY = j + dir[1] * k;
                                        if (tabuleiro.getPiece(interX, interY) != null) {
                                            pathClear = false;
                                            break;
                                        }
                                    }
                                    if (pathClear) {
                                        movimentosPossiveis.add(new PieceAction(peca, novoX, novoY));
                                    } else {
                                        break;
                                    }
                                    d++;
                                }
                            } else {
                                int novoX = i + dir[0];
                                int novoY = j + dir[1];
                                if (Board.isValidPosition(novoX, novoY)) {
                                    Piece target = tabuleiro.getPiece(novoX, novoY);
                                    if (target == null) {
                                        movimentosPossiveis.add(new PieceAction(peca, novoX, novoY));
                                    } else if (target.getPlayer() == null) {
                                        attackMoves.add(new PieceAction(peca, novoX, novoY));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        
        double lateGameFactor = (double) turnCount / MAX_TURNS;
        if (lateGameFactor > 0.8 && !attackMoves.isEmpty()) { 
            return attackMoves.get(new Random().nextInt(attackMoves.size()));
        } else if (attackCount < 8 && !attackMoves.isEmpty()) {
            return attackMoves.get(new Random().nextInt(attackMoves.size()));
        }

        if (movimentosPossiveis.isEmpty() && attackMoves.isEmpty()) {
            for (int i = 0; i < Board.ROWS; i++) {
                for (int j = 0; j < Board.COLS; j++) {
                    Piece peca = tabuleiro.getPiece(i, j);
                    if (peca != null && peca.getPlayer() != null && peca.getPlayer().equals(nomeJogador) && ehMovivel(peca)) {
                        boolean canMove = lastMovedPieces.size() < 3 || !lastMovedPieces.stream().allMatch(p -> p.equals(peca));
                        if (canMove) {
                            for (int[] dir : direcoes) {
                                int novoX = i + dir[0];
                                int novoY = j + dir[1];
                                if (Board.isValidPosition(novoX, novoY) && tabuleiro.getPiece(novoX, novoY) == null) {
                                    return new PieceAction(peca, novoX, novoY);
                                }
                            }
                        }
                    }
                }
            }
            System.out.println("Warning: No valid moves available for " + nomeJogador);
            return null;
        }

        movimentosPossiveis.addAll(attackMoves);
        PieceAction melhorAcao = null;
        double melhorPontuacao = Double.NEGATIVE_INFINITY;
        for (PieceAction acao : movimentosPossiveis) {
            double pontuacao = avaliarMovimento(acao, tabuleiro);
            if (pontuacao > melhorPontuacao) {
                melhorPontuacao = pontuacao;
                melhorAcao = acao;
            }
        }
        return melhorAcao != null ? melhorAcao : movimentosPossiveis.get(0);
    }



    private boolean isAdjacentToEnemy(int x, int y, Board tabuleiro) {
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}; 
        for (int[] dir : dirs) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            if (Board.isValidPosition(nx, ny)) {
                Piece p = tabuleiro.getPiece(nx, ny);
                if (p != null && p.getPlayer() != null && !p.getPlayer().equals(nomeJogador)) {
                    return true;
                }
            }
        }
        return false;
    }

 
    private double distanceToNearestEnemy(int x, int y, Board tabuleiro) {
        double minDist = Double.MAX_VALUE;
        for (int i = 0; i < Board.ROWS; i++) {
            for (int j = 0; j < Board.COLS; j++) {
                Piece p = tabuleiro.getPiece(i, j);
                if (p != null && p.getPlayer() != null && !p.getPlayer().equals(nomeJogador)) {
                    double dist = Math.sqrt(Math.pow(i - x, 2) + Math.pow(j - y, 2));
                    if (dist < minDist) minDist = dist;
                }
            }
        }
        return minDist;
    }


    private int getTotalRevealedEnemyPieces() {
        int total = 0;
        for (int count : revealedCounts.values()) {
            total += count;
        }
        return total;
    }
    /*Avalia a qualidade de um movimento considerando progresso no tabuleiro, proximidade ao inimigo, 
    probabilidades de vitória em ataques e bônus para o final do jogo.*/
    private double avaliarMovimento(PieceAction acao, Board tabuleiro) {
        Piece peca = acao.getPiece();
        int novoX = acao.getNewPosX();
        int novoY = acao.getNewPosY();
        String minhaPatente = peca.getRepresentation();
        Piece alvo = tabuleiro.getPiece(novoX, novoY);
        double pontuacao = 0.0;

        
        double lateGameFactor = (double) turnCount / MAX_TURNS;

        if (alvo == null) {
            int progresso = nomeJogador.equals(tabuleiro.player1.getPlayerName()) ? novoX : (9 - novoX);
            pontuacao += 5.0 * progresso;
            double currentDist = distanceToNearestEnemy(peca.getPosX(), peca.getPosY(), tabuleiro);
            double newDist = distanceToNearestEnemy(novoX, novoY, tabuleiro);
            if (newDist < currentDist) {
                double distReduction = currentDist - newDist;
                pontuacao += 10.0 * distReduction;
            }
            if (minhaPatente.equals("S") || minhaPatente.equals("ST")) {
                int distancia = Math.abs(novoX - peca.getPosX());
                pontuacao += 0.2 * distancia;
            }
            if (isAdjacentToEnemy(novoX, novoY, tabuleiro)) {
                pontuacao += 50.0 + (lateGameFactor * 50.0); 
            }
        } else if (alvo.getPlayer() == null) { 
            String pos = posParaString(novoX, novoY);
            if (knownOpponentPieces.containsKey(pos)) {
                String type = knownOpponentPieces.get(pos);
                int comparacao = compararPatentes(minhaPatente, type);
                if (comparacao > 0) pontuacao += 50.0;
                else if (comparacao < 0) pontuacao = -5.0;
                else pontuacao = -2.0;
            } else if (estadoCrença.containsKey(pos)) {
                double probVitoria = 0.0, probDerrota = 0.0, probEmpate = 0.0;
                for (QuantityPerPiece qpp : QuantityPerPiece.values()) {
                    String type = qpp.getCode();
                    double p = getProbability(pos, type);
                    int comparacao = compararPatentes(minhaPatente, type);
                    if (comparacao > 0) probVitoria += p;
                    else if (comparacao < 0) probDerrota += p;
                    else probEmpate += p;
                }
                double probPrisioneiro = getProbability(pos, "PS");
                double immobileTurns = getImmobileTurns(pos);
                double immobileBonus = (immobileTurns > 3) ? 8.0 : 0.0;
                pontuacao = 50 * probPrisioneiro + 100 * probVitoria - 0.5 * probDerrota - 0 * probEmpate + immobileBonus;
                double explorationBonus = (minhaPatente.equals("S") || minhaPatente.equals("ST")) ? 10.0 : 5.0;
                pontuacao += explorationBonus;
                pontuacao += 40.0;
                int totalRevealed = getTotalRevealedEnemyPieces();
                if (totalRevealed < 10) {
                    double aggressionBonus = (10 - totalRevealed) * 10.0;
                    pontuacao += aggressionBonus;
                }
                
                pontuacao += lateGameFactor * 100.0; 
            }
        }

        return pontuacao;
    }

    private double getProbability(String pos, String type) {
        if (knownOpponentPieces.containsKey(pos)) {
            return knownOpponentPieces.get(pos).equals(type) ? 1.0 : 0.0;
        } else if (estadoCrença.containsKey(pos)) {
            Map<String, Double> likelihoods = estadoCrença.get(pos);
            int remainingCount = getRemainingCount(type);
            double likelihood = likelihoods.getOrDefault(type, 0.0);
            double sum = 0.0;
            for (String t : likelihoods.keySet()) {
                sum += likelihoods.get(t) * getRemainingCount(t);
            }
            return sum > 0 ? (likelihood * remainingCount) / sum : 0.0;
        }
        return 0.0;
    }

    private int getRemainingCount(String type) {
        int initialCount = getInitialQuantity(type);
        int revealed = revealedCounts.getOrDefault(type, 0);
        return Math.max(initialCount - revealed, 0);
    }

    private int getInitialQuantity(String code) {
        for (QuantityPerPiece qpp : QuantityPerPiece.values()) {
            if (qpp.getCode().equals(code)) {
                return qpp.getQuantity();
            }
        }
        throw new IllegalArgumentException("Unknown piece code: " + code);
    }

    private int compararPatentes(String minhaPatente, String patenteOponente) {
        String meuValorStr = mapToNumeric(minhaPatente);
        String valorOponenteStr = mapToNumeric(patenteOponente);

        if (valorOponenteStr.equals("PS")) return 1;
        if (valorOponenteStr.equals("M")) return meuValorStr.equals("7") ? 1 : -1;
        if (meuValorStr.equals("S") && valorOponenteStr.equals("1")) return 1;
        if (valorOponenteStr.equals("S") && meuValorStr.equals("1")) return -1;
        if (meuValorStr.equals("S") || valorOponenteStr.equals("S")) return Integer.compare(valorOponenteStr.charAt(0), meuValorStr.charAt(0));

        int meuValor = Integer.parseInt(meuValorStr);
        int valorOponente = Integer.parseInt(valorOponenteStr);
        return Integer.compare(valorOponente, meuValor);
    }

    private String mapToNumeric(String patente) {
        switch (patente) {
            case "AS": return "S";
            case "M": return "M";
            case "PS": return "PS";
            case "G": return "1";
            case "CR": return "2";
            case "MJ": return "3";
            case "CP": return "4";
            case "T": return "5";
            case "SG": return "6";
            case "C": return "7";
            case "ST": return "8";
            case "S": return "9";
            default: throw new IllegalArgumentException("Unknown piece code: " + patente);
        }
    }

    private String posParaString(int x, int y) {
        return x + "," + y;
    }

    private int parseX(String pos) {
        return Integer.parseInt(pos.split(",")[0]);
    }

    private int parseY(String pos) {
        return Integer.parseInt(pos.split(",")[1]);
    }

    private int getImmobileTurns(String pos) {
        return contagemImobilidade.getOrDefault(pos, 0);
    }

    private boolean ehMovivel(Piece peca) {
        String rep = peca.getRepresentation();
        return !rep.equals("M") && !rep.equals("PS");
    }
}