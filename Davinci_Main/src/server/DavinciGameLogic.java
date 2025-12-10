package server;

import java.util.*;
import protocol.Message;
import protocol.Tile;

public class DavinciGameLogic {
    private RoomUserManager userManager;
    private RoomBroadcaster broadcaster;
    
    // --- 게임 데이터 ---
    private Stack<Tile> deck = new Stack<>();
    private Map<String, Vector<Tile>> userHands = new HashMap<>(); // 유저별 패 (서버 저장용)
    private Tile currentTurnDrawnTile = null; // 이번 턴에 뽑은 타일 (틀리면 공개해야 함)
    private int turnIndex = 0;
    private boolean isPlaying = false;
    
    // ★ [규칙 적용] 이번 턴에 추리를 성공했는지 체크하는 플래그
    // (true가 되어야만 턴 종료 버튼을 누를 수 있음)
    private boolean hasGuessedCorrectly = false;

    public DavinciGameLogic(RoomUserManager userManager) {
        this.userManager = userManager;
        this.broadcaster = userManager.getBroadcaster();
    }

    public boolean isPlaying() { return isPlaying; }

    // [1] 게임 시작
    public void startGame() {
        Vector<ClientHandler> users = userManager.getUserList();
        
        // 인원 체크 (최소 2명)
        if (users.size() < 2) {
            broadcaster.broadcastChat("[System] 인원이 부족합니다 (최소 2명).");
            return;
        }

        isPlaying = true;
        broadcaster.roomLog("=== 게임 시작 ===");
        
        // 1. 덱 초기화
        initializeDeck();
        userHands.clear();

        // 2. 타일 분배 (4개씩) 및 정렬
        for (ClientHandler user : users) {
            Vector<Tile> hand = new Vector<>();
            for (int k = 0; k < 4; k++) {
                if (!deck.isEmpty()) hand.add(deck.pop());
            }
            // 초기 타일 정렬 (숫자 오름차순)
            Collections.sort(hand);
            userHands.put(user.getNickname(), hand);
        }
        
        // 3. 각 유저에게 패 전송 (상대방 패는 색상만 전송)
        for (ClientHandler user : users) {
            String myID = user.getNickname();
            Vector<Tile> myHand = userHands.get(myID);

            Vector<String> names = new Vector<>();
            for (ClientHandler u : users) names.add(u.getNickname());

            // 다른 사람 타일의 "색상만" 수집
            Map<String, Vector<String>> otherColors = new HashMap<>();
            for (ClientHandler other : users) {
                String oid = other.getNickname();
                if (!oid.equals(myID)) {
                    Vector<Tile> oh = userHands.get(oid);
                    Vector<String> colors = new Vector<>();
                    for (Tile t : oh) {
                        colors.add(t.getColor()); 
                    }
                    otherColors.put(oid, colors);
                }
            }
            
            user.sendMessage(new Message(Message.GAME_START, myHand, names, otherColors));
            broadcaster.roomLog(" -> " + myID + "에게 타일 분배 완료");
        }

        // 4. 첫 턴 준비
        turnIndex = 0;
        prepareNextTurn();
    }

