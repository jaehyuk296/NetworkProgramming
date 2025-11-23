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
                        System.out.println("[GAME] 방 입장 요청: " + nickname + " -> " + roomId + "번방");
                        
                        GameRoom room = lobby.getRoom(roomId);
                        
                        if (room != null) {
                            room.enterUser(this);
                            this.currentRoom = room;
                            lobby.removeUser(this); // 로비 퇴장
                        } else {
                            System.out.println("[GAME] 입장 실패 (방 없음): " + roomId);
                        }
                        break;
                        
                    // 나중에 추가할 채팅은 [CHAT] 태그 사용
                    // case Message.CHAT_MSG:
                    //    System.out.println("[CHAT] " + nickname + ": " + msg.getData1());
                    //    break;
                }
            }
        } catch (Exception e) {
            System.out.println("[CONN] 연결 종료: " + nickname);
            lobby.removeUser(this);
            if(currentRoom != null) currentRoom.exitUser(this);
        }
    }
}