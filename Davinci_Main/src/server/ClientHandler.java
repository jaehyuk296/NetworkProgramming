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
    private Lobby lobby;
    private GameRoom currentRoom;

    public ClientHandler(Socket socket, Lobby lobby) {
        this.socket = socket;
        this.lobby = lobby;
    }

    public String getNickname() { return nickname; }

    public void sendMessage(Message msg) {
        try {
            oos.writeObject(msg);
            oos.flush();
            oos.reset();
        } catch (Exception e) { 
            System.out.println("[ERR] 메시지 전송 실패 (" + nickname + "): " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            oos = new ObjectOutputStream(socket.getOutputStream());
            oos.flush();
            ois = new ObjectInputStream(socket.getInputStream());

            // 1. 로그인 처리
            Message msg = (Message) ois.readObject();
            if (msg.getMode() == Message.REQ_LOGIN) {
                nickname = (String) msg.getData1();
                System.out.println("[CONN] 로그인 요청: " + nickname + " (" + socket.getInetAddress() + ")");
                
                // 로그인 성공 전송
                sendMessage(new Message(Message.LOGIN_SUCCESS));
                lobby.addUser(this);
            }

            // 2. 메인 루프
            while (true) {
                msg = (Message) ois.readObject();
                
                switch (msg.getMode()) {
                    case Message.REQ_CREATE_ROOM:
                        String title = (String) msg.getData1();
                        System.out.println("[GAME] 방 생성 요청: " + nickname + " -> " + title);
                        lobby.createRoom(this, title);
                        break;

                    case Message.REQ_JOIN_ROOM:
                        int roomId = Integer.parseInt(String.valueOf(msg.getData1()));
                        System.out.println("[GAME] 방 입장 요청: " + nickname + " -> " + roomId);
                        
                        GameRoom room = lobby.getRoom(roomId);
                        if (room != null) {
                            // 4명 꽉 찼는지 확인
                            if (room.getUserCount() >= 4) {
                                // 실패 메시지 전송 (필요시 구현)
                                System.out.println("[GAME] 입장 실패 (풀방)");
                            } else {
                                lobby.removeUser(this); // 로비에서 제거
                                room.enterUser(this);   // 방에 추가
                                this.currentRoom = room; // 현재 방 기억
                            }
                        }
                        break;

                case Message.CHAT_MSG: // 대기방 채팅
                    if (currentRoom != null) {
                        // "닉네임: 할말" 형태로 만들어서 뿌림
                        String chatText = "[" + nickname + "] " + msg.getData1();
                        currentRoom.broadcastChat(chatText);
                        System.out.println("[CHAT] " + chatText); // 서버 로그
                    }
                    break;

                case Message.REQ_READY: // 준비 버튼
                    if (currentRoom != null) {
                        currentRoom.toggleReady(this);
                    }
                    break;

                case Message.REQ_EXIT_ROOM: // 방 나가기
                    if (currentRoom != null) {
                        currentRoom.exitUser(this); // 방에서 나감
                        this.currentRoom = null;    // 방 정보 지움
                        lobby.addUser(this);        // 다시 로비로 복귀
                    }
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("[CONN] 연결 종료: " + nickname);
            lobby.removeUser(this);
            if(currentRoom != null) currentRoom.exitUser(this);
        }
    }
}