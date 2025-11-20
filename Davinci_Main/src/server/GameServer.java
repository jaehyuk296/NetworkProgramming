package server;

import java.net.ServerSocket;
import java.net.Socket;

public class GameServer {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(9999)) {
            System.out.println(">>> 다빈치코드 서버 실행 중 (Port: 9999) <<<");
            
            while (true) {
                // 클라이언트 접속 대기
                Socket socket = serverSocket.accept();
                System.out.println(">>> 클라이언트 접속됨: " + socket.getInetAddress());
                
                // 전담 마크맨(핸들러) 붙여주기
                new ClientHandler(socket).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}