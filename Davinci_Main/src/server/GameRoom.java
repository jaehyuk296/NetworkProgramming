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

    // [추가] 게임용 변수
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

    // 1. 유저 입장 처리
    public void enterUser(ClientHandler handler) {
        userList.add(handler);
        readyState.put(handler.getNickname(), false); // 기본값: 준비 안됨
        
        broadcastRoomState();
        
        // 입장한 사람에게 성공 메시지 전송
        handler.sendMessage(new Message(Message.RES_JOIN_SUCCESS, roomId));
    }

    // 2. 유저 퇴장 처리
    public boolean exitUser(ClientHandler handler) {
        userList.remove(handler);
        readyState.remove(handler.getNickname());
        
        // 1. 남아있는 유저가 0명인 경우: 방 소멸 (true 반환)
        if (userList.size() == 0) {
            return true; 
        }
        
        //  남았는데 방장이 나갔으면 방장 승계
        if (handler.getNickname().equals(hostName)) {
            hostName = userList.get(0).getNickname(); // 다음 사람에게 방장 넘김
        }
        
        broadcastRoomState();
        
        return false;
    }

    // 3. 준비(Ready) 상태 토글
    public void toggleReady(ClientHandler handler) {
        String nick = handler.getNickname();
        // 방장은 준비 버튼이 없으므로(항상 준비/시작 권한) 패스하거나 별도 처리
        // 여기서는 방장도 준비 상태를 가질 수 있게 하거나, 방장은 무시하도록 구현
        boolean current = readyState.getOrDefault(nick, false);
        readyState.put(nick, !current); // true <-> false 반전
        
        broadcastRoomState();
    }

    // 4. 채팅 브로드캐스트
    public void broadcastChat(String msgText) {
        Message msg = new Message(Message.CHAT_MSG, msgText);
        for (ClientHandler user : userList) {
            user.sendMessage(msg);
        }
    }

    // [추가] 모든 유저에게 일반 메시지 객체 전송 (범용 메서드)
    private void broadcast(Message msg) {
        for (ClientHandler user : userList) {
            user.sendMessage(msg);
        }
    }

    // 5. 방 상태(유저 목록, 준비 상태, 방장 정보)를 모두에게 전송
    public void broadcastRoomState() {
        // 형식: "닉네임:READY여부:방장여부" 문자열의 벡터
        Vector<String> userInfos = new Vector<>();
        
        for (ClientHandler user : userList) {
            String nick = user.getNickname();
            boolean isUserReady = readyState.getOrDefault(nick, false);
            String isReady = isUserReady ? "ON" : "OFF";
            String isHost = nick.equals(hostName) ? "HOST" : "MEMBER";
            
            userInfos.add(nick + ":" + isReady + ":" + isHost);
        }
        
        // ROOM_UPDATE 모드로 전송
        Message msg = new Message(Message.ROOM_UPDATE, "RoomUpdate", userInfos);
        for (ClientHandler user : userList) {
            user.sendMessage(msg);
        }
    }

    // [추가] 게임 시작 로직 (방장이 버튼 누르면 호출됨)
    public void startGame() {
        // 1. 인원 부족 체크 (2명 미만이면 시작 불가)
        if (userList.size() < 2) {
            // 방장에게 경고 메시지 전송 (CHAT_MSG로 보내면 채팅창에 뜸)
            // 방장이 누구인지(index 0 또는 hostName) 찾아서 보냄
            for (ClientHandler user : userList) {
                if (user.getNickname().equals(hostName)) {
                    user.sendMessage(new Message(Message.CHAT_MSG, "[System] 최소 2명 이상이어야 게임을 시작할 수 있습니다."));
                    break;
                }
            }
            return; 
        }
        
        this.isPlaying = true;
        
        // 1. 덱 생성 및 셔플
        initializeDeck();
        
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
            
            // 타일 정렬 (Tile.java의 compareTo 사용)
            Collections.sort(myTiles);
            
            // 3. 각 유저에게 "GAME_START" 메시지와 "자기 패" 전송
            // Data1: 내 타일 리스트, Data2: 전체 플레이어 순서(닉네임 리스트)
            Vector<String> playerOrder = getPlayerNames();
            user.sendMessage(new Message(Message.GAME_START, myTiles, playerOrder));
        }
        
        // 4. 첫 번째 턴 지정 및 알림
        turnIndex = 0;
        notifyTurn();
    }

    // [내부] 덱 초기화 (검/흰 0~11 + 조커)
    private void initializeDeck() {
        deck.clear();
        // 0~11 숫자 타일
        for (int i = 0; i <= 11; i++) {
            deck.add(new Tile("BLACK", i));
            deck.add(new Tile("WHITE", i));
        }
        // 조커 타일 (-1)
        deck.add(new Tile("BLACK", -1));
        deck.add(new Tile("WHITE", -1));
        
        // 섞기
        Collections.shuffle(deck);
    }
    
    // [내부] 플레이어 이름 목록 추출 (순서 보장용)
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
        
        System.out.println("[GAME] 턴 변경: 현재 턴 -> " + currentTurnUser);
        
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