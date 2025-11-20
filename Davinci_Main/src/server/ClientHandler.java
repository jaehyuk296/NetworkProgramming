package server;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import protocol.Message;

public class ClientHandler extends Thread {
    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private String nickname;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            // 1. 스트림 초기화
            oos = new ObjectOutputStream(socket.getOutputStream());
            oos.flush();
            ois = new ObjectInputStream(socket.getInputStream());

            // 2. 첫 번째 메시지 수신 (로그인 요청)
            Message msg = (Message) ois.readObject();

            if (msg.getMode() == Message.REQ_LOGIN) {
                nickname = (String) msg.getData1();
                System.out.println(">>> 접속 요청: " + nickname);

                // 클라이언트에게 로그인 성공 신호 보내기
                oos.writeObject(new Message(Message.LOGIN_SUCCESS));
                oos.flush();
            }

            // 3. 이후 채팅이나 게임 로직 대기
            while (true) {
                Message loopMsg = (Message) ois.readObject();
                // 나중에 구현
            }

        } catch (Exception e) {
            System.out.println(">>> 연결 끊김: " + nickname);
        }
    }
}