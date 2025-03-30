package game.players;

import game.Board;
import game.feedbacks.*;
import game.pieces.PieceAction;
import game.pieces.PieceFactory;
import game.pieces.QuantityPerPiece;
import game.pieces.Piece;

import java.util.*;
import static java.util.Map.entry;

public class PauloSilvestrePlayer implements Player {
    private String nomeJogador = "PauloSilvestre";
    private final Deque<Piece> pecasMovidasRecentemente;
    private static final int MAX_MOVIMENTOS_CONSECUTIVOS = 2;
    private static final Set<String> POSICOES_AGUA = Set.of("E3", "E4", "E7", "E8", "F3", "F4", "F7", "F8");
    private final Random aleatorio;
    private List<Feedback> historicoCombates = new ArrayList<>();
    private int rodadasDesdeAtaque = 0;
    
    private static final Map<String, Integer> HIERARQUIA_PECAS = Map.ofEntries(
        entry("G", 9),   // General
        entry("CR", 8),  // Coronel
        entry("MJ", 7),  // Major
        entry("CP", 6),  // Capitão
        entry("T", 5),   // Tenente
        entry("SG", 4),  // Sargento
        entry("ST", 3),  // Subtenente
        entry("C", 2),   // Cabo
        entry("S", 1),   // Soldado
        entry("PS", 0),  // Prisioneiro
        entry("OP", -1)  // Inimigo desconhecido
    );
    
    public PauloSilvestrePlayer() {
        this.pecasMovidasRecentemente = new ArrayDeque<>(MAX_MOVIMENTOS_CONSECUTIVOS);
        this.aleatorio = new Random();
    }

    @Override
    public String getPlayerName() {
        return this.nomeJogador;
    }

