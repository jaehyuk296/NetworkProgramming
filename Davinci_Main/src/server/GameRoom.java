package server;

import java.util.Vector;
import protocol.Message;

public class GameRoom {
    private int roomId;
    private String title;
    
    private RoomUserManager userManager;
    private DavinciGameLogic gameLogic;
    
    // [추가 1] 상태 변경 시 로비에 알리기 위해 Lobby 참조 변수 필요
    private Lobby lobby; 

    // [수정 2] 생성자에서 Lobby를 받도록 변경
    public GameRoom(int roomId, String title, ClientHandler host, Lobby lobby) {
        this.roomId = roomId;
        this.title = title;
        this.lobby = lobby; // 로비 저장
        
        this.userManager = new RoomUserManager(roomId);
        this.userManager.setHostName(host.getNickname());
        
        this.gameLogic = new DavinciGameLogic(userManager);
        
        // 생성자에서는 입장 검사가 필요 없으므로(방장이니까) 바로 입장
        userManager.enterUser(host);
        host.sendMessage(new Message(Message.RES_JOIN_SUCCESS, roomId));
    }

    // 유저 입장 시 게임 중인지 체크 (입장 차단 로직)
   public void enterUser(ClientHandler handler) {
    // 1. 이미 게임 중이면 입장 거부
    if (gameLogic.isPlaying()) {
        // '입장 실패' 프로토콜을 전송합니다.
        // Data1에 실패 사유(String)를 담아서 보냅니다.
        handler.sendMessage(new Message(Message.RES_JOIN_FAIL, "이미 게임이 진행 중인 방입니다."));
        return; 
    }
    
    // 2. 인원 가득 찼는지 체크
    if (userManager.getUserCount() >= userManager.getMaxUser()) {
        // [수정] 여기도 마찬가지로 팝업을 위해 변경
        handler.sendMessage(new Message(Message.RES_JOIN_FAIL, "방이 꽉 찼습니다."));
        return;
    }

    // 3. 통과 시 입장 처리 (기존 코드 유지)
    userManager.enterUser(handler);
    handler.sendMessage(new Message(Message.RES_JOIN_SUCCESS, roomId));
    
    if (lobby != null) lobby.broadcastRoomList();
}

    public boolean exitUser(ClientHandler handler) {
        boolean isEmpty = userManager.exitUser(handler);
        
        // [추가] 퇴장 시에도 인원수가 변하므로 로비 갱신
        if (!isEmpty) { // 방이 사라지는게 아니라면 갱신
            lobby.broadcastRoomList();
        }
        return isEmpty;
    }

    public void toggleReady(ClientHandler handler) {
        userManager.toggleReady(handler);
    }

    public void broadcastChat(String msg) {
        userManager.getBroadcaster().broadcastChat(msg);
    }

    // [수정 4] 게임 시작 시 로비 상태 갱신
    public void startGame() {
        gameLogic.startGame();
        
        // 게임이 시작되었으니 방 상태가 "대기" -> "게임중"으로 바뀜
        // 로비에 있는 모든 사람에게 방 목록을 다시 뿌려달라고 요청
        if (lobby != null) {
            lobby.broadcastRoomList();
        }
    }

    public void handleDraw(ClientHandler player, String data) {
        gameLogic.handleDraw(player, data);
    }

    public void handleGuess(ClientHandler guesser, String data) {
        gameLogic.handleGuess(guesser, data);
    }
    
    public void handleTimeout(ClientHandler player) {
    	gameLogic.handleTimeout(player);
    }
    
    // [추가] 턴 종료 위임 (아까 추가한 기능)
    public void handleEndTurn(ClientHandler player) {
        gameLogic.handleEndTurn(player);
    }

    public Vector<String> getRoomInfo() {
        Vector<String> info = new Vector<>();
        info.add(String.valueOf(roomId));
        info.add(title);
        info.add(userManager.getUserCount() + "/" + userManager.getMaxUser());
        // 여기서 isPlaying() 상태에 따라 텍스트가 바뀜 -> broadcastRoomList() 호출 시 적용됨
        info.add(gameLogic.isPlaying() ? "게임중" : "대기");
        return info;
    }

    public int getRoomId() { return roomId; }
    public int getUserCount() { return userManager.getUserCount(); }
}