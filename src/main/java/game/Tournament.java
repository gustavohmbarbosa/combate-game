package game;

import game.players.Player;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;

public class Tournament {

    private final List<Map.Entry<String, Class<? extends Player>>> agents = new ArrayList<>();
    private final Map<String, PlayerStats> playerStats = new ConcurrentHashMap<>();
    private final Semaphore semaphore = new Semaphore(100);

    private static class PlayerStats {
        int totalMatches;
        int wins;
        int draws;
        int losses;
        int totalPoints;

        synchronized void addWin() {
            wins++;
            totalPoints += 3;
            totalMatches++;
        }

        synchronized void addDraw() {
            draws++;
            totalPoints++;
            totalMatches++;
        }

        synchronized void addLoss() {
            losses++;
            totalMatches++;
        }
    }

    private void loadAgents() {
        Reflections reflections = new Reflections("game.players", new SubTypesScanner(false));
        Set<Class<? extends Player>> classes = reflections.getSubTypesOf(Player.class);

        classes.forEach(clazz -> {
            agents.add(new AbstractMap.SimpleEntry<>(clazz.getSimpleName(), clazz));
            playerStats.put(clazz.getSimpleName(), new PlayerStats());
        });
    }

    public void execute(int matchesPerPair) {
        printHeader();
        loadingEffect(5);
        loadAgents();

        if (agents.size() < 2) {
            System.out.println("Necessário pelo menos dois jogadores.");
            return;
        }

        System.out.println("Iniciando Torneio...\n");
        long startTime = System.nanoTime();

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < agents.size(); i++) {
            for (int j = i + 1; j < agents.size(); j++) {
                final var player1Class = agents.get(i);
                final var player2Class = agents.get(j);

                System.out.printf("Executando partidas de %s contra %s\n", player1Class.getKey(), player2Class.getKey());

                for (int match = 0; match < matchesPerPair / 2; match++) {
                    futures.add(executor.submit(() -> playMatch(player1Class, player2Class)));
                }
            }
        }

        awaitCompletion(futures);
        executor.shutdown();

        printResults(startTime);
        System.exit(0);
    }

    private void playMatch(Map.Entry<String, Class<? extends Player>> player1Class,
                           Map.Entry<String, Class<? extends Player>> player2Class) {
        try {
            semaphore.acquire();
            Player player1 = instantiatePlayer(player1Class.getValue());
            Player player2 = instantiatePlayer(player2Class.getValue());

            int result = new Game(player1, player2).start();
            updateStats(player1Class.getKey(), player2Class.getKey(), result);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            semaphore.release();
        }
    }

    private Player instantiatePlayer(Class<? extends Player> playerClass) throws Exception {
        return playerClass.getDeclaredConstructor().newInstance();
    }

    private void updateStats(String player1Name, String player2Name, int result) {
        PlayerStats stats1 = playerStats.get(player1Name);
        PlayerStats stats2 = playerStats.get(player2Name);

        if (result == 1) {
            stats1.addWin();
            stats2.addLoss();
        } else if (result == 2) {
            stats2.addWin();
            stats1.addLoss();
        } else {
            stats1.addDraw();
            stats2.addDraw();
        }
    }

    private void awaitCompletion(List<Future<?>> futures) {
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }
    }

    private void printResults(long startTime) {
        long duration = System.nanoTime() - startTime;

        System.out.println("\nTorneio finalizado!\n");
        loadingEffect(5);
        showWinner();

        System.out.println("\nClassificação final:\n");
        loadingEffect(5);
        printFinalRanking();

        System.out.println("\nTotal de jogadores: " + agents.size());
        System.out.println("Duração total: " + TimeUnit.NANOSECONDS.toSeconds(duration) + " segundos");
    }

    private void printHeader() {
        String header = "\n" +
                "===============================================================\n" +
                "          TTTTT  OOO  RRRR   N   N  EEEEE  III   OOO \n" +
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
                .max(Comparator.comparingInt(entry -> entry.getValue().totalPoints))
                .map(Map.Entry::getKey)
                .orElse("Nenhum vencedor");

        System.out.println("\n==================== Vencedor(es) do Torneio ====================");
        System.out.println("🏆 " + winner + " 🏆");
        System.out.println("Parabéns!!!\n");
    }

    private void printFinalRanking() {
        System.out.println("\n======================================== Classificação Final ========================================");

        System.out.printf("%-40s\t%-8s\t%-9s\t%-8s\t%-9s\t%-5s%n",
                "Jogador(es)", "Partidas", "Vitórias", "Empates", "Derrotas", "Pontos");

        playerStats.entrySet().stream()
                .sorted((entry1, entry2) -> Integer.compare(entry2.getValue().totalPoints, entry1.getValue().totalPoints))
                .forEach(entry -> {
                    PlayerStats stats = entry.getValue();
                    String nomeFormatado = entry.getKey();
                    System.out.printf("%-40s\t%-8d\t%-9d\t%-8d\t%-9d\t%-5d%n",
                            nomeFormatado,
                            stats.totalMatches,
                            stats.wins,
                            stats.draws,
                            stats.losses,
                            stats.totalPoints);
                });
    }

    private BigInteger fatorial(int n) {
        BigInteger resultado = BigInteger.ONE;
        for (int i = 1; i <= n; i++) {
            resultado = resultado.multiply(BigInteger.valueOf(i));
        }
        return resultado;
    }
    public static void main(String[] args) {
        new Tournament().execute(50); // preferir números pares
    }
}
