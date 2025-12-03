package server;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.Collections;
import java.util.Stack;
import protocol.Tile;    
import protocol.Message;

public class GameRoom {
    private int roomId;
    private String title;
    private String hostName; // 방장 닉네임
    private int maxUser = 4;
    private boolean isPlaying = false;

    // 유저 리스트 (통신용)
    private Vector<ClientHandler> userList = new Vector<>();

    // 게임용 변수
    private Stack<Tile> deck = new Stack<>(); // 남은 타일 더미
    private int turnIndex = 0; // 현재 누구 턴인지 (0 ~ userList.size()-1)
    
    // 준비 상태 관리 (닉네임 -> 준비여부)
    private Map<String, Boolean> readyState = new HashMap<>();

    public GameRoom(int roomId, String title, ClientHandler host) {
        this.roomId = roomId;
        this.title = title;
        this.hostName = host.getNickname();
        enterUser(host);
    }

    // ★ [추가] 로그 기록용 헬퍼 메서드 (메인 로그 + 방 탭 로그)
    private void roomLog(String msg) {
        // 1. 콘솔(메인 로그)에 출력
        System.out.println("[Room " + roomId + "] " + msg);
        
        // 2. 서버 GUI의 해당 방 탭에 출력
        if (GameServer.instance != null) {
            GameServer.instance.appendRoomLog(roomId, msg);
        }
    }

    // 1. 유저 입장 처리
    public void enterUser(ClientHandler handler) {
        userList.add(handler);
        readyState.put(handler.getNickname(), false); // 기본값: 준비 안됨
        
        broadcastRoomState();
        
        handler.sendMessage(new Message(Message.RES_JOIN_SUCCESS, roomId));
        
        roomLog(handler.getNickname() + "님이 입장했습니다. (현재 " + userList.size() + "명)");
    }

    // 2. 유저 퇴장 처리
    public boolean exitUser(ClientHandler handler) {
        userList.remove(handler);
        readyState.remove(handler.getNickname());
        
        roomLog(handler.getNickname() + "님이 퇴장했습니다.");

        // 1. 남아있는 유저가 0명인 경우: 방 소멸 (true 반환)
        if (userList.size() == 0) {
            return true; 
        }
        
        // 남았는데 방장이 나갔으면 방장 승계
        if (handler.getNickname().equals(hostName)) {
            hostName = userList.get(0).getNickname(); // 다음 사람에게 방장 넘김
            roomLog("방장이 변경되었습니다: " + hostName);
        }
        
        broadcastRoomState();
        
        return false;
    }

    // 3. 준비(Ready) 상태 토글
    public void toggleReady(ClientHandler handler) {
        String nick = handler.getNickname();
        boolean current = readyState.getOrDefault(nick, false);
        readyState.put(nick, !current); 
        
        broadcastRoomState();
    }

    // 4. 채팅 브로드캐스트
    public void broadcastChat(String msgText) {
        // 1. 클라이언트들에게 전송 (기존 로직)
        Message msg = new Message(Message.CHAT_MSG, msgText);
        for (ClientHandler user : userList) {
            user.sendMessage(msg);
        }

        // 2. 서버의 해당 방 탭(Room X)에도 채팅 기록 남기기
        roomLog("[CHAT] " + msgText);
    }

    // [추가] 모든 유저에게 일반 메시지 객체 전송 (범용 메서드)
    private void broadcast(Message msg) {
        for (ClientHandler user : userList) {
            user.sendMessage(msg);
        }
    }

    // 5. 방 상태(유저 목록, 준비 상태, 방장 정보)를 모두에게 전송
    public void broadcastRoomState() {
        Vector<String> userInfos = new Vector<>();
        
        for (ClientHandler user : userList) {
            String nick = user.getNickname();
            boolean isUserReady = readyState.getOrDefault(nick, false);
            String isReady = isUserReady ? "ON" : "OFF";
            String isHost = nick.equals(hostName) ? "HOST" : "MEMBER";
            
            userInfos.add(nick + ":" + isReady + ":" + isHost);
        }
        
        Message msg = new Message(Message.ROOM_UPDATE, "RoomUpdate", userInfos);
        broadcast(msg);
    }

    // [추가] 게임 시작 로직 (방장이 버튼 누르면 호출됨)
    public void startGame() {
        // 1. 인원 부족 체크
        if (userList.size() < 2) {
            for (ClientHandler user : userList) {
                if (user.getNickname().equals(hostName)) {
                    user.sendMessage(new Message(Message.CHAT_MSG, "[System] 최소 2명 이상이어야 게임을 시작할 수 있습니다."));
                    break;
                }
            }
            return; 
        }
        
        this.isPlaying = true;
        roomLog("게임 시작! (인원: " + userList.size() + "명)");
        
        // 1. 덱 생성 및 셔플
        initializeDeck();
        roomLog("덱 생성 및 셔플 완료 (총 " + deck.size() + "장)");
        
        // 2. 타일 분배 및 시작 메시지 전송
        for (int i = 0; i < userList.size(); i++) {
            ClientHandler user = userList.get(i);
            
            // 각자에게 4개씩 뽑아줌
            Vector<Tile> myTiles = new Vector<>();
            for (int k = 0; k < 4; k++) {
                if (!deck.isEmpty()) {
                    myTiles.add(deck.pop());
                }
            }
            
            // 타일 정렬
            Collections.sort(myTiles);
            
            // 3. 각 유저에게 전송
            Vector<String> playerOrder = getPlayerNames();
            user.sendMessage(new Message(Message.GAME_START, myTiles, playerOrder));
            
            roomLog(" -> " + user.getNickname() + "에게 타일 분배: " + myTiles.size() + "개");
        }
        
        // 4. 첫 번째 턴 지정 및 알림
        turnIndex = 0;
        notifyTurn();
    }

    // [내부] 덱 초기화
    private void initializeDeck() {
        deck.clear();
        for (int i = 0; i <= 11; i++) {
            deck.add(new Tile("BLACK", i));
            deck.add(new Tile("WHITE", i));
        }
        deck.add(new Tile("BLACK", -1));
        deck.add(new Tile("WHITE", -1));
        
        Collections.shuffle(deck);
    }
    
    // [내부] 플레이어 이름 목록 추출
    private Vector<String> getPlayerNames() {
        Vector<String> names = new Vector<>();
        for (ClientHandler user : userList) {
            names.add(user.getNickname());
        }
        return names;
    }

    // [내부] 현재 턴인 사람 알림
    private void notifyTurn() {
        if (userList.isEmpty()) return;
        
        String currentTurnUser = userList.get(turnIndex).getNickname();
        
        roomLog("턴 변경: 현재 턴 -> " + currentTurnUser + " (남은 덱: " + deck.size() + ")");
        
        Message turnMsg = new Message(Message.TURN_START, currentTurnUser);
        broadcast(turnMsg); 
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