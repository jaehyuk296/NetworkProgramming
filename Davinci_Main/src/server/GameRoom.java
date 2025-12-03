package server;

import java.util.Vector;
import protocol.Message;

public class GameRoom {
    private int roomId;
    private String title;
    
    // 분리된 컴포넌트들
    private RoomUserManager userManager;
    private RoomBroadcaster broadcaster;
    private DavinciGameLogic gameLogic;

    public GameRoom(int roomId, String title, ClientHandler host) {
        this.roomId = roomId;
        this.title = title;
        
        // 1. 매니저 & 브로드캐스터 생성
        this.userManager = new RoomUserManager(roomId);
        this.broadcaster = userManager.getBroadcaster();
        
        // 2. 게임 로직 생성
        this.gameLogic = new DavinciGameLogic(userManager, broadcaster);
        
        // 3. 방장 입장 처리
        userManager.setHost(host.getNickname());
        enterUser(host);
    }

    // --- 위임 메서드 (Handler -> Room -> Components) ---

    public void enterUser(ClientHandler handler) {
        userManager.enterUser(handler);
        handler.sendMessage(new Message(Message.RES_JOIN_SUCCESS, roomId));
    }

    public boolean exitUser(ClientHandler handler) {
        return userManager.exitUser(handler);
    }

    public void toggleReady(ClientHandler handler) {
        userManager.toggleReady(handler);
    }

    public void broadcastChat(String msg) {
        broadcaster.broadcastChat(msg);
    }

    public void startGame() {
        gameLogic.startGame();
    }

    public void handleDraw(ClientHandler player, String data) {
        gameLogic.handleDraw(player, data);
    }

    public void handleGuess(ClientHandler guesser, String data) {
        gameLogic.handleGuess(guesser, data);
    }

    // 로비에 보여줄 정보
    public Vector<String> getRoomInfo() {
        Vector<String> info = new Vector<>();
        info.add(String.valueOf(roomId));
        info.add(title);
        info.add(userManager.getUserCount() + "/4");
        info.add(gameLogic.isPlaying() ? "게임중" : "대기");
        return info;
    }

    public int getRoomId() { return roomId; }
    public int getUserCount() { return userManager.getUserCount(); }
}