package server;

import java.util.Vector;
import protocol.Message;

public class GameRoom {
    private int roomId;
    private String title;
    
    private RoomUserManager userManager;
    private DavinciGameLogic gameLogic;

    public GameRoom(int roomId, String title, ClientHandler host) {
        this.roomId = roomId;
        this.title = title;
        
        this.userManager = new RoomUserManager(roomId);
        this.userManager.setHostName(host.getNickname());
        
        this.gameLogic = new DavinciGameLogic(userManager);
        
        enterUser(host);
    }

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
        userManager.getBroadcaster().broadcastChat(msg);
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

    public Vector<String> getRoomInfo() {
        Vector<String> info = new Vector<>();
        info.add(String.valueOf(roomId));
        info.add(title);
        info.add(userManager.getUserCount() + "/" + userManager.getMaxUser());
        info.add(gameLogic.isPlaying() ? "게임중" : "대기");
        return info;
    }

    public int getRoomId() { return roomId; }
    public int getUserCount() { return userManager.getUserCount(); }
}