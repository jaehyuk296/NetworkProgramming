package server;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import protocol.Message;

public class RoomUserManager {
    private Vector<ClientHandler> userList = new Vector<>();
    private Map<String, Boolean> readyState = new HashMap<>();
    private String hostName;
    private int roomId;
    private int maxUser = 4; // 필드 선언 확인
    
    private RoomBroadcaster broadcaster;

    public RoomUserManager(int roomId) {
        this.roomId = roomId;
        this.broadcaster = new RoomBroadcaster(userList, roomId);
    }

    // Getter & Setter
    public void setHostName(String name) { this.hostName = name; }
    public String getHostName() { return hostName; }
    public Vector<ClientHandler> getUserList() { return userList; }
    public RoomBroadcaster getBroadcaster() { return broadcaster; }
    public int getUserCount() { return userList.size(); }
    
    public int getMaxUser() { return maxUser; }

    public void enterUser(ClientHandler handler) {
        userList.add(handler);
        readyState.put(handler.getNickname(), false);
        
        broadcastRoomState();
        
        handler.sendMessage(new Message(Message.RES_JOIN_SUCCESS, roomId));
        
        broadcaster.roomLog(handler.getNickname() + "님이 입장했습니다. (현재 " + userList.size() + "명)");
    }

    public boolean exitUser(ClientHandler handler) {
        userList.remove(handler);
        readyState.remove(handler.getNickname());
        
        broadcaster.roomLog(handler.getNickname() + "님이 퇴장했습니다.");

        if (userList.size() == 0) return true;

        if (handler.getNickname().equals(hostName)) {
            hostName = userList.get(0).getNickname();
            broadcaster.roomLog("방장이 변경되었습니다: " + hostName);
        }
        
        broadcastRoomState();
        return false;
    }

    public void toggleReady(ClientHandler handler) {
        String nick = handler.getNickname();
        if (!nick.equals(hostName)) {
            boolean current = readyState.getOrDefault(nick, false);
            readyState.put(nick, !current);
            broadcastRoomState();
        }
    }

    public void broadcastRoomState() {
        Vector<String> userInfos = new Vector<>();
        
        for (ClientHandler user : userList) {
            String nick = user.getNickname();
            boolean isUserReady = readyState.getOrDefault(nick, false);
            String isReady = isUserReady ? "ON" : "OFF";
            String isHost = nick.equals(hostName) ? "HOST" : "MEMBER";
            
            userInfos.add(nick + ":" + isReady + ":" + isHost);
        }
        
        broadcaster.broadcast(new Message(Message.ROOM_UPDATE, userInfos, hostName));
    }
}