    // [2] 타일 뽑기 로직
    public void handleDraw(ClientHandler player, String data) {
        Vector<ClientHandler> users = userManager.getUserList();
        ClientHandler currentTurnUser = users.get(turnIndex);
        
        // 내 턴 검증
        if (!player.equals(currentTurnUser)) return;

        // 이미 뽑았는지 검증
        if (currentTurnDrawnTile != null) {
            player.sendMessage(new Message(Message.CHAT_MSG, "[System] 이미 타일을 뽑았습니다."));
            return;
        }

        String[] parts = data.split(":");
        String color = parts[0];
        int insertIndex = Integer.parseInt(parts[1]);

        // 덱에서 해당 색상 타일 찾기
        Tile drawn = null;
        for (int i = deck.size() - 1; i >= 0; i--) {
            if (deck.get(i).getColor().equals(color)) {
                drawn = deck.remove(i);
                break;
            }
        }

        if (drawn != null) {
        // ... (기존 로직: hand.add(insertIndex, drawn) 및 currentTurnDrawnTile 저장) ...
        
        // 1. 클라이언트(본인)에게 결과 전송 (전체 정보)
        player.sendMessage(new Message(Message.RES_DRAW, drawn, insertIndex));
        
        // 2. ★ 상대방에게 갱신 정보 브로드캐스트 추가 ★
        // Message.BCAST_DRAW 모드를 사용하여, 어떤 유저가, 어떤 색상의 타일을,
        // 몇 번째 위치에 삽입했는지 알립니다.
        // 데이터 구조: [뽑은 사람 ID, 타일 색상, 삽입 인덱스]
        Vector<Object> broadcastData = new Vector<>();
        broadcastData.add(player.getNickname());
        broadcastData.add(drawn.getColor());
        broadcastData.add(insertIndex); 
        
        broadcaster.broadcast(new Message(Message.BCAST_DRAW, broadcastData)); // Message.BCAST_DRAW는 Message.java에 정의 필요
        
        broadcaster.broadcastChat("[System] " + player.getNickname() + "님이 " + color + " 타일을 가져갔습니다.");
        broadcaster.roomLog(player.getNickname() + "님이 " + color + " 타일 드로우 (위치: " + insertIndex + ")");
        
    }else {
            player.sendMessage(new Message(Message.CHAT_MSG, "[System] 해당 색상의 타일이 없습니다."));
        }
    }

