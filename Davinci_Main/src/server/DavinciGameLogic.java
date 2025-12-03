package server;

import java.util.*;
import protocol.Message;
import protocol.Tile;

public class DavinciGameLogic {
    private RoomUserManager userManager;
    private RoomBroadcaster broadcaster;
    
    private Stack<Tile> deck = new Stack<>();
    private Map<String, Vector<Tile>> userHands = new HashMap<>();
    private Tile currentTurnDrawnTile = null;
    private int turnIndex = 0;
    private boolean isPlaying = false;

    public DavinciGameLogic(RoomUserManager manager, RoomBroadcaster broadcaster) {
        this.userManager = manager;
        this.broadcaster = broadcaster;
    }

    public boolean isPlaying() { return isPlaying; }

    public void startGame() {
        Vector<ClientHandler> users = userManager.getUserList();
        if (users.size() < 2) {
            broadcaster.broadcastChat("[System] 인원이 부족합니다.");
            return;
        }

        isPlaying = true;
        broadcaster.log("=== 게임 시작 ===");
        
        initializeDeck();
        userHands.clear();

        for (ClientHandler user : users) {
            Vector<Tile> hand = new Vector<>();
            for (int k = 0; k < 4; k++) {
                if (!deck.isEmpty()) hand.add(deck.pop());
            }
            Collections.sort(hand);
            userHands.put(user.getNickname(), hand);
            
            // 이름 목록
            Vector<String> names = new Vector<>();
            for(ClientHandler u : users) names.add(u.getNickname());
            
            // data1: 내패, data2: 이름목록
            user.sendMessage(new Message(Message.GAME_START, hand, names));
        }

        turnIndex = 0;
        currentTurnDrawnTile = null;
        notifyTurn();
    }

    public void handleDraw(ClientHandler player, String data) {
        Vector<ClientHandler> users = userManager.getUserList();
        ClientHandler currentTurnUser = users.get(turnIndex);
        if (!player.equals(currentTurnUser)) return;

        if (currentTurnDrawnTile != null) {
            player.sendMessage(new Message(Message.CHAT_MSG, "[System] 이미 뽑았습니다."));
            return;
        }

        String[] parts = data.split(":");
        String color = parts[0];
        int insertIndex = Integer.parseInt(parts[1]);

        Tile drawn = null;
        for (int i = deck.size() - 1; i >= 0; i--) {
            if (deck.get(i).getColor().equals(color)) {
                drawn = deck.remove(i);
                break;
            }
        }

        if (drawn != null) {
            Vector<Tile> hand = userHands.get(player.getNickname());
            if (insertIndex > hand.size()) insertIndex = hand.size();
            hand.add(insertIndex, drawn);
            
            this.currentTurnDrawnTile = drawn;
            
            player.sendMessage(new Message(Message.RES_DRAW, drawn, insertIndex, null));
            broadcaster.broadcastChat("[System] " + player.getNickname() + "님이 " + color + " 타일을 가져갔습니다.");
        }
    }

    public void handleGuess(ClientHandler guesser, String data) {
        String[] parts = data.split(":");
        String targetID = parts[0];
        int targetIndex = Integer.parseInt(parts[1]);
        String guessValue = parts[2];

        Vector<Tile> targetHand = userHands.get(targetID);
        Tile actualTile = targetHand.get(targetIndex);

        boolean isCorrect = false;
        if (actualTile.isJoker()) {
             if(guessValue.equals("-1") || guessValue.equals("JOKER")) isCorrect = true;
        } else {
             if(guessValue.equals(String.valueOf(actualTile.getNumber()))) isCorrect = true;
        }

        if (isCorrect) {
            actualTile.setRevealed(true);
            broadcaster.broadcastChat("[System] " + guesser.getNickname() + "님이 맞췄습니다!");
            
            // data1: "ID:Index", data2: Tile
            String revealInfo = targetID + ":" + targetIndex;
            broadcaster.broadcast(new Message(Message.BCAST_REVEAL, revealInfo, actualTile, null));
            
        } else {
            broadcaster.broadcastChat("[System] " + guesser.getNickname() + "님이 틀렸습니다!");
            
            if (currentTurnDrawnTile != null) {
                currentTurnDrawnTile.setRevealed(true);
                Vector<Tile> myHand = userHands.get(guesser.getNickname());
                int myIndex = myHand.indexOf(currentTurnDrawnTile);
                
                String myInfo = guesser.getNickname() + ":" + myIndex;
                broadcaster.broadcast(new Message(Message.BCAST_REVEAL, myInfo, currentTurnDrawnTile, null));
            }
            nextTurn();
        }
    }

    private void nextTurn() {
        currentTurnDrawnTile = null;
        turnIndex = (turnIndex + 1) % userManager.getUserList().size();
        notifyTurn();
    }

    private void notifyTurn() {
        Vector<ClientHandler> users = userManager.getUserList();
        if (users.isEmpty()) return;
        String current = users.get(turnIndex).getNickname();
        broadcaster.broadcast(new Message(Message.TURN_START, current));
        broadcaster.log("현재 턴: " + current);
    }

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
}