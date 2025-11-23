package server;

import java.util.Vector;
import protocol.Message;

public class GameRoom {
    private int roomId;
    private String title;
    private String hostName;
    private int maxUser = 4;
    private boolean isPlaying = false;

    private Vector<ClientHandler> userList = new Vector<>();

    public GameRoom(int roomId, String title, ClientHandler host) {
        this.roomId = roomId;
        this.title = title;
        this.hostName = host.getNickname();
        this.userList.add(host);
    }

    public void enterUser(ClientHandler handler) {
        userList.add(handler);
    }

    public void exitUser(ClientHandler handler) {
        userList.remove(handler);
        if (userList.size() > 0 && handler.getNickname().equals(hostName)) {
            hostName = userList.get(0).getNickname();
        }
    }

    public void broadcast(Message msg) {
        for (ClientHandler user : userList) {
            user.sendMessage(msg);
        }
    }

    public Vector<String> getRoomInfo() {
        Vector<String> info = new Vector<>();
        info.add(String.valueOf(roomId));
        info.add(title);
        info.add(userList.size() + "/" + maxUser);
        info.add(isPlaying ? "게임중" : "대기");
        return info;
    }

    public int getRoomId() { return roomId; }
}