package org.example;
import java.io.*;
import java.net.*;
import java.util.*;

// 첫번째 클라이언트
public class CalcClient {
    public static void main(String[] args) {
        BufferedReader in = null;
        BufferedWriter out = null;
        Socket socket = null;
        Scanner scanner = new Scanner(System.in);
        try {
            ConfigReader.ServerEndpoint ep = ConfigReader.loadServerEndpoint();

            socket = new Socket();
            socket.connect(new InetSocketAddress(ep.host, ep.port), 5000);

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            String banner = in.readLine();
            if (banner == null) {
                System.out.println("No banner");
                return;
            }
            System.out.println("서버와 연결 상태: " + banner);
            if (!banner.startsWith("OK READY")) {
                System.out.println("예상과 다른 배너: " + banner);
                return;
            }
            while (true) {
                System.out.print("계산식(빈칸으로 띄어 입력, 예: ADD 24 24) >>> ");
                String outputMessage = scanner.nextLine();
                if (outputMessage.equalsIgnoreCase("bye")) {
                    out.write(outputMessage + "\n");
                    out.flush();
                    break;
                }
                out.write(outputMessage + "\n");
                out.flush();
                String inputMessage = in.readLine();
                System.out.println("계산 결과: " + inputMessage);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } finally {
            try {
                scanner.close();
                if (socket != null)
                    socket.close();
            } catch (IOException e) {
                System.out.println("서버와 채팅 중 오류가 발생했습니다.");
            }
        }
    }
}
