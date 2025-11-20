package client;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import protocol.Message; // 프로토콜

public class GameClient extends JFrame {
    
    private CardLayout cardLayout;
    private JPanel mainPanel;
    
    private LoginPanel loginPanel;
    private Lobby lobbyPanel;

    // 네트워크 관련
    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private String myPlayerID;

    public static void main(String[] args) {
        new GameClient();
    }

    public GameClient() {
        super("다빈치코드 Online");
        setSize(1000, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // 1. 화면 전환용 카드 레이아웃 설정
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // 2. 로그인 화면 생성
        loginPanel = new LoginPanel(e -> connectToServer());

        // 3. 로비 화면 생성
        // (방 만들기 / 입장하기 버튼 눌렀을 때 동작 정의)
        lobbyPanel = new Lobby(
            e -> System.out.println("방 만들기 클릭됨! (나중에 서버 요청 구현)"), 
            e -> System.out.println("입장하기 클릭됨! (나중에 서버 요청 구현)")
        );

        // 4. 패널 등록
        mainPanel.add(loginPanel, "LOGIN");
        mainPanel.add(lobbyPanel, "LOBBY");

        add(mainPanel);
        
        // 시작은 로그인 화면
        cardLayout.show(mainPanel, "LOGIN");
        
        setVisible(true);
    }

    // 서버 접속 및 화면 전환 로직
    private void connectToServer() {
        String nickname = loginPanel.getNickname();
        loginPanel.setStatus("서버 접속 시도 중...");

        // IP/Port 설정 (server.txt)
        String ip = "localhost";
        int port = 9999;
        try {
            File file = new File("server.txt");
            if (file.exists()) {
                Scanner sc = new Scanner(file);
                if (sc.hasNext()) ip = sc.next();
                if (sc.hasNextInt()) port = sc.nextInt();
                sc.close();
            }
        } catch (Exception e) { }

        try {
            socket = new Socket(ip, port);
            oos = new ObjectOutputStream(socket.getOutputStream());
            oos.flush();
            ois = new ObjectInputStream(socket.getInputStream());

            // 로그인 요청
            oos.writeObject(new Message(Message.REQ_LOGIN, nickname));
            oos.flush();

            // 응답 대기
            Message response = (Message) ois.readObject();      

            if (response.getMode() == Message.LOGIN_SUCCESS) {
                lobbyPanel.updateGreeting(nickname);// 닉네임
                cardLayout.show(mainPanel, "LOBBY");
                setTitle("다빈치코드 - " + nickname);
            } else {    
                JOptionPane.showMessageDialog(this, "로그인 실패");
            }

        } catch (Exception e) {
            e.printStackTrace();
            loginPanel.setStatus("연결 실패");
            JOptionPane.showMessageDialog(this, "서버 연결 실패!\n서버를 먼저 실행해주세요.");
        }
    }
}