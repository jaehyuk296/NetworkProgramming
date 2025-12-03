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
        
        host.setRoom(newRoom); 
        lobbyUsers.remove(host); 
        
        // 방장에게 화면 전환 신호 전송
        host.sendMessage(new Message(Message.RES_CREATE_SUCCESS, newRoom.getRoomId()));
        
        broadcastRoomList(); // 로비 갱신
    }
    
    public synchronized void joinRoom(ClientHandler user, int roomId) {
        GameRoom room = getRoom(roomId);
        if (room != null && room.getUserCount() < 4) {
            lobbyUsers.remove(user);
            user.setRoom(room);
            room.enterUser(user);
        } else {
            // 실패 처리 (생략 가능)
        }
    }

    public synchronized void removeRoom(int roomId) {
        roomList.removeIf(r -> r.getRoomId() == roomId);
        System.out.println("[LOBBY] 방 삭제됨: " + roomId);
        broadcastRoomList();
    }
    
    public void sendRoomList(ClientHandler handler) {
        Vector<Vector<String>> roomData = new Vector<>();
        for (GameRoom room : roomList) roomData.add(room.getRoomInfo());
        handler.sendMessage(new Message(Message.SRES_ROOM_LIST, roomData));
    }

    public void broadcastRoomList() {
        Vector<Vector<String>> roomData = new Vector<>();
        for (GameRoom room : roomList) roomData.add(room.getRoomInfo());
        Message msg = new Message(Message.SRES_ROOM_LIST, roomData);
        
        for (ClientHandler user : lobbyUsers) {
            user.sendMessage(msg);
        }
    }
    
    public GameRoom getRoom(int roomId) {
        for(GameRoom r : roomList) {
            if(r.getRoomId() == roomId) return r;
        }
        return null;
    }
}