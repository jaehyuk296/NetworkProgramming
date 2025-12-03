package server;

import java.util.Vector;
import protocol.Message;

public class Lobby {
    private Vector<ClientHandler> lobbyUsers = new Vector<>();
    private Vector<GameRoom> roomList = new Vector<>();
    private int roomCounter = 1;

    public synchronized void addUser(ClientHandler handler) {
        lobbyUsers.add(handler);
        // [중요] 유저 입장 시 현재 방 목록 전송
        sendRoomList(handler);
    }

    public synchronized void removeUser(ClientHandler handler) {
        lobbyUsers.remove(handler);
    }

    public synchronized void createRoom(ClientHandler host, String roomTitle) {
        GameRoom newRoom = new GameRoom(roomCounter++, roomTitle, host);
        roomList.add(newRoom);
        
        // 1. 방장 이동 처리
        host.setRoom(newRoom); 
        lobbyUsers.remove(host); 
        
        // 2. 방장에게 화면 전환 신호 전송
        host.sendMessage(new Message(Message.RES_CREATE_SUCCESS, newRoom.getRoomId()));
        
        // ★ [핵심] 로비에 남은 사람들에게 "새 방 생겼다!"고 알림
        broadcastRoomList();
        
        // 서버 GUI 연동
        if (GameServer.instance != null) {
            GameServer.instance.addRoomTab(newRoom.getRoomId(), roomTitle);
        }
    }
    
    public synchronized void joinRoom(ClientHandler user, int roomId) {
        GameRoom room = getRoom(roomId);
        if (room != null && room.getUserCount() < 4) {
            lobbyUsers.remove(user);
            user.setRoom(room);
            room.enterUser(user);
            
            // ★ [핵심] 인원 수 변동이 있으므로 로비 갱신
            broadcastRoomList();
        } else {
            user.sendMessage(new Message(Message.CHAT_MSG, "[System] 입장 실패"));
        }
    }

    public synchronized void removeRoom(int roomId) {
        GameRoom roomToRemove = null;
        for (GameRoom room : roomList) {
            if (room.getRoomId() == roomId) {
                roomToRemove = room;
                break;
            }
        }
        
        if (roomToRemove != null) {
            roomList.remove(roomToRemove);
            System.out.println("[LOBBY] 방 삭제됨: " + roomId);
            
            // ★ [핵심] 방 삭제되었으니 로비 갱신
            broadcastRoomList();
            
            if (GameServer.instance != null) {
                GameServer.instance.removeRoomTab(roomId);
            }
        }
    }
    
    // 특정 유저에게만 방 목록 전송
    public void sendRoomList(ClientHandler handler) {
        Vector<Vector<String>> roomData = new Vector<>();
        for (GameRoom room : roomList) {
            roomData.add(room.getRoomInfo());
        }
        handler.sendMessage(new Message(Message.SRES_ROOM_LIST, "RoomList", roomData));
    }

    // 로비 전체 유저에게 방 목록 전송
    public void broadcastRoomList() {
        Vector<Vector<String>> roomData = new Vector<>();
        for (GameRoom room : roomList) {
            roomData.add(room.getRoomInfo());
        }
        
        // Message(mode, data1, payload)
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