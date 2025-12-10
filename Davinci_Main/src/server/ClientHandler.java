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
    public void setRoom(GameRoom room) { this.currentRoom = room; }

    public void sendMessage(Message msg) {
        try {
            oos.writeObject(msg);
            oos.flush();
            oos.reset();
        } catch (Exception e) {
            System.out.println("[ERR] 전송 실패 (" + nickname + ")");
        }
    }

    @Override
    public void run() {
        try {
            oos = new ObjectOutputStream(socket.getOutputStream());
            oos.flush();
            ois = new ObjectInputStream(socket.getInputStream());

            // 1. 로그인
            Message msg = (Message) ois.readObject();
            if (msg.getMode() == Message.REQ_LOGIN) {
                nickname = (String) msg.getData1();
                System.out.println("[CONN] 접속: " + nickname);
                sendMessage(new Message(Message.LOGIN_SUCCESS));
                lobby.addUser(this);
            }

            // 2. 메인 루프
            while (true) {
                msg = (Message) ois.readObject();
                
                switch (msg.getMode()) {
                    // [로비 관련]
                    case Message.REQ_CREATE_ROOM:
                        String title = (String) msg.getData1();
                        lobby.createRoom(this, title);
                        break;
                    case Message.REQ_JOIN_ROOM:
                        int roomId = Integer.parseInt(String.valueOf(msg.getData1()));
                        GameRoom targetRoom = lobby.getRoom(roomId); 
                        
                        // ★ 방이 존재하고, 인원이 4명 이상이면 입장 거부
                        if (targetRoom != null && targetRoom.getUserCount() >= 4) {
                            sendMessage(new Message(Message.RES_JOIN_FAIL, "이미 게임이 진행 중인 방입니다."));
                        } 
                        else {
                            // 자리가 있으면 정상적으로 입장 처리
                            lobby.joinRoom(this, roomId);
                        }
                        break;

                    // [게임방 관련] - GameRoom에게 위임
                    case Message.CHAT_MSG:
                        if(currentRoom != null) currentRoom.broadcastChat("[" + nickname + "] " + msg.getData1());
                        break;
                    case Message.REQ_READY:
                        if(currentRoom != null) currentRoom.toggleReady(this);
                        break;
                    case Message.REQ_EXIT_ROOM:
                        if(currentRoom != null) {
                            boolean isEmpty = currentRoom.exitUser(this);
                            if(isEmpty) lobby.removeRoom(currentRoom.getRoomId());
                            
                            this.currentRoom = null;
                            lobby.addUser(this); // 로비로 복귀
                        }
                        break;
                    
                    // [게임 진행 관련]
                    case Message.REQ_START_GAME:
                        if(currentRoom != null) currentRoom.startGame();
                        break;
                        
                    case Message.REQ_DRAW:
                    	if (currentRoom != null) {
                            String drawData = (String) msg.getData1();
                            currentRoom.handleDraw(this, drawData);
                        }
                    	break;
                    	
                    case Message.REQ_JOKER_POSITION:
                    	if (currentRoom != null) {
                            String jokerData = (String) msg.getData1();
                            currentRoom.handleJokerPosition(this, jokerData);
                        }
                    	break;
                    	
                    case Message.REQ_GUESS:
                        currentRoom.handleGuess(this, (String) msg.getData1());
                        break;
                        
                    case Message.REQ_END_TURN:
                        if (currentRoom != null) {
                            currentRoom.handleEndTurn(this);
                        }
                        break;
                    
                    case Message.TURN_TIMEOUT:
                    	currentRoom.handleTimeout(this);
                    	break;

                }
            }
        } catch (Exception e) {
            System.out.println("[CONN] 종료: " + nickname);
            lobby.removeUser(this);
            if(currentRoom != null) {
                if(currentRoom.exitUser(this)) lobby.removeRoom(currentRoom.getRoomId());
            }
        }
    }
}