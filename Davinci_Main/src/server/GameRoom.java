package server;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import protocol.Message;

public class GameRoom {
    private int roomId;
    private String title;
    private String hostName; // 방장 닉네임
    private int maxUser = 4;
    private boolean isPlaying = false;

    // 유저 리스트 (통신용)
    private Vector<ClientHandler> userList = new Vector<>();
    
    // 준비 상태 관리 (닉네임 -> 준비여부)
    private Map<String, Boolean> readyState = new HashMap<>();

    public GameRoom(int roomId, String title, ClientHandler host) {
        this.roomId = roomId;
        this.title = title;
        this.hostName = host.getNickname();
        enterUser(host);
    }

    // 1. 유저 입장 처리
    public void enterUser(ClientHandler handler) {
        userList.add(handler);
        readyState.put(handler.getNickname(), false); // 기본값: 준비 안됨
        
        broadcastRoomState();
        
        // 입장한 사람에게 성공 메시지 전송
        handler.sendMessage(new Message(Message.RES_JOIN_SUCCESS, roomId));
    }

    // 2. 유저 퇴장 처리
    public boolean exitUser(ClientHandler handler) {
        userList.remove(handler);
        readyState.remove(handler.getNickname());
        
        // 1. 남아있는 유저가 0명인 경우: 방 소멸 (true 반환)
        if (userList.size() == 0) {
            return true; 
        }
        
        //  남았는데 방장이 나갔으면 방장 승계
        if (handler.getNickname().equals(hostName)) {
            hostName = userList.get(0).getNickname(); // 다음 사람에게 방장 넘김
        }
        
        broadcastRoomState();
        
        return false;
    }

    // 3. 준비(Ready) 상태 토글
    public void toggleReady(ClientHandler handler) {
        String nick = handler.getNickname();
        // 방장은 준비 버튼이 없으므로(항상 준비/시작 권한) 패스하거나 별도 처리
        // 여기서는 방장도 준비 상태를 가질 수 있게 하거나, 방장은 무시하도록 구현
        boolean current = readyState.getOrDefault(nick, false);
        readyState.put(nick, !current); // true <-> false 반전
        
        broadcastRoomState();
    }

    // 4. 채팅 브로드캐스트
    public void broadcastChat(String msgText) {
        Message msg = new Message(Message.CHAT_MSG, msgText);
        for (ClientHandler user : userList) {
            user.sendMessage(msg);
        }
    }

    // 5. 방 상태(유저 목록, 준비 상태, 방장 정보)를 모두에게 전송
    public void broadcastRoomState() {
        // 형식: "닉네임:READY여부:방장여부" 문자열의 벡터
        Vector<String> userInfos = new Vector<>();
        
        for (ClientHandler user : userList) {
            String nick = user.getNickname();
            boolean isUserReady = readyState.getOrDefault(nick, false);
            String isReady = isUserReady ? "ON" : "OFF";
            String isHost = nick.equals(hostName) ? "HOST" : "MEMBER";
            
            userInfos.add(nick + ":" + isReady + ":" + isHost);
        }
        
        // ROOM_UPDATE 모드로 전송
        Message msg = new Message(Message.ROOM_UPDATE, "RoomUpdate", userInfos);
        for (ClientHandler user : userList) {
            user.sendMessage(msg);
        }
    }

    // JTable용 정보
    public Vector<String> getRoomInfo() {
        Vector<String> info = new Vector<>();
        info.add(String.valueOf(roomId));
        info.add(title);
        info.add(userList.size() + "/" + maxUser);
        info.add(isPlaying ? "게임중" : "대기");
        return info;
    }

    public int getRoomId() { return roomId; }
    public int getUserCount() { return userList.size(); }
}