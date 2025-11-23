package server;

import java.util.Vector;
import protocol.Message;

public class Lobby {
    private Vector<ClientHandler> lobbyUsers = new Vector<>();
    private Vector<GameRoom> roomList = new Vector<>();
    private int roomCounter = 1;

    public synchronized void addUser(ClientHandler handler) {
        lobbyUsers.add(handler);
        sendRoomList(handler);
    }

    public synchronized void removeUser(ClientHandler handler) {
        lobbyUsers.remove(handler);
    }

    public synchronized void createRoom(ClientHandler host, String roomTitle) {
        GameRoom newRoom = new GameRoom(roomCounter++, roomTitle, host);
        roomList.add(newRoom);
        
        host.sendMessage(new Message(Message.RES_CREATE_SUCCESS, newRoom.getRoomId()));
        broadcastRoomList();
    }
    
    public void sendRoomList(ClientHandler handler) {
        Vector<Vector<String>> roomData = new Vector<>();
        for (GameRoom room : roomList) {
            roomData.add(room.getRoomInfo());
        }
        
        handler.sendMessage(new Message(Message.SRES_ROOM_LIST, "RoomList", roomData));
    }

    public void broadcastRoomList() {
        Vector<Vector<String>> roomData = new Vector<>();
        for (GameRoom room : roomList) {
            roomData.add(room.getRoomInfo());
        }
        
        Message msg = new Message(Message.SRES_ROOM_LIST, "RoomList", roomData);
        
        for (ClientHandler user : lobbyUsers) {
            user.sendMessage(msg);
        }
    }
    
    public synchronized GameRoom getRoom(int roomId) {
        for(GameRoom r : roomList) {
            if(r.getRoomId() == roomId) return r;
        }
        return null;
    }
}