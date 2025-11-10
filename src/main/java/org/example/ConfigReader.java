package org.example;

import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

// resources/org/server_info.dat에서 server 주소와 port번호 읽어오는 class
public final class ConfigReader {
    private ConfigReader() {}

    // ip주소와 port 기본값 설정
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 9999;

    public static ServerEndpoint loadServerEndpoint() throws IOException {

        InputStream in = ConfigReader.class.getResourceAsStream("/org/server_info.dat");
        if (in != null) {
            System.out.println(" classpath:/org/server_info.dat에서 설정 로드");
            return parseLines(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines().toList());
        }

        Path path = Paths.get("org", "server_info.dat");
        if (Files.exists(path)) {
            System.out.println("파일시스템에서 설정 로드: "+ path.toAbsolutePath());
            return parseLines(Files.readAllLines(path, StandardCharsets.UTF_8));
        }

        System.out.printf("설정 파일을 찾을 수 없음. 기본값 사용 -> %s:%d%n", DEFAULT_HOST, DEFAULT_PORT);
        return new ServerEndpoint(DEFAULT_HOST, DEFAULT_PORT);

    }

    private static ServerEndpoint parseLines(List<String> rawLines) throws IOException {
        List<String> lines = rawLines.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty() && !s.startsWith("#"))
                .toList();

        if (lines.isEmpty()) throw new IOException("server_info.dat 내용이 비어 있음");
        String first = lines.get(0);

        if (first.contains("=")) {
            Properties props = new Properties();
            props.load(new StringReader(String.join("\n", lines)));
            String host = getRequired(props, "host");
            int port = Integer.parseInt(getRequired(props, "port"));
            return new ServerEndpoint(host, port);
        }
        throw new IOException("지원하지 않는 서버 주소 형식: " + first);
    }

    private static String getRequired(Properties props, String key) throws IOException {
        String v = props.getProperty(key);
        if (v == null || v.isBlank()) throw new IOException("누락된 키: " + key);
        return v.trim();
    }

    public static final class ServerEndpoint {
        public final String host;
        public final int port;
        public ServerEndpoint(String host, int port) {
            this.host = host;
            this.port = port;
        }
        @Override public String toString() { return host + ":" + port;}
    }
}
