package server;

import java.util.*;
import protocol.Message;
import protocol.Tile;

public class DavinciGameLogic {
    private RoomUserManager userManager;
    private RoomBroadcaster broadcaster;
    
    // 게임 데이터
    private Stack<Tile> deck = new Stack<>();
    private Map<String, Vector<Tile>> userHands = new HashMap<>(); // 유저별 패 (서버 저장용)
    private Tile currentTurnDrawnTile = null; // 이번 턴에 뽑은 타일 (틀리면 공개해야 함)
    private int turnIndex = 0;
    private boolean isPlaying = false;

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

        // 2. 타일 분배 (4개씩)
        for (ClientHandler user : users) {
            Vector<Tile> hand = new Vector<>();
            for (int k = 0; k < 4; k++) {
                if (!deck.isEmpty()) hand.add(deck.pop());
            }
            // 초기 타일 정렬 (숫자 오름차순)
            Collections.sort(hand);
            
            // 서버 메모리에 저장 (중요: 정답 확인용)
            userHands.put(user.getNickname(), hand);
            
            // 플레이어 순서 목록 생성
            Vector<String> names = new Vector<>();
            for(ClientHandler u : users) names.add(u.getNickname());
            
            // 각 유저에게 패 전송
            // Data1: 내 패(Vector<Tile>), Data2: 플레이어 명단(Vector<String>)
            user.sendMessage(new Message(Message.GAME_START, hand, names));
            
            broadcaster.roomLog(" -> " + user.getNickname() + "에게 타일 분배 완료");
        }

        // 3. 첫 턴 시작
        turnIndex = 0;
        currentTurnDrawnTile = null;
        notifyTurn();
    }

    // [2] 타일 뽑기 로직
    // data format: "COLOR:INDEX" (예: "BLACK:3")
    public void handleDraw(ClientHandler player, String data) {
        Vector<ClientHandler> users = userManager.getUserList();
        ClientHandler currentTurnUser = users.get(turnIndex);
        
        // 내 턴이 아니면 무시
        if (!player.equals(currentTurnUser)) return;

        // 이미 뽑았으면 무시
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
            // 서버 메모리(userHands)에 해당 위치로 삽입
            Vector<Tile> hand = userHands.get(player.getNickname());
            
            // 인덱스 안전장치
            if (insertIndex > hand.size()) insertIndex = hand.size();
            if (insertIndex < 0) insertIndex = 0;
            
            hand.add(insertIndex, drawn);
            
            // 방금 뽑은 타일 임시 저장 (추리 실패 시 공개 패널티용)
            this.currentTurnDrawnTile = drawn;

            // 클라이언트에게 결과 전송
            // RES_DRAW -> Data1: 타일객체, Payload: 인덱스(Integer)
            player.sendMessage(new Message(Message.RES_DRAW, drawn, insertIndex));
            
            broadcaster.broadcastChat("[System] " + player.getNickname() + "님이 " + color + " 타일을 가져갔습니다.");
            broadcaster.roomLog(player.getNickname() + "님이 " + color + " 타일 드로우 (위치: " + insertIndex + ")");
        } else {
            player.sendMessage(new Message(Message.CHAT_MSG, "[System] 해당 색상의 타일이 없습니다."));
        }
    }

    // [3] 추리 로직
    // data format: "TargetID:Index:Value"
    public void handleGuess(ClientHandler guesser, String data) {
        String[] parts = data.split(":");
        String targetID = parts[0];
        int targetIndex = Integer.parseInt(parts[1]);
        String guessValue = parts[2]; // 숫자문자열 ("7") 또는 "-1"(조커)

        // 정답 확인을 위해 타겟의 패 가져오기
        Vector<Tile> targetHand = userHands.get(targetID);
        Tile actualTile = targetHand.get(targetIndex);

        boolean isCorrect = false;
        if (actualTile.isJoker()) {
             // 조커는 -1 또는 JOKER 입력 시 정답
             if(guessValue.equals("-1") || guessValue.equals("JOKER")) isCorrect = true;
        } else {
             // 숫자는 일치해야 정답
             if(guessValue.equals(String.valueOf(actualTile.getNumber()))) isCorrect = true;
        }

        if (isCorrect) {
            // --- [정답!] ---
            actualTile.setRevealed(true); // 서버 상태 공개로 변경
            
            broadcaster.broadcastChat("[System] " + guesser.getNickname() + "님이 맞췄습니다!");
            broadcaster.roomLog(guesser.getNickname() + " 추리 성공 -> " + targetID + "의 " + targetIndex + "번 타일");
            
            // 모든 클라에게 공개 알림
            // Data1: "TargetID:Index", Payload: Tile객체
            String revealInfo = targetID + ":" + targetIndex;
            broadcaster.broadcast(new Message(Message.BCAST_REVEAL, revealInfo, actualTile));
            
            // 승리 조건 체크 (해당 타겟이 아웃되었는지, 남은 사람이 1명인지)
            checkWinCondition();
            
            // 맞추면 턴을 계속 이어갈 수도 있지만, 여기선 일단 턴 넘김 (규칙에 따라 수정 가능)
            // nextTurn(); 
            
        } else {
            // --- [땡! 실패!] ---
            broadcaster.broadcastChat("[System] " + guesser.getNickname() + "님이 틀렸습니다!");
            broadcaster.roomLog(guesser.getNickname() + " 추리 실패");
            
            // 패널티: 이번 턴에 뽑아온 타일을 공개해야 함
            if (currentTurnDrawnTile != null) {
                currentTurnDrawnTile.setRevealed(true);
                
                // 내 패에서 방금 뽑은 타일의 인덱스 찾기
                Vector<Tile> myHand = userHands.get(guesser.getNickname());
                int myIndex = myHand.indexOf(currentTurnDrawnTile);
                
                String myInfo = guesser.getNickname() + ":" + myIndex;
                // 내 타일 공개 알림
                broadcaster.broadcast(new Message(Message.BCAST_REVEAL, myInfo, currentTurnDrawnTile));
            }
            
            // 틀리면 무조건 턴 넘김
            nextTurn(); 
        }
    }

    private void nextTurn() {
        currentTurnDrawnTile = null; // 뽑은 타일 초기화
        
        Vector<ClientHandler> userList = userManager.getUserList();
        // 다음 사람으로 인덱스 이동
        turnIndex = (turnIndex + 1) % userList.size();
        
        notifyTurn();
    }

    private void notifyTurn() {
        Vector<ClientHandler> users = userManager.getUserList();
        if (users.isEmpty()) return;
        
        String current = users.get(turnIndex).getNickname();
        broadcaster.roomLog("턴 변경: 현재 턴 -> " + current);
        
        // 모든 클라에게 "지금은 OO의 턴이다" 알림
        broadcaster.broadcast(new Message(Message.TURN_START, current));
    }

    private void checkWinCondition() {
        // (간단 버전) 모든 타일이 공개된 유저는 탈락 처리 로직이 필요하지만
        // 여기서는 생략하고 게임을 계속 진행합니다.
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