package client;

import java.awt.CardLayout;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.Scanner;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import protocol.Message;
import protocol.Tile;

public class GameClient extends JFrame {
    
    private CardLayout cardLayout;
    private JPanel mainPanel;
    
    private LoginPanel loginPanel;
    private Lobby lobbyPanel;
    private Waiting waitingPanel;
    private InGame inGamePanel;

    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private String myPlayerID;

    public static void main(String[] args) {
        new GameClient();
    }
    
	// Waiting 방에서 최신으로 받은 유저 목록 저장용
    private Vector<String> latestUserInfos = new Vector<>();


    public GameClient() {
        super("다빈치코드 Online");
        setSize(1000, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // 화면 전환용 카드 레이아웃 설정
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // 로그인 화면 생성
        loginPanel = new LoginPanel(e -> connectToServer());

        // 로비 화면 생성
        lobbyPanel = new Lobby(
            // 방 만들기 버튼 동작
            e -> {
                String roomTitle = JOptionPane.showInputDialog(this, "방 제목을 입력하세요:", "방 만들기", JOptionPane.QUESTION_MESSAGE);
                if (roomTitle != null && !roomTitle.trim().isEmpty()) {
                    requestCreateRoom(roomTitle.trim());
                }
            }, 
            // 입장하기 버튼 동작
            e -> {
                String roomId = lobbyPanel.getSelectedRoomId();
                if (roomId == null) {
                    JOptionPane.showMessageDialog(this, "입장할 방을 목록에서 선택해주세요.");
                } else {
                    requestJoinRoom(roomId);
                }
            }
        );

        // 대기방 화면 생성
        waitingPanel = new Waiting(this);
        
        // 인게임방 화면 생성
        inGamePanel = new InGame(this);

        // 패널 등록
        mainPanel.add(loginPanel, "LOGIN");
        mainPanel.add(lobbyPanel, "LOBBY");
        mainPanel.add(waitingPanel, "ROOM");
        mainPanel.add(inGamePanel, "GAME");

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
    
    public void showLobby() {
        // UI 스레드에서 화면 전환 실행
        SwingUtilities.invokeLater(() -> {
            cardLayout.show(mainPanel, "LOBBY");
        });
    }

    // 서버 접속 및 화면 전환 로직
    private void connectToServer() {
        String nickname = loginPanel.getNickname();
        loginPanel.setStatus("서버 접속 시도 중...");

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
            	this.myPlayerID = nickname;
                lobbyPanel.updateGreeting(nickname);
                cardLayout.show(mainPanel, "LOBBY");
                setTitle("다빈치코드 - " + nickname);
                
                // 로그인 성공 시 리스너 시작
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
    
    // 서버 메시지를 듣는 리스너
    class ServerListener extends Thread {
        @Override
        public void run() {
            try {
                while (true) {
                    Message msg = (Message) ois.readObject();

                    switch (msg.getMode()) {
                    	// 방 목록
                        case Message.SRES_ROOM_LIST: 
                            Vector<Vector<String>> rooms = (Vector<Vector<String>>) msg.getPayload();
                            SwingUtilities.invokeLater(() -> {
                                lobbyPanel.updateRoomList(rooms);
                            });
                            break;

                        case Message.RES_JOIN_FAIL:
                            String failReason = (String) msg.getData1();
                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(
                                    GameClient.this, 
                                    failReason, 
                                    "입장 실패", 
                                    JOptionPane.WARNING_MESSAGE
                                );
                            });
                            break;
                            
                        // 방 만들기 성공 -> 대기방 이동
                        case Message.RES_CREATE_SUCCESS:
                            System.out.println("방 생성 성공: " + msg.getData1());
                            SwingUtilities.invokeLater(() -> cardLayout.show(mainPanel, "ROOM"));
                            break;

                        // 방 입장 성공 -> 대기방 이동
                        case Message.RES_JOIN_SUCCESS:
                            System.out.println("방 입장 성공!");
                            SwingUtilities.invokeLater(() -> cardLayout.show(mainPanel, "ROOM"));
                            break;
                            
                         // 채팅
                        case Message.CHAT_MSG:
                            String chatText = (String) msg.getData1();
                            SwingUtilities.invokeLater(() -> {
                            	if (waitingPanel != null && waitingPanel.isVisible()) {
                                    waitingPanel.appendChat(chatText);
                                }
                            	
                                if (inGamePanel != null && inGamePanel.isVisible()) {
                                    inGamePanel.appendChat(chatText);
                                }
                            });
                            break;
                         // 방 상태 업데이트
                        case Message.ROOM_UPDATE:
                            Vector<String> userInfos = (Vector<String>) msg.getData1();
                            
                            latestUserInfos = userInfos;
                            SwingUtilities.invokeLater(() -> {
                                waitingPanel.updatePlayers(latestUserInfos, myPlayerID); 
                            });
                            break;

                        case Message.GAME_START:                            
                            Vector<Tile> myHand = (Vector<Tile>) msg.getData1();
                            Vector<String> players = (Vector<String>) msg.getData2();
                            Map<String, Vector<String>> otherColors = (Map<String, Vector<String>>) msg.getData3();

                            SwingUtilities.invokeLater(() -> {
                                System.out.println("게임 시작!");
                                cardLayout.show(mainPanel, "GAME");
                                inGamePanel.startGame(myHand, players, otherColors, myPlayerID);
                            });
                            break;

                        case Message.TURN_START:
                            // Data1: 현재 턴인 사람 닉네임
                            String currentNick = (String) msg.getData1();
                            SwingUtilities.invokeLater(() -> {
                                inGamePanel.updateTurn(currentNick);
                            });
                            break;

                         // 타일 뽑기 결과
                        case Message.RES_DRAW:
                            Tile drawnTile = (Tile) msg.getData1();
                            int insertIdx = (Integer) msg.getData2();

                            SwingUtilities.invokeLater(() -> {
                                inGamePanel.handleDrawResult(drawnTile, insertIdx);
                            });
                            break;
                            
                        case Message.BCAST_DRAW:
                            // Vector<Object> [닉네임, 색상, 인덱스]
                            Vector<Object> drawInfo = (Vector<Object>) msg.getData1();
                            
                            String drawerNick = (String) drawInfo.get(0);
                            String drawnColor = (String) drawInfo.get(1);
                            int drawnIdx = (int) drawInfo.get(2);

                            SwingUtilities.invokeLater(() -> {
                                if (inGamePanel != null) {
                                    // InGameUI에 상대방 타일 추가 요청
                                    inGamePanel.handleOpponentDraw(drawerNick, drawnColor, drawnIdx);
                                }
                            });
                            break;

                         // 타일 공개 알림
                        case Message.BCAST_REVEAL:
                            // Data1: "TargetID:Index"
                            String revealInfo = (String) msg.getData1();
                            // Data2: Tile 객체
                            Tile revealedTile = (Tile) msg.getData2();
                            
                            String[] rParts = revealInfo.split(":");
                            String rTarget = rParts[0];
                            int rIndex = Integer.parseInt(rParts[1]);

                            SwingUtilities.invokeLater(() -> {
                                inGamePanel.handleReveal(rTarget, rIndex, revealedTile);
                                inGamePanel.appendChat("[System] " + rTarget + "님의 타일이 공개되었습니다: " + revealedTile.getNumber());
                            });
                            break;

                         // 서버에서 턴 넘김 알림을 보내는 경우
                        case Message.TURN_TIMEOUT:
                            String nextPlayer = (String) msg.getData1();
                            inGamePanel.updateTurn(nextPlayer);
                            break;
                        
                         // 게임 종료
                        case Message.GAME_OVER:
                            String winner = (String) msg.getData1();
                            String gameOverMessage = "게임 종료! 승자는 " + winner + "님 입니다.";
                            
                            SwingUtilities.invokeLater(() -> {
                                // 팝업창 표시
                                int option = JOptionPane.showOptionDialog(
                                    GameClient.this,
                                    gameOverMessage,
                                    "게임 종료",
                                    JOptionPane.DEFAULT_OPTION,
                                    JOptionPane.INFORMATION_MESSAGE,
                                    null,
                                    new Object[]{"대기하기", "나가기"},
                                    "대기하기"
                                );
                                
                                // 대기하기 버튼 클릭 시
                                if (option == 0) {
                                    cardLayout.show(mainPanel, "ROOM");
                                }
                                // 나가기 버튼 클릭 시
                                else if (option == 1) {
                                    send(new Message(Message.REQ_EXIT_ROOM));
                                    showLobby();
                                }
                                // 팝업 닫기 시 기본 동작은 나가기
                                else if (option == JOptionPane.CLOSED_OPTION) {
                                    send(new Message(Message.REQ_EXIT_ROOM));
                                    showLobby();
                                }
                            });
                            break;
                        
                    }
                }
            } catch (Exception e) {
                System.out.println("서버와의 연결이 끊어졌습니다.");
            }
        }
    }
    
    // 서버와 통신
    public void send(Message msg) {
        if (oos == null) return;
        try {
            oos.writeObject(msg);
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}