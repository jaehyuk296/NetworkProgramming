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
        broadcaster.roomLog("=== 게임 시작 (초기 분배: 조커 제외) ===");
        
        userHands.clear();
        deck.clear(); // 메인 덱 초기화

        // ★ [규칙 적용] 인원수에 따른 초기 타일 개수 설정
        // 4명일 경우 3개씩, 2~3명일 경우 4개씩 분배
        int tilesPerUser = (users.size() == 4) ? 3 : 4;

        // ---------------------------------------------------------
        // [수정된 로직] 1. 일반 타일(0~11)로만 임시 덱 생성
        // ---------------------------------------------------------
        Stack<Tile> tempDeck = new Stack<>();
        for (int i = 0; i <= 11; i++) {
            tempDeck.add(new Tile("BLACK", i));
            tempDeck.add(new Tile("WHITE", i));
        }
        // 일반 타일만 섞음
        Collections.shuffle(tempDeck);

        // ---------------------------------------------------------
        // [수정된 로직] 2. 플레이어들에게 타일 분배 (조커 절대 안 나옴)
        // ---------------------------------------------------------
        for (ClientHandler user : users) {
            Vector<Tile> hand = new Vector<>();
            // ★ 설정한 tilesPerUser 만큼 반복
            for (int k = 0; k < tilesPerUser; k++) { 
                if (!tempDeck.isEmpty()) {
                    hand.add(tempDeck.pop());
                }
            }
            // 받은 패 정렬 (숫자 오름차순)
            Collections.sort(hand);
            userHands.put(user.getNickname(), hand);
        }
        
        // ---------------------------------------------------------
        // [수정된 로직] 3. 남은 타일에 조커를 추가하고 다시 섞음 (메인 덱 생성)
        // ---------------------------------------------------------
        this.deck.addAll(tempDeck); // 남은 일반 타일 옮기기
        this.deck.add(new Tile("BLACK", -1)); // 조커 추가
        this.deck.add(new Tile("WHITE", -1)); // 조커 추가
        
        // 이제 조커가 포함된 상태로 다시 섞음 (앞으로 뽑을 때 조커 등장 가능)
        Collections.shuffle(this.deck);
        
        // ---------------------------------------------------------
        // 4. 각 유저에게 정보 전송 (기존 로직 동일)
        // ---------------------------------------------------------
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
            broadcaster.roomLog(" -> " + myID + "에게 타일 분배 완료 (" + tilesPerUser + "개)");
        }

        // 5. 첫 턴 준비
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

        // 덱이 비었는지 확인
        if (deck.isEmpty()) {
            player.sendMessage(new Message(Message.CHAT_MSG, "[System] 더 이상 가져올 타일이 없습니다. 바로 추리하세요."));
            // 드로우한 타일이 없음을 명시 (이미 null이겠지만 확실히)
            currentTurnDrawnTile = null; 
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
        // 이번 턴에 뽑은 타일 저장
        currentTurnDrawnTile = drawn;
        
        // ★ 조커인 경우: 위치가 정해질 때까지 상대방에게 알리지 않음 ★
        if (drawn.isJoker()) {
            // 조커는 일단 패에 추가하지 않고, 클라이언트에서 위치를 지정할 때까지 대기
            // 클라이언트에게만 조커 정보 전송 (상대방에게는 아직 브로드캐스트 안 함)
            player.sendMessage(new Message(Message.RES_DRAW, drawn, -1)); // 인덱스는 -1로 전송 (아직 미정)
            broadcaster.broadcastChat("[System] " + player.getNickname() + "님이 조커를 가져갔습니다.");
            broadcaster.roomLog(player.getNickname() + "님이 조커 드로우 (위치 미정)");
            return; // 조커는 handleJokerPosition에서 처리
        }
        
        // 일반 타일 처리
        // 타일을 패에 추가
        Vector<Tile> hand = userHands.get(player.getNickname());
        if (insertIndex < 0 || insertIndex > hand.size()) {
            insertIndex = hand.size();
        }
        hand.add(insertIndex, drawn);
        
        // ★ 조커를 제외한 타일들만 정렬 ★
        sortTilesExcludingJokers(hand);
        
        // 정렬 후 실제 인덱스 찾기 (본인에게 전송할 정확한 위치)
        int sortedIndex = hand.indexOf(drawn);
        
        // ★ 정렬 후 공개된 타일들의 새로운 인덱스 계산 및 업데이트 ★
        // 공개된 타일들이 정렬로 인해 위치가 변경되었을 수 있으므로 업데이트
        // 단, 조커는 정렬에서 제외되므로 공개된 조커는 업데이트하지 않음
        for (int i = 0; i < hand.size(); i++) {
            Tile t = hand.get(i);
            if (t.isRevealed() && t != drawn && !t.isJoker()) {
                // 공개된 타일의 새로운 인덱스를 전송 (본인에게만, 조커 제외)
                String revealInfo = player.getNickname() + ":" + i;
                player.sendMessage(new Message(Message.BCAST_REVEAL, revealInfo, t));
            }
        }
        
        // ★ 상대방에게는 실제 정렬된 순서의 인덱스를 그대로 전송 ★
        // 상대방은 숫자를 모르지만, 정렬된 순서의 색상 정보를 그대로 보여야 함
        // (색상 기준으로 재정렬하지 않고 실제 정렬 순서 유지)
        int opponentIndex = sortedIndex;
        
        // 1. 클라이언트(본인)에게 결과 전송 (전체 정보)
        player.sendMessage(new Message(Message.RES_DRAW, drawn, sortedIndex));
        
        // 2. ★ 상대방에게 갱신 정보 브로드캐스트 추가 ★
        // 상대방은 색상만 알 수 있지만, 실제 정렬된 순서(숫자+색상 기준)의 인덱스를 전송
        // 이렇게 하면 상대방이 보는 타일 순서가 내 타일의 실제 정렬 순서와 일치함
        // 데이터 구조: [뽑은 사람 ID, 타일 색상, 정렬된 인덱스]
        Vector<Object> broadcastData = new Vector<>();
        broadcastData.add(player.getNickname());
        broadcastData.add(drawn.getColor());
        broadcastData.add(opponentIndex); 
        
        broadcaster.broadcast(new Message(Message.BCAST_DRAW, broadcastData)); // Message.BCAST_DRAW는 Message.java에 정의 필요
        
        broadcaster.broadcastChat("[System] " + player.getNickname() + "님이 " + color + " 타일을 가져갔습니다.");
        broadcaster.roomLog(player.getNickname() + "님이 " + color + " 타일 드로우 (위치: " + insertIndex + ")");
        
    }else {
            player.sendMessage(new Message(Message.CHAT_MSG, "[System] 해당 색상의 타일이 없습니다."));
        }
    }
    
    // [2-1] 조커 위치 설정 로직 (조커를 뽑은 후 클라이언트에서 위치를 지정했을 때)
    public void handleJokerPosition(ClientHandler player, String data) {
        Vector<ClientHandler> users = userManager.getUserList();
        ClientHandler currentTurnUser = users.get(turnIndex);
        
        // 내 턴 검증
        if (!player.equals(currentTurnUser)) return;
        
        // 조커를 뽑았는지 검증
        if (currentTurnDrawnTile == null || !currentTurnDrawnTile.isJoker()) {
            player.sendMessage(new Message(Message.CHAT_MSG, "[System] 조커를 뽑지 않았습니다."));
            return;
        }
        
        String[] parts = data.split(":");
        String color = parts[0];
        int insertIndex = Integer.parseInt(parts[1]);
        
        // 조커 색상 확인
        if (!currentTurnDrawnTile.getColor().equals(color)) {
            player.sendMessage(new Message(Message.CHAT_MSG, "[System] 조커 색상이 일치하지 않습니다."));
            return;
        }
        
        // 조커를 패에 추가 (handleDraw에서 추가하지 않았으므로 여기서 추가)
        Vector<Tile> hand = userHands.get(player.getNickname());
        
        // 지정된 인덱스에 조커 삽입
        if (insertIndex < 0 || insertIndex > hand.size()) {
            insertIndex = hand.size();
        }
        hand.add(insertIndex, currentTurnDrawnTile);
        
        // ★ 조커를 제외한 타일들만 정렬 ★
        sortTilesExcludingJokers(hand);
        
        // 정렬 후 실제 인덱스 찾기
        int sortedIndex = hand.indexOf(currentTurnDrawnTile);
        
        // ★ 정렬 후 공개된 타일들의 새로운 인덱스 계산 및 업데이트 ★
        // 공개된 타일들이 정렬로 인해 위치가 변경되었을 수 있으므로 업데이트
        // 단, 조커는 정렬에서 제외되므로 공개된 조커는 업데이트하지 않음
        for (int i = 0; i < hand.size(); i++) {
            Tile t = hand.get(i);
            if (t.isRevealed() && t != currentTurnDrawnTile && !t.isJoker()) {
                // 공개된 타일의 새로운 인덱스를 전송 (본인에게만, 조커 제외)
                String revealInfo = player.getNickname() + ":" + i;
                player.sendMessage(new Message(Message.BCAST_REVEAL, revealInfo, t));
            }
        }
        
        // ★ 상대방에게는 실제 정렬된 순서의 인덱스를 그대로 전송 ★
        int opponentIndex = sortedIndex;
        
        // ★ 상대방에게 갱신 정보 브로드캐스트 (조커 위치 업데이트) ★
        Vector<Object> broadcastData = new Vector<>();
        broadcastData.add(player.getNickname());
        broadcastData.add(currentTurnDrawnTile.getColor());
        broadcastData.add(opponentIndex);
        
        broadcaster.broadcast(new Message(Message.BCAST_DRAW, broadcastData));
        broadcaster.roomLog(player.getNickname() + "님이 조커 위치 설정 (위치: " + insertIndex + " -> 정렬 후: " + sortedIndex + ")");
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
    if (!player.equals(users.get(turnIndex))) return; 

    // [수정] 덱이 남아있는데 드로우를 안 했으면 에러 (기존 로직 유지)
    if (!deck.isEmpty() && currentTurnDrawnTile == null) {
        player.sendMessage(new Message(Message.CHAT_MSG, "[System] 타일을 뽑아야 합니다."));
        return;
    }

    // ★ [핵심 수정] 덱에서 타일을 뽑았는데, 한 번도 맞추지 않았다면 턴 종료 불가
    // (덱이 비어서 못 뽑은 경우는 예외적으로 추리만 하므로 이 조건 패스)
    if (currentTurnDrawnTile != null && !hasGuessedCorrectly) {
        player.sendMessage(new Message(Message.CHAT_MSG, "[System] 최소 한 번은 추리를 시도해야 합니다!"));
        return; 
    }
    
    // 통과 시 턴 넘김
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

    // ★ 조커를 제외한 타일들만 정렬하는 헬퍼 메서드 ★
    private void sortTilesExcludingJokers(Vector<Tile> tiles) {
        // 조커의 인덱스와 타일을 저장 (인덱스는 오름차순으로 저장)
        Vector<Integer> jokerIndices = new Vector<>();
        Vector<Tile> jokers = new Vector<>();
        
        // 조커 찾기 및 제거 (뒤에서부터 제거하여 인덱스 변화 최소화)
        for (int i = tiles.size() - 1; i >= 0; i--) {
            Tile t = tiles.get(i);
            if (t.isJoker()) {
                jokerIndices.add(0, i); // 오름차순으로 저장
                jokers.add(0, tiles.remove(i));
            }
        }
        
        // 숫자 타일들만 정렬
        Collections.sort(tiles);
        
        // 조커를 원래 인덱스에 다시 삽입 (뒤에서부터 삽입하여 인덱스 변화 최소화)
        for (int i = jokers.size() - 1; i >= 0; i--) {
            int originalIndex = jokerIndices.get(i);
            // 원래 인덱스가 범위를 벗어나면 마지막에 추가
            if (originalIndex > tiles.size()) {
                originalIndex = tiles.size();
            }
            tiles.add(originalIndex, jokers.get(i));
        }
    }
}