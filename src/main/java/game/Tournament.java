package game;

import game.players.Player;

import java.io.File;
import java.math.BigInteger;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

public class Tournament {
    private final List<Map.Entry<String, Player>> agents = new ArrayList<>();
    private final Map<String, PlayerStats> playerStats = new ConcurrentHashMap<>();
    private final Semaphore semaphore = new Semaphore(100); // Limitar o número de partidas simultâneas

    private static class PlayerStats {
        int totalPartidas;
        int vitorias;
        int empates;
        int derrotas;
        int pontosTotais;

        public PlayerStats() {
            this.totalPartidas = 0;
            this.vitorias = 0;
            this.empates = 0;
            this.derrotas = 0;
            this.pontosTotais = 0;
        }
    }

    private void loadAgents() {
        String packagePath = "players";

        try {
            URL url = Thread.currentThread().getContextClassLoader().getResource(packagePath);
            if (url == null) {
                System.out.println("Pacote não encontrado: " + packagePath);
                return;
            }

            File directory = new File(url.toURI());
            if (directory.exists() && directory.isDirectory()) {
                for (File file : Objects.requireNonNull(directory.listFiles())) {
                    String fileName = file.getName();
                    if (fileName.endsWith(".class")) {
                        String className = packagePath.replace("/", ".") + "." + fileName.substring(0, fileName.length() - 6);
                        Class<?> clazz = Class.forName(className);
                        Player agent = (Player) clazz.getDeclaredConstructor().newInstance();
                        agents.add(new AbstractMap.SimpleEntry<>(clazz.getSimpleName(), agent));
                        playerStats.put(clazz.getSimpleName(), new PlayerStats());
                    }
                }
            } else {
                System.out.println("O diretório não existe ou não é válido: " + directory.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void execute(int numeroDePartidasParaCadaPar) {
        this.printHeader();
        this.loadingEffect(5);

        this.loadAgents();

        if (agents.size() < 2) {
            System.out.println("É necessário pelo menos dois jogadores para criar o torneio.");
            return;
        }

        System.out.println("Iniciando o Torneio... Vamos ver quem levará o título!\n");
        long startTime = System.nanoTime();

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < agents.size(); i++) {
            for (int j = i + 1; j < agents.size(); j++) {
                var item1 = agents.get(i);
                var item2 = agents.get(j);
//                System.out.printf("Partidas entre %s e %s%n", item1.getKey(), item2.getKey());

                for (int partida = 1; partida <= numeroDePartidasParaCadaPar / 2; partida++) {
                    futures.add(executor.submit(() -> {
                        try {
                            this.play(item1, item2);
                            this.play(item2, item1);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }));
                }
            }
        }

        // Aguardar todas as tarefas terminarem
        try {
            for (Future<?> future : futures) {
                future.get(); // Espera até que a tarefa termine
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }

        long endTime = System.nanoTime();
        long duration = endTime - startTime; // em nanossegundos

        System.out.println("\nTorneio finalizado! O vencedor será revelado a seguir.\n");
        loadingEffect(5);
        showWinner();

        System.out.println("\nA seguir a classificação final:\n");
        loadingEffect(5);
        printFinalRanking();

        var n = agents.size();
        int k = 2;
        var combinacoes = fatorial(n).divide((fatorial(k).multiply(fatorial(n - k))));


        System.out.println("\nTotal de jogadores: " + this.agents.size());
        System.out.println("Total de partidas para cada par: " + numeroDePartidasParaCadaPar);
        System.out.println("Total de partidas Jogadas: " + combinacoes.multiply(BigInteger.valueOf(numeroDePartidasParaCadaPar)));
        System.out.printf("Duração total do torneio: %d segundos\n", TimeUnit.NANOSECONDS.toSeconds(duration));

        System.exit(0);
    }

    private void printHeader() {
        String header = "\n" +
                "===============================================================\n" +
                "          TTTTT  OOO  RRRR   N   N  EEEEE  III  OOO \n" +
                "            T   O   O R   R  NN  N  E       I   O   O\n" +
                "            T   O   O RRRR   N N N  EEEE    I   O   O\n" +
                "            T   O   O R  R   N  NN  E       I   O   O\n" +
                "            T    OOO  R   R  N   N  EEEEE  III   OOO \n" +
                "               Edição Especial: Lua de Mel\n" +
                "===============================================================\n";
        System.out.println(header);
    }

    private void loadingEffect(int loop) {
        String[] loading = {"", ".", "..", "..."};
        for (int i = 0; i < loop; i++) {
            for (int j = 0; j < 4; j++) {
                try {
                    Thread.sleep(500);
                    System.out.print("Carregando" + loading[j] + "\r");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        System.out.println();
    }

    private void showWinner() {
        String winner = playerStats.entrySet().stream()
                .max(Comparator.comparingInt(entry -> entry.getValue().pontosTotais))
                .map(Map.Entry::getKey)
                .orElse("Nenhum vencedor");

        System.out.println("\n==================== Vencedor(es) do Torneio ====================");
        System.out.println("🏆 " + formatarNome(winner).replace("(Busca)", "").replace("(Modelo)", "") + " 🏆");
        System.out.println("Parabéns!!!\n");
    }

    private void printFinalRanking() {
        System.out.println("\n======================================== Classificação Final ========================================");

        System.out.printf("%-40s\t%-8s\t%-9s\t%-8s\t%-9s\t%-5s%n",
                "Jogador(es)", "Partidas", "Vitórias", "Empates", "Derrotas", "Pontos");

        playerStats.entrySet().stream()
                .sorted((entry1, entry2) -> Integer.compare(entry2.getValue().pontosTotais, entry1.getValue().pontosTotais))
                .forEach(entry -> {
                    PlayerStats stats = entry.getValue();
                    String nomeFormatado = formatarNome(entry.getKey());
                    System.out.printf("%-40s\t%-8d\t%-9d\t%-8d\t%-9d\t%-5d%n",
                            nomeFormatado,
                            stats.totalPartidas,
                            stats.vitorias,
                            stats.empates,
                            stats.derrotas,
                            stats.pontosTotais);
                });
    }

    private String formatarNome(String nomeCompleto) {
        nomeCompleto = nomeCompleto.replace("Agente", "").trim();

        String sufixo = "";
        if (nomeCompleto.contains("Modelo")) {
            sufixo = "Modelo";
            nomeCompleto = nomeCompleto.replace("Modelo", "").trim();
        } else if (nomeCompleto.contains("Busca")) {
            sufixo = "Busca";
            nomeCompleto = nomeCompleto.replace("Busca", "").trim();
        }

        String[] partesNome = nomeCompleto.split("(?=[A-Z])");
        if (partesNome.length > 1) {
            String nomeComposto = String.join(" e ", partesNome);
            return nomeComposto + " (" + sufixo + ")";
        }

        return nomeCompleto + " (" + sufixo + ")";
    }

    private BigInteger fatorial(int n) {
        BigInteger resultado = BigInteger.ONE;
        for (int i = 1; i <= n; i++) {
            resultado = resultado.multiply(BigInteger.valueOf(i));
        }
        return resultado;
    }

    private void play(Map.Entry<String, Player> item1, Map.Entry<String, Player> item2) throws InterruptedException {
        semaphore.acquire(); // Controle do número máximo de threads simultâneas

        try {
            String nomeJogador1 = item1.getKey();
            String nomeJogador2 = item2.getKey();
            Game game = new Game(item1.getValue(), item2.getValue());
            int resultado = game.start();

            PlayerStats stats1 = playerStats.get(nomeJogador1);
            PlayerStats stats2 = playerStats.get(nomeJogador2);

            stats1.totalPartidas++;
            stats2.totalPartidas++;

            if (resultado == 1) {
                stats1.vitorias++;
                stats1.pontosTotais += 3;
                stats2.derrotas++;
            } else if (resultado == 2) {
                stats2.vitorias++;
                stats2.pontosTotais += 3;
                stats1.derrotas++;
            } else {
                stats1.empates++;
                stats2.empates++;
                stats1.pontosTotais++;
                stats2.pontosTotais++;
            }
        } finally {
            semaphore.release(); // Liberar a capacidade para outra thread
        }
    }

    public static void main(String[] args) {
        new Tournament().execute(50); // preferir números pares
    }
}
