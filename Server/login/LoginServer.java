package Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 클라이언트의 접속 요청 받기
 * 이후 ClientHadler에게 처리 위임
 * @author ESH
 */
public class LoginServer {
    
    public static int loginPort = 20805; // 로그인 포트 번호

    public static void main(String[] args) {
        
        try (ServerSocket serverSocket = new ServerSocket(loginPort)) {
            System.out.println("로그인 서버 시작됨...");
            
            while (true) {
                // 클라이언트 연결을 기다림
                Socket clientSocket = serverSocket.accept();
                System.out.println("클라이언트 연결됨: " + clientSocket.getInetAddress().getHostAddress());
                
                // ClientHandler에게 연결 처리 위임
                new Thread(new ClientHandler(clientSocket)).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
