package game.players;

import game.Board;
import game.feedbacks.Feedback;
import game.pieces.PieceAction;
import game.pieces.PieceFactory;
import game.pieces.QuantityPerPiece;
import game.pieces.Piece;

import java.util.*;

public class PauloSilvestrePlayer implements Player {
    private final String nomeJogador;
    private final Random aleatorio;

    public PauloSilvestrePlayer(String nomeJogador) {
        this.nomeJogador = nomeJogador;
        this.aleatorio = new Random();
    }

    @Override
    public String getPlayerName() {
        return this.nomeJogador;
    }

    @Override
    public Piece[][] setup(Board tabuleiro) {
        var resultado = new Piece[4][10];
        List<String> representacoesPecas = new ArrayList<>();

        for (QuantityPerPiece peca : QuantityPerPiece.values()) {
            int quantidade = peca.getQuantity();
            for (int i = 0; i < quantidade; i++) {
                representacoesPecas.add(peca.getCode());
            }
        }

        Collections.shuffle(representacoesPecas);
        int indice = 0;

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 10; j++) {
                String codigoPeca = representacoesPecas.get(indice);
                resultado[i][j] = PieceFactory.createPiece(codigoPeca, this.nomeJogador, tabuleiro);
                indice++;
            }
        }
        return resultado;
    }

    @Override
    public PieceAction play(Board tabuleiro, Feedback meuUltimoFeedback, Feedback ultimoFeedbackInimigo) {
        int direcaoFrente = nomeJogador.equals("Player1") ? 1 : -1;
        List<AcaoPonderada> acoesPossiveis = new ArrayList<>();

        for (int i = 0; i < Board.ROWS; i++) {
            for (int j = 0; j < Board.COLS; j++) {
                Piece peca = tabuleiro.getPiece(i, j);
                if (peca != null && nomeJogador.equals(peca.getPlayer())) {
                    avaliarMovimentosPossiveis(peca, tabuleiro, direcaoFrente, acoesPossiveis);
                }
            }
        }

        if (acoesPossiveis.isEmpty()) {
            return null;
        }

        normalizarPesos(acoesPossiveis);
        return selecionarAcaoPorProbabilidade(acoesPossiveis);
    }

    private void avaliarMovimentosPossiveis(Piece peca, Board tabuleiro, int direcaoFrente,
                                            List<AcaoPonderada> acoesPossiveis) {
        int x = peca.getPosX();
        int y = peca.getPosY();

        avaliarMovimento(x + direcaoFrente, y, peca, tabuleiro, acoesPossiveis, 3.0);

        avaliarMovimento(x, y - 1, peca, tabuleiro, acoesPossiveis, 2.0); // Esquerda
        avaliarMovimento(x, y + 1, peca, tabuleiro, acoesPossiveis, 2.0); // Direita

        avaliarMovimento(x - direcaoFrente, y, peca, tabuleiro, acoesPossiveis, 0.5);
    }

    private void avaliarMovimento(int novoX, int novoY, Piece peca, Board tabuleiro,
                                  List<AcaoPonderada> acoesPossiveis, double pesoBase) {
        if (!Board.isValidPosition(novoX, novoY)) {
            return;
        }

        Piece pecaAlvo = tabuleiro.getPiece(novoX, novoY);
        if (pecaAlvo == null || pecaAlvo.getPlayer() == null || !nomeJogador.equals(pecaAlvo.getPlayer())) {
            double peso = pesoBase;

            peso *= calcularFatorSegurancaPosicao(novoX, novoY, tabuleiro);
            peso *= calcularFatorProximidadeInimigo(novoX, novoY, tabuleiro);

            acoesPossiveis.add(new AcaoPonderada(
                    new PieceAction(peca, novoX, novoY),
                    peso
            ));
        }
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
                    if (vizinho != null && nomeJogador.equals(vizinho.getPlayer()) == false) {
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
                if (p != null && nomeJogador.equals(p.getPlayer()) == false) {
                    int distancia = Math.abs(x - i) + Math.abs(y - j);
                    if (distancia < distanciaInimigoMaisProximo) {
                        distanciaInimigoMaisProximo = distancia;
                    }
                }
            }
        }

        return distanciaInimigoMaisProximo < 3 ? 1.5 : 1.0;
    }

    private void normalizarPesos(List<AcaoPonderada> acoes) {
        double pesoTotal = acoes.stream().mapToDouble(a -> a.peso).sum();
        if (pesoTotal > 0) {
            for (AcaoPonderada acao : acoes) {
                acao.peso /= pesoTotal;
            }
        }
    }

    private PieceAction selecionarAcaoPorProbabilidade(List<AcaoPonderada> acoes) {
        double valorAleatorio = aleatorio.nextDouble();
        double probabilidadeAcumulada = 0.0;

        for (AcaoPonderada acao : acoes) {
            probabilidadeAcumulada += acao.peso;
            if (valorAleatorio <= probabilidadeAcumulada) {
                return acao.acao;
            }
        }

        return acoes.get(acoes.size() - 1).acao;
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