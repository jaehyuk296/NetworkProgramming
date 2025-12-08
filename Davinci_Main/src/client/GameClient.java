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
    private Waiting waitingPanel; // [추가] 대기방 패널
    private InGameUI inGamePanel;

    // 네트워크 관련
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

        // 1. 화면 전환용 카드 레이아웃 설정
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // 2. 로그인 화면 생성
        loginPanel = new LoginPanel(e -> connectToServer());

        // 3. 로비 화면 생성
        lobbyPanel = new Lobby(
            // [방 만들기 버튼 동작]
            e -> {
                String roomTitle = JOptionPane.showInputDialog(this, "방 제목을 입력하세요:", "방 만들기", JOptionPane.QUESTION_MESSAGE);
                if (roomTitle != null && !roomTitle.trim().isEmpty()) {
                    requestCreateRoom(roomTitle.trim());
                }
            }, 
            // [입장하기 버튼 동작]
            e -> {
                String roomId = lobbyPanel.getSelectedRoomId();
                if (roomId == null) {
                    JOptionPane.showMessageDialog(this, "입장할 방을 목록에서 선택해주세요.");
                } else {
                    requestJoinRoom(roomId);
                }
            }
        );

        // 4. [추가] 대기방 화면 생성
        waitingPanel = new Waiting(this); // 팀원 코드에 맞춰 파라미터 없이 생성
        
        inGamePanel = new InGameUI(this);

        // 5. 패널 등록
        mainPanel.add(loginPanel, "LOGIN");
        mainPanel.add(lobbyPanel, "LOBBY");
        mainPanel.add(waitingPanel, "ROOM"); // [추가] 대기방 등록
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
     /* Waiting Panel에서 Lobby Panel로 화면을 전환하고,
     * 방 목록을 갱신하도록 요청합니다.
     */
    public void showLobby() {
        // UI 스레드에서 화면 전환 실행
        SwingUtilities.invokeLater(() -> {
            cardLayout.show(mainPanel, "LOBBY");
            // 로비로 돌아온 후, 방 목록을 서버에 요청하여 갱신합니다.
            requestRoomList(); 
        });
    }
    
    // [추가] 서버에 방 목록 갱신을 요청하는 메서드
    private void requestRoomList() {
        if (oos == null) return;
        try {
            // 서버에 방 목록 갱신을 요청하는 메시지를 보냅니다.
            // (Message.REQ_JOIN_ROOM 상수가 재활용되거나 새로운 상수가 필요할 수 있습니다. 
            // 현재는 REQ_JOIN_ROOM이 방 입장 요청이므로, 새로운 요청 상수(예: REQ_ROOM_LIST)가 필요하지만, 
            // 현재 코드를 기반으로 메시지를 보냅니다. Lobby.java의 broadcastRoomList()는 
            // lobbyUsers에게 SRES_ROOM_LIST를 보내므로, Lobby로 돌아왔다면 목록이 갱신되어야 합니다.)
            // 임시로 Lobby에서 방 목록을 요청받는 기능이 있다고 가정하고 메시지를 보냅니다.
            
            // Note: 현재 서버 코드는 클라이언트가 로비로 돌아오면 (Lobby.addUser(this)) 
            // 서버가 자동으로 목록을 보내주므로, 클라이언트가 명시적으로 요청할 필요는 없을 수 있습니다.
            // 하지만 안전을 위해 명시적 요청을 보낼 수도 있습니다.
            
            // 현재 서버 코드는 클라이언트가 방에서 나가면 (ClientHandler.REQ_EXIT_ROOM 처리):
            // 1. currentRoom.exitUser(this);
            // 2. this.currentRoom = null;
            // 3. lobby.addUser(this);  <-- 이 시점에 Lobby.addUser()가 sendRoomList(this)를 호출하여 
            //    클라이언트에게 목록을 보내줍니다. (따라서 명시적 요청은 불필요할 수 있습니다.)
            
            // 만약 서버 측에서 자동 전송이 안된다면 이 주석을 제거하고 사용하세요:
            // oos.writeObject(new Message(Message.REQ_ROOM_LIST)); 
            // oos.flush();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
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
    
    // [내부 클래스] 서버 메시지를 계속 듣는 리스너
    class ServerListener extends Thread {
        @Override
        public void run() {
            try {
                while (true) {
                    Message msg = (Message) ois.readObject();

                    switch (msg.getMode()) {
                        case Message.SRES_ROOM_LIST: 
                            Vector<Vector<String>> rooms = (Vector<Vector<String>>) msg.getPayload();
                            SwingUtilities.invokeLater(() -> {
                                lobbyPanel.updateRoomList(rooms);
                            });
                            break;
                            
                        // [추가] 방 만들기 성공 -> 대기방 이동
                        case Message.RES_CREATE_SUCCESS:
                            System.out.println("방 생성 성공: " + msg.getData1());
                            SwingUtilities.invokeLater(() -> cardLayout.show(mainPanel, "ROOM"));
                            break;

                        // [추가] 방 입장 성공 -> 대기방 이동
                        // (Message.java에 RES_JOIN_SUCCESS 상수가 있어야 합니다. 없으면 3000 사용)
                        case Message.RES_JOIN_SUCCESS:
                            System.out.println("방 입장 성공!");
                            SwingUtilities.invokeLater(() -> cardLayout.show(mainPanel, "ROOM"));
                            break;
                            
                         // ServerListener 내부 run() 메서드의 switch 문 안
                        case Message.CHAT_MSG: // (상수 값이 없다면 서버 코드와 동일하게 맞춤)
                            String chatText = (String) msg.getData1(); // 서버 코드: "[" + nickname + "] " + msg.getData1()
                            SwingUtilities.invokeLater(() -> {
                            	if (waitingPanel != null && waitingPanel.isVisible()) {
                                    waitingPanel.appendChat(chatText);
                                }

                                // InGame 화면에서도 받을 수 있어야 함
                                if (inGamePanel != null && inGamePanel.isVisible()) {
                                    inGamePanel.appendChat(chatText);
                                }
                            });
                            break;
                        case Message.ROOM_UPDATE: // 방 상태 업데이트
                            
                            // [수정 전] 에러 발생 (String을 Vector로 바꾸려 함)
                            // Vector<String> userInfos = (Vector<String>) msg.getPayload();
                            
                            // [수정 후] 정답 (data1에 있는 Vector를 꺼냄)
                            Vector<String> userInfos = (Vector<String>) msg.getData1();
                            
                            // (필요하다면 방장 이름은 이렇게 꺼냄)
                            // String hostName = (String) msg.getPayload(); // data2

                            latestUserInfos = userInfos;
                            SwingUtilities.invokeLater(() -> {
                                waitingPanel.updatePlayers(latestUserInfos, myPlayerID); 
                            });
                            break;
                         // GameClient.java 내부 ServerListener 클래스

                        case Message.GAME_START:                            
                            // Data1: 내 패(Vector<Tile>), Data2: 이름목록(Vector<String>)
                            Vector<Tile> myHand = (Vector<Tile>) msg.getData1();
                            Vector<String> players = (Vector<String>) msg.getData2();
                            Map<String, Vector<String>> otherColors = (Map<String, Vector<String>>) msg.getData3();

                            SwingUtilities.invokeLater(() -> {
                                System.out.println("게임 시작!");
                                cardLayout.show(mainPanel, "GAME");
                                // InGameUI 초기화 호출
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

                        case Message.RES_DRAW:
                            // 타일 뽑기 결과
                            // Data1: Tile 객체, Data2: Index (Integer) -> Payload getter 사용 시 주의
                            // Message 생성자를 보면 (mode, data1, payload) 순서일 수 있음. 확인 필요.
                            // 여기선 Server 코드: new Message(RES_DRAW, drawn, insertIndex)
                            Tile drawnTile = (Tile) msg.getData1();
                            int insertIdx = (Integer) msg.getData2(); // data2(payload)에 인덱스

                            SwingUtilities.invokeLater(() -> {
                                inGamePanel.handleDrawResult(drawnTile, insertIdx);
                            });
                            break;

                        case Message.BCAST_REVEAL:
                            // 타일 공개 알림
                            // Data1: "TargetID:Index", Data2: Tile 객체
                            String revealInfo = (String) msg.getData1();
                            Tile revealedTile = (Tile) msg.getData2();
                            
                            String[] rParts = revealInfo.split(":");
                            String rTarget = rParts[0];
                            int rIndex = Integer.parseInt(rParts[1]);

                            SwingUtilities.invokeLater(() -> {
                                inGamePanel.handleReveal(rTarget, rIndex, revealedTile);
                                inGamePanel.appendChat("[시스템] " + rTarget + "님의 타일이 공개되었습니다: " + revealedTile.getNumber());
                            });
                            break;

                        case Message.TURN_TIMEOUT:
                        	// 서버에서 턴 넘김 알림을 보내는 경우
                            String nextPlayer = (String) msg.getData1();
                            inGamePanel.updateTurn(nextPlayer);
                            break;
                    }
                }
            } catch (Exception e) {
                System.out.println("서버와의 연결이 끊어졌습니다.");
            }
        }
    }
    
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