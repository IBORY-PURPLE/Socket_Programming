package org.example;
import java.io.*;
import java.net.*;

public class Server1 {
    public static void main(String[] args) throws IOException {
        ServerSocket welcomeSocket;
        String clientSentence;
        String capitalizedSentence;
        int nPort;

        // 10. welcomeSocket은 클라이언트가 요청을 보내 올 때 까지 대기하는 소켓이고
        //connectionSocket이 클라이언트 요청이 왔을 떄 실제 사용하는 소켓으로 이해하면 되나?
        nPort = 6789;
        welcomeSocket = new ServerSocket(nPort);
        System.out.println("Server start.. (port#=" + nPort + ")\n ");
        while (true) {
            // connectionSocket이 실제 데이터 통신을 할 때 사용하는 소켓
            Socket connectionSocket = welcomeSocket.accept();
            // getInputStream은 바이트 단위의 0,1로만 문자열을 인식하고 그 것을 Reader클래스로 문자로 변환해주고 그것을 Buffered에 저장해서  readLine()함수로 필요할 때 한 줄
            // 바로 긁어올 수 있는 느낌의 코드.
            BufferedReader inFromClient = new BufferedReader(
                    new InputStreamReader(connectionSocket.getInputStream()));
            // 2. outToClient가 DataOutputStream 클래스의 인스턴스로서 서버 클래스에서 클라이언트로 내보낼 데이터를 정하는 함수의 역할?
            DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
            clientSentence = inFromClient.readLine();
            capitalizedSentence = clientSentence.toUpperCase() + '\n';
            outToClient.writeBytes(capitalizedSentence);
        }
    }
}