    // [3] 추리 로직
    public void handleGuess(ClientHandler guesser, String data) {
        String[] parts = data.split(":");
        String targetID = parts[0];
        int targetIndex = Integer.parseInt(parts[1]);
        String guessValue = parts[2]; // 숫자문자열 ("7") 또는 "-1"(조커)

        // 정답 확인용
        Vector<Tile> targetHand = userHands.get(targetID);
        Tile actualTile = targetHand.get(targetIndex);

        // 이미 공개된 타일이면 무시
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
            
            // ★ [규칙 적용] 추리 성공 기록 -> 이제부터 턴 종료 가능
            hasGuessedCorrectly = true; 
            
            broadcaster.broadcastChat("[System] " + guesser.getNickname() + "님이 맞췄습니다! (계속하려면 추리, 멈추려면 턴종료)");
            broadcaster.roomLog(guesser.getNickname() + " 추리 성공 -> " + targetID + "의 " + targetIndex + "번 타일");
            
            // 공개 알림
            String revealInfo = targetID + ":" + targetIndex;
            broadcaster.broadcast(new Message(Message.BCAST_REVEAL, revealInfo, actualTile));
            
            // 승리 조건 체크
            if (checkWinCondition()) {
                endGame(guesser.getNickname());
            }
            
            // ★ 중요: 맞췄을 때는 nextTurn()을 호출하지 않음 (유저에게 선택권 부여)
            
        } else {
            // --- [땡! 실패!] ---
            broadcaster.broadcastChat("[System] " + guesser.getNickname() + "님이 틀렸습니다!");
            broadcaster.roomLog(guesser.getNickname() + " 추리 실패");
            
            // 패널티: 이번 턴에 뽑아온 타일 공개 (아직 안 뒤집힌 경우만)
            if (currentTurnDrawnTile != null && !currentTurnDrawnTile.isRevealed()) {
                currentTurnDrawnTile.setRevealed(true);
                
                Vector<Tile> myHand = userHands.get(guesser.getNickname());
                int myIndex = myHand.indexOf(currentTurnDrawnTile);
                
                String myInfo = guesser.getNickname() + ":" + myIndex;
                broadcaster.broadcast(new Message(Message.BCAST_REVEAL, myInfo, currentTurnDrawnTile));
            }
            
            // 틀리면 강제 턴 넘김
            nextTurn(); 
        }
    }
    
    // [4] 턴 종료 요청 (클라이언트가 "턴 넘기기" 버튼 클릭 시)
    public void handleEndTurn(ClientHandler player) {
        Vector<ClientHandler> users = userManager.getUserList();
        
        // 1. 내 턴인지 확인
        if (!player.equals(users.get(turnIndex))) return; 

        // ★ [수정된 규칙 적용] ★
        
        // Case 1: 드로우 자체를 하지 않은 경우 (이럴 리는 없으나 방어 코드)
        if (currentTurnDrawnTile == null) {
            player.sendMessage(new Message(Message.CHAT_MSG, "[System] 타일을 뽑아야만 게임 진행이 가능합니다."));
            return; 
        }
        
        // Case 2: 드로우는 했으나, 추리 성공 기록이 없는 경우
        // (성공 기록이 없다는 것은 추리를 아예 안 했거나, 추리할 기회가 없었다는 뜻)
        if (!hasGuessedCorrectly) {
            player.sendMessage(new Message(Message.CHAT_MSG, "[System] 타일을 뽑은 후 최소 1번은 추리해야 턴을 넘길 수 있습니다."));
            return;
        }
        
        // Case 3: 드로우 했고, 추리 성공 기록도 있는 경우 -> 턴 종료 허용
        broadcaster.broadcastChat("[System] " + player.getNickname() + "님이 턴을 마쳤습니다.");
        nextTurn();
    }
    
    // 타임아웃 처리
    public void handleTimeout(ClientHandler player) {
        Vector<ClientHandler> users = userManager.getUserList();
        if (!player.equals(users.get(turnIndex))) return; 

        broadcaster.broadcastChat("[System] 시간 초과! 턴이 넘어갑니다.");
        nextTurn();
    }

    // 다음 턴으로 넘기기 로직
    private void nextTurn() {
        Vector<ClientHandler> users = userManager.getUserList();
        if (users.isEmpty()) return;

        // 탈락하지 않은 다음 사람 찾기
        int loopCount = 0;
        do {
            turnIndex = (turnIndex + 1) % users.size();
            loopCount++;
        } while (isPlayerOut(users.get(turnIndex).getNickname()) && loopCount < users.size());
        
        prepareNextTurn();
    }
    
    // 턴 시작 전 초기화 작업
    private void prepareNextTurn() {
        currentTurnDrawnTile = null;   // 뽑은 타일 초기화
        hasGuessedCorrectly = false;   // ★ 추리 성공 플래그 초기화 (새 턴이므로 다시 맞춰야 함)
        
        notifyTurn();
    }

    private void notifyTurn() {
        Vector<ClientHandler> users = userManager.getUserList();
        if (users.isEmpty()) return;
        
        String current = users.get(turnIndex).getNickname();
        broadcaster.roomLog("턴 변경: 현재 턴 -> " + current);
        broadcaster.broadcast(new Message(Message.TURN_START, current));
    }

    // 유저 탈락 여부 (모든 타일이 공개되었는지)
    private boolean isPlayerOut(String nickname) {
        Vector<Tile> hand = userHands.get(nickname);
        if (hand == null) return true;
        
        for (Tile t : hand) {
            if (!t.isRevealed()) return false; // 하나라도 숨겨져 있으면 생존
        }
        return true; // 전부 공개되면 탈락
    }

    // 승리 조건 체크 (생존자가 1명 이하인가)
    private boolean checkWinCondition() {
        int survivorCount = 0;
        for (String user : userHands.keySet()) {
            if (!isPlayerOut(user)) {
                survivorCount++;
            }
        }
        return survivorCount <= 1;
    }
    
    // 게임 종료 처리
    private void endGame(String winner) {
        isPlaying = false;
        broadcaster.broadcastChat("[System] 게임 종료! 승자는 " + winner + "님 입니다.");
        broadcaster.broadcast(new Message(Message.GAME_OVER, winner));
        broadcaster.roomLog("게임 종료 - 승자: " + winner);
    }

    private void initializeDeck() {
        deck.clear();
        // 0~11 숫자 타일
        for (int i = 0; i <= 11; i++) {
            deck.add(new Tile("BLACK", i));
            deck.add(new Tile("WHITE", i));
        }
        // 조커 타일
        deck.add(new Tile("BLACK", -1));
        deck.add(new Tile("WHITE", -1));
        
        Collections.shuffle(deck);
    }
}