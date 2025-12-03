package server;

import java.util.Vector;
import protocol.Message;

public class RoomBroadcaster {
    private Vector<ClientHandler> userList;
    private int roomId;

    public RoomBroadcaster(Vector<ClientHandler> userList, int roomId) {
        this.userList = userList;
        this.roomId = roomId;
    }

    // 메소드 이름을 roomLog로 통일 + [GAME] 태그 자동 추가
    public void roomLog(String msg) {
        String logMsg = msg;
        
        // 채팅이나 이미 태그가 있는 경우가 아니면 [GAME] 태그 붙이기
        if (!msg.startsWith("[") && !msg.startsWith("===")) {
             logMsg = "[GAME] " + msg;
        }

        System.out.println("[Room " + roomId + "] " + logMsg);
        
        if (GameServer.instance != null) {
            GameServer.instance.appendRoomLog(roomId, logMsg);
        }
    }

    public void broadcast(Message msg) {
        for (ClientHandler user : userList) {
            user.sendMessage(msg);
        }
    }

    public void broadcastChat(String msgText) {
        broadcast(new Message(Message.CHAT_MSG, msgText));
        String chatLog = "[CHAT] " + msgText;
        System.out.println("[Room " + roomId + "] " + chatLog);
        
        if (GameServer.instance != null) {
            GameServer.instance.appendRoomLog(roomId, chatLog);
        }
    }
}