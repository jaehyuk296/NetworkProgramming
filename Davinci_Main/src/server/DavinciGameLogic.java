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

    public DavinciGameLogic(RoomUserManager userManager) {
        this.userManager = userManager;
        this.broadcaster = userManager.getBroadcaster();
    }

    public boolean isPlaying() { return isPlaying; }

    public void startGame() {
        Vector<ClientHandler> users = userManager.getUserList();
        
        if (users.size() < 2) {
            broadcaster.broadcastChat("[System] 최소 2명 이상이어야 합니다.");
            return;
        }

        isPlaying = true;
        broadcaster.roomLog("=== 게임 시작 ===");
        
        initializeDeck();
        userHands.clear();

        for (ClientHandler user : users) {
            Vector<Tile> hand = new Vector<>();
            for (int k = 0; k < 4; k++) {
                if (!deck.isEmpty()) hand.add(deck.pop());
            }
            Collections.sort(hand);
            userHands.put(user.getNickname(), hand);
            
            Vector<String> names = new Vector<>();
            for(ClientHandler u : users) names.add(u.getNickname());
            
            user.sendMessage(new Message(Message.GAME_START, hand, names));
            broadcaster.roomLog(" -> " + user.getNickname() + "에게 타일 분배 완료");
        }

        turnIndex = 0;
        currentTurnDrawnTile = null;
        notifyTurn();
    }

    public void handleDraw(ClientHandler player, String data) {
        Vector<ClientHandler> userList = userManager.getUserList();
        ClientHandler currentTurnUser = userList.get(turnIndex);
        
        if (!player.equals(currentTurnUser)) return;

        if (currentTurnDrawnTile != null) {
            player.sendMessage(new Message(Message.CHAT_MSG, "[System] 이미 타일을 뽑았습니다."));
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

            player.sendMessage(new Message(Message.RES_DRAW, drawn, insertIndex));
            
            broadcaster.broadcastChat("[System] " + player.getNickname() + "님이 " + color + " 타일을 가져갔습니다.");
            broadcaster.roomLog(player.getNickname() + "님이 " + color + " 타일 드로우");
        } else {
            player.sendMessage(new Message(Message.CHAT_MSG, "[System] 해당 색상 타일이 없습니다."));
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
            broadcaster.roomLog(guesser.getNickname() + " 추리 성공 -> " + targetID + "의 타일 공개");
            
            String revealInfo = targetID + ":" + targetIndex;
            broadcaster.broadcast(new Message(Message.BCAST_REVEAL, revealInfo, actualTile));
            
        } else {
            broadcaster.broadcastChat("[System] " + guesser.getNickname() + "님이 틀렸습니다!");
            broadcaster.roomLog(guesser.getNickname() + " 추리 실패");
            
            if (currentTurnDrawnTile != null) {
                currentTurnDrawnTile.setRevealed(true);
                Vector<Tile> myHand = userHands.get(guesser.getNickname());
                int myIndex = myHand.indexOf(currentTurnDrawnTile);
                
                String myInfo = guesser.getNickname() + ":" + myIndex;
                broadcaster.broadcast(new Message(Message.BCAST_REVEAL, myInfo, currentTurnDrawnTile));
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
        
        broadcaster.roomLog("턴 변경: 현재 턴 -> " + current);
        broadcaster.broadcast(new Message(Message.TURN_START, current));
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