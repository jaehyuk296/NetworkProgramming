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

    public void log(String msg) {
        System.out.println("[Room " + roomId + "] " + msg);
    }

    public void broadcast(Message msg) {
        for (ClientHandler user : userList) {
            user.sendMessage(msg);
        }
    }

    public void broadcastChat(String msgText) {
        broadcast(new Message(Message.CHAT_MSG, msgText));
        log("[CHAT] " + msgText);
    }
}   