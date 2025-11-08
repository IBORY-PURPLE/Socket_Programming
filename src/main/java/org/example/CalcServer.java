package org.example;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;


public class CalcServer {
    private static final int PORT = 9999;

    private static final int POOL_SIZE = Math.max(4, Runtime.getRuntime().availableProcessors() * 2);

    private static final int SOCKET_TIMEOUT_MS = 60_000;

    public static void main(String[] args) {
        ExecutorService pool = new ThreadPoolExecutor(
                POOL_SIZE,
                POOL_SIZE,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new ThreadFactory() {
                    private final ThreadFactory df = Executors.defaultThreadFactory();
                    @Override public Thread newThread(Runnable r) {
                        Thread t = df.newThread(r);
                        t.setName("client-handler-" + t.getId());
                        return t;
                    }
                },
                new ThreadPoolExecutor.AbortPolicy()
        );

        try (ServerSocket server = new ServerSocket()) {
            server.setReuseAddress(true);
            server.bind(new InetSocketAddress(PORT));
            System.out.println("[SERVER] started on port " + PORT + " (pool=" + POOL_SIZE + ")");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n[SERVER] shutdown requested. closing pool...");
                pool.shutdown();
                try {
                    if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                        System.out.println("[SERVER] forcing pool shutdownNow()");
                        pool.shutdownNow();
                    }
                } catch (InterruptedException ignored) {
                    pool.shutdownNow();
                }
                System.out.println("[SERVER] bye");
            }));

            while (true) {
                try {
                    Socket client = server.accept();
                    client.setSoTimeout(SOCKET_TIMEOUT_MS);
                    System.out.println("[ACCEPT] " + client.getRemoteSocketAddress());
                    pool.submit(new ClientHandler(client)); // Runnable 제출
                } catch (RejectedExecutionException ex) {
                    System.err.println("[WARN] pool rejected client: " + ex.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("[FATAL] server error: " + e);
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (socket;
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                BufferedWriter out = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream()))
            ) {
                out.write("OK READY\n"); out.flush();

                String line;
                while ((line = in.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    if ("QUIT".equalsIgnoreCase(line)) {
                        out.write("OK BYE\n"); out.flush();
                        break;
                    }

                    try {
                        String response = handleCalc(line);
                        out.write("OK " + response + "\n");
                    } catch (IllegalArgumentException ex) {
                        out.write("ERR " + errorCodeOf(ex) + " " + ex.getMessage() + "\n");
                    } catch (Exception ex) {
                        out.write("ERR E_INTERNAL internal_error\n");
                    }
                    out.flush();
                }
            } catch (SocketTimeoutException ste) {
                System.err.println("[TIMEOUT] " + socket.getRemoteSocketAddress());
            } catch (IOException ioe) {
                System.err.println("[IOERR] " + socket.getRemoteSocketAddress() + " : " + ioe.getMessage());
            }
        }

        private String handleCalc(String req) {
            String[] tok = req.split("\\s+");
            if (tok.length < 3) throw new IllegalArgumentException("bad_request");

            String op = tok[0].toUpperCase(Locale.ROOT);
            if (tok.length != 3) throw new IllegalArgumentException("too_many_or_few_args");

            double a = parseNumber(tok[1]);
            double b = parseNumber(tok[2]);

            return switch (op) {
                case "ADD" -> String.valueOf(a + b);
                case "SUB" -> String.valueOf(a - b);
                case "MUL" -> String.valueOf(a * b);
                case "DIV" -> {
                    if (b == 0.0) throw new IllegalArgumentException("divided_by_zero");
                    yield String.valueOf(a / b);
                }
                default -> throw new IllegalArgumentException("unknown_op");
            };
        }

        private static double parseNumber(String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("nan");
            }
        }

        private static String errorCodeOf(IllegalArgumentException ex) {
            return switch (ex.getMessage()) {
                case "divided_by_zero" -> "0으로 나눌 수 없습니다.";
                case "unknown_op" -> "알수없는 연산자입니다.";
                case "too_many_or_few_args" -> "인자가 너무 많습니다.";
                case "nan" -> "숫자가 아닌 값을 입력했습니다.";
                case "bad_request" -> "잘못된 요청입니다.";
                default -> "잘못된 요청입니다.";
            };
        }

    }
}

