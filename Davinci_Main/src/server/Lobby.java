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
        // 1. 방장의 currentRoom 설정 (GameRoom 참조 주입)
        host.setRoom(newRoom); 
        
        // 2. 로비 유저 목록에서 방장을 제거 (로비 채팅과 방 채팅 분리)
        lobbyUsers.remove(host); 
        
        // 로비에 남아있는 유저들에게 방 목록 갱신
        broadcastRoomList();
    }
    
    public synchronized void removeRoom(int roomId) {
        System.out.println("[LOBBY] 방 폭파 요청: ID " + roomId);
        
        // 1. roomId를 사용하여 roomList에서 해당 GameRoom 객체를 찾아서 제거
        GameRoom roomToRemove = null;
        
        // Vector를 순회하며 제거할 방을 찾습니다.
        for (GameRoom room : roomList) {
            if (room.getRoomId() == roomId) {
                roomToRemove = room;
                break;
            }
        }
        
        if (roomToRemove != null) {
            // 방을 Vector에서 제거합니다.
            roomList.remove(roomToRemove);
            
            System.out.println("[LOBBY] 방 " + roomToRemove.getRoomId() + " (ID: " + roomId + ") 이(가) 목록에서 제거되었습니다.");
            
            // 2. 모든 로비 유저에게 갱신된 방 목록을 브로드캐스트
            broadcastRoomList();
            
        } else {
            System.err.println("[LOBBY] 경고: 존재하지 않는 방 ID (" + roomId + ") 제거 요청을 받았습니다.");
        }
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