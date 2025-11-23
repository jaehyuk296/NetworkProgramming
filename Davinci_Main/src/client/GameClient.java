package client;

import java.awt.CardLayout;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

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
        		// [방 만들기 버튼 동작]
            e -> {
            	// 1. 팝업으로 방 제목 입력 받기
            	String roomTitle = JOptionPane.showInputDialog(
            			this,
            			"방 제목을 입력하세요: ",
            			"방 만들기",
            			JOptionPane.QUESTION_MESSAGE
            			);
            	
            	// 2. 입력값이 유효하면 서버로 요청 전송
            	if (roomTitle != null && !roomTitle.trim().isEmpty()) {
            		requestCreateRoom(roomTitle.trim());
            	}
            }, 
            // [입장하기 버튼 동작]
            e -> {
            	// 1. 로비 테이블에서 선택된 방 번호 가져오기
            	String roomId = lobbyPanel.getSelectedRoomId();
            	
            	if (roomId == null) {
            		JOptionPane.showMessageDialog(this, "입장할 방을 목록에서 선택해주세요.");
            	} else {
            		// 2. 서버로 입장 요청 전송
            		requestJoinRoom(roomId);
            	}
            }
        );

        // 4. 패널 등록
        mainPanel.add(loginPanel, "LOGIN");
        mainPanel.add(lobbyPanel, "LOBBY");

        add(mainPanel);
        
        // 시작은 로그인 화면
        cardLayout.show(mainPanel, "LOGIN");
        
        setVisible(true);
    }
    
    // 방 만들기 요청 전송
    private void requestCreateRoom(String title) {
        if (oos == null) return;
        try {
            oos.writeObject(new Message(Message.REQ_CREATE_ROOM, title));
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 방 입장 요청 전송
    private void requestJoinRoom(String roomId) {
        if (oos == null) return;
        try {
            oos.writeObject(new Message(Message.REQ_JOIN_ROOM, roomId));
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                
                // 로그인 성공 시 서버 메시지 수신 스레드 시작
                new ServerListener().start();
                
            } else {    
                JOptionPane.showMessageDialog(this, "로그인 실패");
            }

        } catch (Exception e) {
            e.printStackTrace();
            loginPanel.setStatus("연결 실패");
            JOptionPane.showMessageDialog(this, "서버 연결 실패!\n서버를 먼저 실행해주세요.");
        }
    }
    
 // [내부 클래스] 서버 메시지를 계속 듣는 리스너
    class ServerListener extends Thread {
        @Override
        public void run() {
            try {
                while (true) {
                    // 서버로부터 메시지 수신 (여기서 대기함)
                    Message msg = (Message) ois.readObject();

                    // 메시지 타입에 따른 처리
                    switch (msg.getMode()) {
                        case Message.SRES_ROOM_LIST: 
                            // 서버가 보낸 방 목록 데이터 꺼내기
                            Vector<Vector<String>> rooms = (Vector<Vector<String>>) msg.getPayload();
                            
                            // Swing UI 갱신은 안전하게 처리
                            SwingUtilities.invokeLater(() -> {
                                lobbyPanel.updateRoomList(rooms);
                            });
                            break;
                            
                        // 다른 케이스 추가 (예: 게임 시작, 채팅 등)
                    }
                }
            } catch (Exception e) {
                System.out.println("서버와의 연결이 끊어졌습니다.");
            }
        }
    }
}