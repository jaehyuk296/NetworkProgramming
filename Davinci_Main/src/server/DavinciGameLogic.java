package server;

import java.util.*;
import protocol.Message;
import protocol.Tile;

public class DavinciGameLogic {
    private RoomUserManager userManager;
    private RoomBroadcaster broadcaster;
    
    // 게임 데이터
    private Stack<Tile> deck = new Stack<>();
    private Map<String, Vector<Tile>> userHands = new HashMap<>(); 
    private Tile currentTurnDrawnTile = null; // 이번 턴에 뽑은 타일 (공개 패널티용)
    private int turnIndex = 0;
    private boolean isPlaying = false;
    
    // [추가] 현재 턴인 사람이 추리에 성공했는지 여부 (성공했다면 턴 종료 시 패널티 없음)
    private boolean hasGuessedCorrectly = false; 

    public DavinciGameLogic(RoomUserManager userManager) {
        this.userManager = userManager;
        this.broadcaster = userManager.getBroadcaster();
    }

    public boolean isPlaying() { return isPlaying; }

    // [1] 게임 시작
    public void startGame() {
        Vector<ClientHandler> users = userManager.getUserList();
        if (users.size() < 2) {
            broadcaster.broadcastChat("[System] 인원이 부족합니다 (최소 2명).");
            return;
        }

        isPlaying = true;
        broadcaster.roomLog("=== 게임 시작 ===");
        
        initializeDeck();
        userHands.clear();

        // 타일 분배 및 정렬
        for (ClientHandler user : users) {
            Vector<Tile> hand = new Vector<>();
            for (int k = 0; k < 4; k++) { // 4개씩 (2,3인 기준 보통 4개, 4인은 3개지만 여기선 4개로 통일)
                if (!deck.isEmpty()) hand.add(deck.pop());
            }
            Collections.sort(hand); // 초기 정렬
            userHands.put(user.getNickname(), hand);
        }
        
        // 각 유저에게 정보 전송 (상대방 타일은 색상만 보이게)
        for (ClientHandler user : users) {
            String myID = user.getNickname();
            Vector<Tile> myHand = userHands.get(myID);
            Vector<String> names = new Vector<>();
            for (ClientHandler u : users) names.add(u.getNickname());

            Map<String, Vector<String>> otherColors = new HashMap<>();
            for (ClientHandler other : users) {
                String oid = other.getNickname();
                if (!oid.equals(myID)) {
                    Vector<Tile> oh = userHands.get(oid);
                    Vector<String> colors = new Vector<>();
                    for (Tile t : oh) colors.add(t.getColor());
                    otherColors.put(oid, colors);
                }
            }
            user.sendMessage(new Message(Message.GAME_START, myHand, names, otherColors));
        }

        turnIndex = 0;
        prepareNextTurn();
    }
    
    // 턴 준비 공통 메소드
    private void prepareNextTurn() {
        currentTurnDrawnTile = null;
        hasGuessedCorrectly = false; // 상태 초기화
        notifyTurn();
    }

    // [2] 타일 뽑기
    public void handleDraw(ClientHandler player, String data) {
        Vector<ClientHandler> users = userManager.getUserList();
        ClientHandler currentTurnUser = users.get(turnIndex);
        
        if (!player.equals(currentTurnUser)) return;
        if (currentTurnDrawnTile != null) { // 이미 뽑음
            player.sendMessage(new Message(Message.CHAT_MSG, "[System] 이미 타일을 뽑았습니다."));
            return;
        }

        String[] parts = data.split(":");
        String color = parts[0];
        int insertIndex = Integer.parseInt(parts[1]);

        Tile drawn = null;
        // 덱 뒤에서부터 해당 색상 검색 (Stack처럼 사용)
        for (int i = deck.size() - 1; i >= 0; i--) {
            if (deck.get(i).getColor().equals(color)) {
                drawn = deck.remove(i);
                break;
            }
        }

        if (drawn != null) {
            Vector<Tile> hand = userHands.get(player.getNickname());
            if (insertIndex > hand.size()) insertIndex = hand.size();
            if (insertIndex < 0) insertIndex = 0;
            
            hand.add(insertIndex, drawn); // 위치에 삽입
            this.currentTurnDrawnTile = drawn; // 패널티용 저장

            // 클라이언트에 전송
            player.sendMessage(new Message(Message.RES_DRAW, drawn, insertIndex));
            broadcaster.broadcastChat("[System] " + player.getNickname() + "님이 " + color + " 타일을 가져갔습니다.");
        }
    }