    @Override
    public Piece[][] setup(Board tabuleiro) {
        Piece[][] resultado = new Piece[4][10];
        List<String> pecasParaDistribuir = new ArrayList<>();

        for (QuantityPerPiece peca : QuantityPerPiece.values()) {
            if (!peca.getCode().equals("PS")) {
                for (int i = 0; i < peca.getQuantity(); i++) {
                    pecasParaDistribuir.add(peca.getCode());
                }
            }
        }

        int linhaPS = nomeJogador.equals(tabuleiro.player1.getPlayerName()) ? 0 : 3;
        int colunaPS = aleatorio.nextInt(10);
        resultado[linhaPS][colunaPS] = PieceFactory.createPiece("PS", this.nomeJogador, tabuleiro);

        Collections.shuffle(pecasParaDistribuir);
        int index = 0;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 10; j++) {
                if (i == linhaPS && j == colunaPS) continue;
                if (index < pecasParaDistribuir.size()) {
                    resultado[i][j] = PieceFactory.createPiece(pecasParaDistribuir.get(index++), this.nomeJogador, tabuleiro);
                }
            }
        }

        return resultado;
    }

    @Override
    public PieceAction play(Board tabuleiro, Feedback meuUltimoFeedback, Feedback ultimoFeedbackInimigo) {
        rodadasDesdeAtaque++;
        processarFeedbacks(meuUltimoFeedback, ultimoFeedbackInimigo);

        
        PieceAction acaoAtaque = encontrarAtaqueEstrategico(tabuleiro);
        if (acaoAtaque != null && podeMover(acaoAtaque.getPiece())) {
            rodadasDesdeAtaque = 0;
            registrarMovimento(acaoAtaque.getPiece());
            return acaoAtaque;
        }

        if (rodadasDesdeAtaque > 2) {
            PieceAction ataqueOportuno = encontrarAtaqueOportuno(tabuleiro);
            if (ataqueOportuno != null && podeMover(ataqueOportuno.getPiece())) {
                rodadasDesdeAtaque = 0;
                registrarMovimento(ataqueOportuno.getPiece());
                return ataqueOportuno;
            }
        }

        int direcaoFrente = nomeJogador.equals(tabuleiro.player1.getPlayerName()) ? 1 : -1;
        List<AcaoPonderada> acoesPossiveis = new ArrayList<>();

        for (int i = 0; i < Board.ROWS; i++) {
            for (int j = 0; j < Board.COLS; j++) {
                Piece peca = tabuleiro.getPiece(i, j);
                if (pecaValidaParaMovimento(peca)) {
                    avaliarMovimentosAgressivos(peca, tabuleiro, direcaoFrente, acoesPossiveis);
                }
            }
        }

        if (!acoesPossiveis.isEmpty()) {
            acoesPossiveis.sort((a1, a2) -> Double.compare(a2.peso, a1.peso));
            
            for (AcaoPonderada acaoPonderada : acoesPossiveis) {
                if (podeMover(acaoPonderada.acao.getPiece())) {
                    registrarMovimento(acaoPonderada.acao.getPiece());
                    return acaoPonderada.acao;
                }
            }
        }
        
        return null;
    }

    private PieceAction encontrarAtaqueEstrategico(Board tabuleiro) {
        List<PieceAction> ataquesComVantagem = new ArrayList<>();
        List<PieceAction> outrosAtaques = new ArrayList<>();

        for (int i = 0; i < Board.ROWS; i++) {
            for (int j = 0; j < Board.COLS; j++) {
                Piece peca = tabuleiro.getPiece(i, j);
                if (pecaValidaParaMovimento(peca)) {
                    for (int[] dir : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                        int novoX = i + dir[0];
                        int novoY = j + dir[1];
                        if (posicaoValida(novoX, novoY)) {
                            Piece alvo = tabuleiro.getPiece(novoX, novoY);
                            if (alvo != null && !nomeJogador.equals(alvo.getPlayer())) {
                                // Verifica se temos vantagem de força ou se é um ataque conhecido
                                if (podeAtacarComVantagem(peca, alvo) || 
                                    ehAlvoConhecido(alvo.getRepresentation())) {
                                    ataquesComVantagem.add(new PieceAction(peca, novoX, novoY));
                                } else {
                                    outrosAtaques.add(new PieceAction(peca, novoX, novoY));
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (!ataquesComVantagem.isEmpty()) {
            return ataquesComVantagem.get(aleatorio.nextInt(ataquesComVantagem.size()));
        }
        
        return outrosAtaques.isEmpty() ? null : 
               outrosAtaques.get(aleatorio.nextInt(outrosAtaques.size()));
    }

    private boolean podeAtacarComVantagem(Piece minhaPeca, Piece pecaInimiga) {
        Integer minhaForca = HIERARQUIA_PECAS.get(minhaPeca.getRepresentation());
        Integer forcaInimiga = HIERARQUIA_PECAS.get(pecaInimiga.getRepresentation());
        
        return minhaForca != null && forcaInimiga != null && 
               (minhaForca > forcaInimiga || pecaInimiga.getRepresentation().equals("OP"));
    }

    private boolean ehAlvoConhecido(String representacao) {
        for (Feedback fb : historicoCombates) {
            if (fb instanceof AttackFeedback) {
                AttackFeedback ataque = (AttackFeedback) fb;
                if (ataque.attacker.getRepresentation().equals(representacao) || 
                    ataque.defender.getRepresentation().equals(representacao)) {
                    return true;
                }
            }
        }
        return false;
    }

    private PieceAction encontrarAtaqueOportuno(Board tabuleiro) {
        List<PieceAction> ataquesPossiveis = new ArrayList<>();
        
        for (int i = 0; i < Board.ROWS; i++) {
            for (int j = 0; j < Board.COLS; j++) {
                Piece peca = tabuleiro.getPiece(i, j);
                if (pecaValidaParaMovimento(peca)) {
                    for (int[] dir : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                        int novoX = i + dir[0];
                        int novoY = j + dir[1];
                        if (posicaoValida(novoX, novoY)) {
                            Piece alvo = tabuleiro.getPiece(novoX, novoY);
                            if (alvo != null && !nomeJogador.equals(alvo.getPlayer())) {
                                ataquesPossiveis.add(new PieceAction(peca, novoX, novoY));
                            }
                        }
                    }
                }
            }
        }
        
        return ataquesPossiveis.isEmpty() ? null : 
               ataquesPossiveis.get(aleatorio.nextInt(ataquesPossiveis.size()));
    }

    private void avaliarMovimentosAgressivos(Piece peca, Board tabuleiro, int direcaoFrente,
                                           List<AcaoPonderada> acoesPossiveis) {
        int x = peca.getPosX();
        int y = peca.getPosY();

        avaliarMovimento(x + direcaoFrente, y, peca, tabuleiro, acoesPossiveis, 5.0);
        avaliarMovimento(x, y + 1, peca, tabuleiro, acoesPossiveis, 3.0);
        avaliarMovimento(x, y - 1, peca, tabuleiro, acoesPossiveis, 3.0);
        avaliarMovimento(x - direcaoFrente, y, peca, tabuleiro, acoesPossiveis, 1.0);
    }

    private void avaliarMovimento(int novoX, int novoY, Piece peca, Board tabuleiro,
                                List<AcaoPonderada> acoesPossiveis, double pesoBase) {
        if (!posicaoValida(novoX, novoY)) return;

        Piece pecaAlvo = tabuleiro.getPiece(novoX, novoY);
        if (pecaAlvo == null || !nomeJogador.equals(pecaAlvo.getPlayer())) {
            double peso = pesoBase;
            peso *= calcularFatorSegurancaPosicao(novoX, novoY, tabuleiro);
            peso *= calcularFatorProximidadeInimigo(novoX, novoY, tabuleiro);
            acoesPossiveis.add(new AcaoPonderada(new PieceAction(peca, novoX, novoY), peso));
        }
    }

    private boolean pecaValidaParaMovimento(Piece peca) {
        return peca != null && 
               nomeJogador.equals(peca.getPlayer()) &&
               !peca.getRepresentation().equals("M") && 
               !peca.getRepresentation().equals("PS");
    }

    private boolean podeMover(Piece peca) {
        int contagem = 0;
        for (Piece p : pecasMovidasRecentemente) {
            if (p.equals(peca)) {
                contagem++;
            }
        }
        return contagem < MAX_MOVIMENTOS_CONSECUTIVOS;
    }

    private void processarFeedbacks(Feedback meuFeedback, Feedback feedbackInimigo) {
        if (meuFeedback instanceof AttackFeedback) {
            historicoCombates.add(meuFeedback);
        }
        if (feedbackInimigo instanceof AttackFeedback) {
            historicoCombates.add(feedbackInimigo);
        }
    }

    private boolean posicaoValida(int x, int y) {
        return Board.isValidPosition(x, y) && !isAgua(x, y);
    }

    private double calcularFatorSegurancaPosicao(int x, int y, Board tabuleiro) {
        int contagemInimigos = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                int nx = x + dx;
                int ny = y + dy;
                if (Board.isValidPosition(nx, ny)) {
                    Piece vizinho = tabuleiro.getPiece(nx, ny);
                    if (vizinho != null && !nomeJogador.equals(vizinho.getPlayer())) {
                        contagemInimigos++;
                    }
                }
            }
        }
        return 1.0 / (1.0 + contagemInimigos * 0.3);
    }

    private double calcularFatorProximidadeInimigo(int x, int y, Board tabuleiro) {
        int distanciaInimigoMaisProximo = Integer.MAX_VALUE;
        for (int i = 0; i < Board.ROWS; i++) {
            for (int j = 0; j < Board.COLS; j++) {
                Piece p = tabuleiro.getPiece(i, j);
                if (p != null && !nomeJogador.equals(p.getPlayer())) {
                    int distancia = Math.abs(x - i) + Math.abs(y - j);
                    if (distancia < distanciaInimigoMaisProximo) {
                        distanciaInimigoMaisProximo = distancia;
                    }
                }
            }
        }
        return distanciaInimigoMaisProximo < 3 ? 1.5 : 1.0;
    }

    private void registrarMovimento(Piece pecaMovida) {
        if (!pecasMovidasRecentemente.isEmpty() && !pecasMovidasRecentemente.peekFirst().equals(pecaMovida)) {
            pecasMovidasRecentemente.clear();
        }
        pecasMovidasRecentemente.addFirst(pecaMovida);
        while (pecasMovidasRecentemente.size() > MAX_MOVIMENTOS_CONSECUTIVOS) {
            pecasMovidasRecentemente.removeLast();
        }
    }

    private boolean isAgua(int x, int y) {
        char coluna = (char) ('A' + y);
        int linha = x + 1;
        String posicao = coluna + "" + linha;
        return POSICOES_AGUA.contains(posicao);
    }

    private static class AcaoPonderada {
        PieceAction acao;
        double peso;

        AcaoPonderada(PieceAction acao, double peso) {
            this.acao = acao;
            this.peso = peso;
        }
    }
}