package server;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import protocol.Message;

public class RoomUserManager {
    private Vector<ClientHandler> userList = new Vector<>();
    private Map<String, Boolean> readyState = new HashMap<>();
    private String hostName;
    private RoomBroadcaster broadcaster;

    public RoomUserManager(int roomId) {
        this.broadcaster = new RoomBroadcaster(userList, roomId);
    }

    public void setHost(String name) { this.hostName = name; }
    public Vector<ClientHandler> getUserList() { return userList; }
    public RoomBroadcaster getBroadcaster() { return broadcaster; }
    public int getUserCount() { return userList.size(); }

    public void enterUser(ClientHandler handler) {
        userList.add(handler);
        readyState.put(handler.getNickname(), false);
        broadcaster.log(handler.getNickname() + " 입장");
        broadcastRoomState();
    }

    public boolean exitUser(ClientHandler handler) {
        userList.remove(handler);
        readyState.remove(handler.getNickname());
        broadcaster.log(handler.getNickname() + " 퇴장");

        if (userList.isEmpty()) return true;

        if (handler.getNickname().equals(hostName)) {
            hostName = userList.get(0).getNickname();
            broadcaster.broadcastChat("[System] 방장이 " + hostName + "님으로 변경되었습니다.");
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
            String isReady = readyState.getOrDefault(nick, false) ? "ON" : "OFF";
            String role = nick.equals(hostName) ? "HOST" : "MEMBER";
            userInfos.add(nick + ":" + isReady + ":" + role);
        }
        // Message 생성자: (mode, data1, data2, data3)
        // data1: List, data2: HostName, data3: null
        broadcaster.broadcast(new Message(Message.ROOM_UPDATE, userInfos, hostName, null));
    }
}