    // [3] 추리 로직 (수정됨)
    public void handleGuess(ClientHandler guesser, String data) {
        String[] parts = data.split(":");
        String targetID = parts[0];
        int targetIndex = Integer.parseInt(parts[1]);
        String guessValue = parts[2]; 

        Vector<Tile> targetHand = userHands.get(targetID);
        Tile actualTile = targetHand.get(targetIndex);

        // 이미 공개된 타일을 맞추려는 경우 방지
        if (actualTile.isRevealed()) return;

        boolean isCorrect = false;
        if (actualTile.isJoker()) {
             if(guessValue.equals("-1") || guessValue.equals("JOKER")) isCorrect = true;
        } else {
             if(guessValue.equals(String.valueOf(actualTile.getNumber()))) isCorrect = true;
        }

        if (isCorrect) {
            // --- [정답!] ---
            actualTile.setRevealed(true); 
            hasGuessedCorrectly = true; // 이번 턴에 최소 1개 맞춤 (패널티 면제권 획득)
            
            broadcaster.broadcastChat("[System] " + guesser.getNickname() + "님이 맞췄습니다! (계속하려면 추리, 멈추려면 턴종료)");
            
            // 공개 알림
            String revealInfo = targetID + ":" + targetIndex;
            broadcaster.broadcast(new Message(Message.BCAST_REVEAL, revealInfo, actualTile));
            
            // 승리 조건 체크 (게임 끝나는지)
            if (checkWinCondition()) {
                endGame(guesser.getNickname());
            } 
            // 정답을 맞춰도 턴이 바로 넘어가지 않음! (다빈치코드 규칙)
            // 클라이언트가 계속 추리할지, handleEndTurn을 호출할지 결정함.
            
        } else {
            // --- [땡! 실패!] ---
            broadcaster.broadcastChat("[System] " + guesser.getNickname() + "님이 틀렸습니다!");
            
            // 패널티 1: 뽑았던 타일 공개 (단, 이번 턴에 뽑은게 있을 때만)
            if (currentTurnDrawnTile != null && !currentTurnDrawnTile.isRevealed()) {
                currentTurnDrawnTile.setRevealed(true);
                Vector<Tile> myHand = userHands.get(guesser.getNickname());
                int myIndex = myHand.indexOf(currentTurnDrawnTile);
                
                broadcaster.broadcast(new Message(Message.BCAST_REVEAL, guesser.getNickname() + ":" + myIndex, currentTurnDrawnTile));
            }
            
            // 패널티 2: 턴 강제 종료
            nextTurn(); 
        }
    }
    
    // [4] 턴 종료 요청 (사용자가 "그만하기" 버튼 눌렀을 때)
    public void handleEndTurn(ClientHandler player) {
        Vector<ClientHandler> users = userManager.getUserList();
        if (!player.equals(users.get(turnIndex))) return; // 내 턴 아니면 무시
        
        // 추리를 한 번도 안하고 턴을 넘기는 건 불가능 (규칙상 드로우 후 최소 1번 추리 시도 필요)
        // 하지만 여기선 드로우만 하고 바로 넘기는 걸 허용할지, 강제할지 결정 필요.
        // 일반적 규칙: 드로우 했으면 추리 해야함. 맞췄으면 멈출 수 있음.
        
        // 여기서는 "드로우 후엔 무조건 추리 시도"를 강제하고 싶다면 currentTurnDrawnTile 상태 체크
        // 일단 단순하게 턴 넘김 처리
        nextTurn();
    }

    private void nextTurn() {
        Vector<ClientHandler> users = userManager.getUserList();
        
        // 살아있는(패가 다 공개되지 않은) 사람을 찾을 때까지 턴 인덱스 증가
        int attempts = 0;
        do {
            turnIndex = (turnIndex + 1) % users.size();
            attempts++;
        } while (isPlayerOut(users.get(turnIndex).getNickname()) && attempts < users.size());

        prepareNextTurn();
    }
    
    // 유저가 탈락 상태(모든 패 공개됨)인지 확인
    private boolean isPlayerOut(String nickname) {
        Vector<Tile> hand = userHands.get(nickname);
        for (Tile t : hand) {
            if (!t.isRevealed()) return false; // 하나라도 안 뒤집혔으면 생존
        }
        return true; // 전부 뒤집혔으면 탈락
    }

    private void notifyTurn() {
        Vector<ClientHandler> users = userManager.getUserList();
        if (users.isEmpty()) return;
        
        String current = users.get(turnIndex).getNickname();
        broadcaster.roomLog("현재 턴: " + current);
        broadcaster.broadcast(new Message(Message.TURN_START, current));
    }
    
    public void handleTimeout(ClientHandler player) {
        Vector<ClientHandler> users = userManager.getUserList();
        if (!player.equals(users.get(turnIndex))) return;
        
        broadcaster.broadcastChat("[System] 시간 초과! 턴이 넘어갑니다.");
        // 시간 초과 시 패널티 없이 넘어갈지, 공개하고 넘어갈지는 선택. 여기선 그냥 넘김.
        nextTurn();
    }

    // 승리 조건 체크: 나 말고 다른 모든 사람의 패가 다 공개되었는가?
    private boolean checkWinCondition() {
        int survivorCount = 0;
        for (String user : userHands.keySet()) {
            if (!isPlayerOut(user)) {
                survivorCount++;
            }
        }
        // 생존자가 1명(방금 맞춘 사람) 뿐이면 승리
        return survivorCount <= 1;
    }
    
    private void endGame(String winner) {
        isPlaying = false;
        broadcaster.broadcastChat("[System] 게임 종료! 승자는 " + winner + "님 입니다.");
        broadcaster.broadcast(new Message(Message.GAME_OVER, winner));
        broadcaster.roomLog("게임 종료 - 승자: " + winner);
        
        // 게임 끝나면 방 상태 대기로 변경 등을 처리할 수 있음 (여기선 생략)
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