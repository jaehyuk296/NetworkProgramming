package client;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import javax.imageio.ImageIO;
import javax.swing.*;
import protocol.Message;
import protocol.Tile; // Tile 클래스 import 필수

public class InGameUI extends JPanel { 
    private Image backgroundImage; 
    private GameClient gameClient; 
    
    // --- UI 컴포넌트 ---
    private JLabel[] playerLabels; // 0:나, 1:좌, 2:상, 3:우
    private JTextArea chatArea; 
    private JTextField t_chatInput; 
    
    // 액션 패널 컴포넌트
    private JPanel actionPanel;
    private JLabel lblTurnInfo;
    private JButton b_getBlack, b_getWhite;
    private JButton b_endTurn;
    
    private JTextField t_guessTarget, t_guessIndex, t_guessValue;
    private JButton b_guess;
    
    private JLabel lblTimer;
    private Timer turnTimer;
    private int remainingTime = 30;

    // --- 게임 데이터 ---
    private String myNickname;
    // 나의 타일 리스트 (서버와 동기화)
    private Vector<Tile> myTiles = new Vector<>();
    // 상대방 타일 리스트 (Key: 닉네임, Value: 타일 벡터)
    private Map<String, Vector<Tile>> opponentHands = new HashMap<>();
    // 닉네임 -> 화면 위치 인덱스 매핑 (0:나, 1:좌, 2:상, 3:우)
    private Map<String, Integer> userPositionMap = new HashMap<>();

    public InGameUI(GameClient gameClient) { 
        this.gameClient = gameClient; 
        try { 
            backgroundImage = ImageIO.read(new File("./imgs/game_img.jpg"));
        } catch (IOException e) { 
            System.out.println("이미지 로드 실패: ./imgs/game_img.jpg"); 
        } 
        setLayout(null);
        buildGUI(); 
    } 

    private void buildGUI() { 
        // 1. 플레이어 라벨 (위치 표시용)
        playerLabels = new JLabel[4];
        int[][] pos = { {400, 550}, {50, 250}, {400, 30}, {750, 250} }; // 하, 좌, 상, 우
        
        for(int i=0; i<4; i++) {
            playerLabels[i] = new JLabel("", JLabel.CENTER);
            playerLabels[i].setBounds(pos[i][0], pos[i][1], 200, 40);
            playerLabels[i].setFont(new Font("Malgun Gothic", Font.BOLD, 16));
            playerLabels[i].setOpaque(true);
            playerLabels[i].setBackground(new Color(255, 255, 255, 150));
            playerLabels[i].setVisible(false); // 초기엔 숨김
            add(playerLabels[i]);
        }
        playerLabels[0].setText("나 (Player1)"); // 내 위치 고정

        // 2. 채팅창 (좌측 하단)
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(chatArea);
        scroll.setBounds(20, 580, 300, 120);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        add(scroll);

        t_chatInput = new JTextField();
        t_chatInput.setBounds(20, 710, 220, 30);
        t_chatInput.addActionListener(e -> sendMessage());
        add(t_chatInput);

        JButton b_send = new JButton("Send");
        b_send.setBounds(250, 710, 70, 30);
        b_send.addActionListener(e -> sendMessage());
        add(b_send);

        // 3. 액션 패널 (우측 하단)
        buildActionPanel();

        // 4. 타이머 (중앙 상단)
        lblTimer = new JLabel("대기 중...", JLabel.CENTER);
        lblTimer.setBounds(400, 300, 200, 30);
        lblTimer.setFont(new Font("Arial", Font.BOLD, 20));
        lblTimer.setForeground(Color.YELLOW);
        add(lblTimer);
        
        // 타이머 객체 초기화 (1초마다 실행)
        turnTimer = new Timer(1000, e -> {
            if(remainingTime > 0) {
                remainingTime--;
                lblTimer.setText(remainingTime + "sec Remain!");
            } else {
                lblTimer.setText("It's Over");
                ((Timer)e.getSource()).stop();
                sendTurnTimeout();
            }
        });
    }
    
    private void sendTurnTimeout() {
        // 서버에 "타임아웃!" 메시지 전송
        gameClient.send(new Message(Message.TURN_TIMEOUT, myNickname));
    }

    private void buildActionPanel() {
        actionPanel = new JPanel();
        actionPanel.setLayout(null);
        actionPanel.setBounds(600, 600, 360, 170);
        actionPanel.setBackground(new Color(0, 0, 0, 150)); // 반투명 검정
        
        // 턴 표시
        lblTurnInfo = new JLabel("게임을 시작합니다.");
        lblTurnInfo.setBackground(Color.BLACK);
        lblTurnInfo.setForeground(Color.WHITE);
        lblTurnInfo.setBounds(10, 5, 300, 20);
        actionPanel.add(lblTurnInfo);

        // 타일 가져오기
        b_getBlack = new JButton("Black");
        b_getBlack.setBackground(Color.BLACK);
        b_getBlack.setForeground(Color.WHITE);
        b_getBlack.setBounds(170, 30, 80, 30);
        b_getBlack.addActionListener(e -> requestDraw("BLACK"));
        actionPanel.add(b_getBlack);

        b_getWhite = new JButton("White");
        b_getWhite.setBackground(Color.WHITE);
        b_getWhite.setForeground(Color.BLACK);
        b_getWhite.setBounds(260, 30, 80, 30);
        b_getWhite.addActionListener(e -> requestDraw("WHITE"));
        actionPanel.add(b_getWhite);
        
        // [NEW] 턴 종료 버튼 (맞췄을 때만 활성화)
        b_endTurn = new JButton("턴 종료");
        b_endTurn.setBackground(Color.RED);
        b_endTurn.setForeground(Color.WHITE);
        b_endTurn.setBounds(260, 30, 80, 30);
        b_endTurn.setEnabled(false); // 기본 비활성
        b_endTurn.addActionListener(e -> requestEndTurn());
        actionPanel.add(b_endTurn);

        // --- 추측 하기 ---
        JLabel l2 = new JLabel("ID:");
        l2.setForeground(Color.WHITE);
        l2.setBounds(10, 80, 20, 20);
        actionPanel.add(l2);
        
        t_guessTarget = new JTextField(); // 대상 ID
        t_guessTarget.setBounds(35, 80, 60, 20);
        actionPanel.add(t_guessTarget);
        
        JLabel l3 = new JLabel("Idx:");
        l3.setForeground(Color.WHITE);
        l3.setBounds(105, 80, 25, 20);
        actionPanel.add(l3);

        t_guessIndex = new JTextField(); // 타일 인덱스
        t_guessIndex.setBounds(135, 80, 30, 20);
        actionPanel.add(t_guessIndex);

        JLabel l4 = new JLabel("Num:");
        l4.setForeground(Color.WHITE);
        l4.setBounds(175, 80, 35, 20);
        actionPanel.add(l4);
        
        t_guessValue = new JTextField(); // 숫자 or JOKER
        t_guessValue.setBounds(210, 80, 50, 20);
        actionPanel.add(t_guessValue);
        
        b_endTurn = new JButton("턴 종료");
        // 위치를 타일 가져오기 버튼과 겹치지 않게 수정 (예: 좌측 아래)
        b_endTurn.setBounds(170, 120, 170, 30); // 위치 수정
        b_endTurn.setEnabled(false); // 기본 비활성
        b_endTurn.addActionListener(e -> requestEndTurn());
        actionPanel.add(b_endTurn);

        b_guess = new JButton("추측");
        b_guess.setBounds(270, 80, 70, 25);
        b_guess.addActionListener(e -> requestGuess());
        actionPanel.add(b_guess);

        add(actionPanel);
     // 초기 버튼 상태 비활성화
        setButtonsEnabled(false, false, false);
    }

    // --- 통신 요청 메서드 ---
    private void sendMessage() {
        String msg = t_chatInput.getText().trim();
        if(!msg.isEmpty()) {
            gameClient.send(new Message(Message.CHAT_MSG, msg));
            t_chatInput.setText("");
        }
    }

    private void requestDraw(String color) {
        // Protocol: "COLOR:INDEX"
        gameClient.send(new Message(Message.REQ_DRAW, color + ":-1"));
    }
    
    private void requestEndTurn() {
        gameClient.send(new Message(Message.REQ_END_TURN, myNickname));
    }

    private void requestGuess() {
        String target = t_guessTarget.getText().trim();
        String idxStr = t_guessIndex.getText().trim();
        String val = t_guessValue.getText().trim();

        if(target.isEmpty() || idxStr.isEmpty() || val.isEmpty()) {
            appendChat("[System] 추측 정보를 모두 입력하세요.");
            return;
        }
        // 조커 처리: JOKER라고 입력하면 -1로 변환 전송 (서버 로직에 맞춤)
        if(val.equalsIgnoreCase("JOKER") || val.equalsIgnoreCase("J")) {
            val = "-1";
        }
        
        // Protocol: "TargetID:Index:Value"
        gameClient.send(new Message(Message.REQ_GUESS, target + ":" + idxStr + ":" + val));
        
        // 입력 필드 초기화
        t_guessTarget.setText("");
        t_guessIndex.setText("");
        t_guessValue.setText("");
    }

    // --- 게임 로직 및 UI 업데이트 (GameClient에서 호출) ---

    // 1. 게임 시작 (내 패, 플레이어 명단 수신)
    public void startGame(Vector<Tile> myHand, Vector<String> players, Map<String, Vector<String>> otherColors, String myNick) {
        this.myTiles = new Vector<>(myHand);
        this.myNickname = myNick;
        this.opponentHands.clear();
        this.userPositionMap.clear();

        // 플레이어 배치 로직 (나는 0번, 나머지는 순서대로 1,2,3)
        int posIndex = 1;
        for(String pName : players) {
            if(pName.equals(myNickname)) {
                userPositionMap.put(pName, 0); // 나
                playerLabels[0].setText(pName + " (나)");
                playerLabels[0].setVisible(true);
            } else {
                if(posIndex <= 3) {
                    userPositionMap.put(pName, posIndex);
                    playerLabels[posIndex].setText(pName);
                    playerLabels[posIndex].setVisible(true);
                    
                    // 상대방 타일 초기화 (서버 규칙상 4장씩 받음)
                    Vector<Tile> dummyHand = new Vector<>();
                    Vector<String> colorList = otherColors.get(pName);
                    for(int k = 0; k < 4; k++) {
                        String c = colorList.get(k);

                        // 숫자는 모름 -> -999 사용, 색깔만 넣기
                        Tile t = new Tile(c, -999);
                        dummyHand.add(t);
                    }
                    opponentHands.put(pName, dummyHand);
                    
                    posIndex++;
                }
            }
        }
        appendChat("=== 게임 시작 ===");
        repaint();
    }

    // [UPDATE] 턴 시작 알림
    public void updateTurn(String currentNick) {
        boolean isMyTurn = currentNick.equals(myNickname);
        
        if (isMyTurn) {
            lblTurnInfo.setText("나의 턴! 타일을 가져오세요.");
            lblTurnInfo.setForeground(Color.RED);
            
            // ★ [수정] 내 턴 시작 시: 드로우O, 추측X, 종료X
            setButtonsEnabled(true, false, false);
            
        } else {
            lblTurnInfo.setText(currentNick + "의 턴입니다.");
            lblTurnInfo.setForeground(Color.WHITE);
            
            // 남의 턴: 모든 액션 불가 (턴 종료 버튼도 비활성화)
            setButtonsEnabled(false, false, false);
        }
        
        lblTurnInfo.setBackground(Color.BLACK);
        
        // 타이머 리셋
        remainingTime = 30;
        lblTimer.setText("30sec Remain");
        turnTimer.restart();
    }

    // [UPDATE] 타일 뽑기 결과 처리 (내 동작)
    public void handleDrawResult(Tile tile, int index) {
        // 1. 조커 처리 로직
        if (tile.getNumber() == -1) {
            String input = JOptionPane.showInputDialog(
                    this, 
                    "조커가 나왔습니다!\n삽입할 인덱스를 입력하세요.",
                    "JOKER 삽입",
                    JOptionPane.QUESTION_MESSAGE
            );
            
            int insertIndex = myTiles.size();
            try {
                if(input != null && !input.trim().isEmpty()) {
                    insertIndex = Integer.parseInt(input.trim());
                }
            } catch (NumberFormatException e) {
                appendChat("[System] 숫자가 아닙니다. 자동으로 마지막 위치에 삽입합니다.");
            }

            if(insertIndex < 0) insertIndex = 0;
            if(insertIndex > myTiles.size()) insertIndex = myTiles.size();

            // 조커를 지정된 인덱스에 삽입
            myTiles.add(insertIndex, tile);
            
            // 조커를 제외한 숫자 타일들만 정렬
            sortTilesExcludingJokers(myTiles);
            
            // ★ 서버로 조커의 위치 정보 전송 (색상:인덱스 형식)
            gameClient.send(new Message(Message.REQ_JOKER_POSITION, tile.getColor() + ":" + insertIndex));
            
        } else {
            // 일반 타일: 조커를 제외하고 정렬
            myTiles.add(tile);
            sortTilesExcludingJokers(myTiles);
        }
        
        // 2. 화면 갱신
        repaint();
        appendChat("[System] 타일을 가져왔습니다. (" + tile.toString() + ")");
        
        // ★ [수정] 상태 업데이트: 드로우 끝 -> 추측 가능, 턴 종료도 가능 (추측 없이도 턴 종료 가능)
        setButtonsEnabled(false, true, false); // (Draw X, Guess O, EndTurn X)
        lblTurnInfo.setText("타일을 배치했습니다. 반드시 추리를 시도해야 합니다.");
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
    
    // [NEW] 상대방이 타일을 가져갔을 때 UI 처리
    public void handleOpponentDraw(String nickname, String color, int index) {
        // 나 자신이면 이미 handleDrawResult에서 처리했으므로 무시
        if (nickname.equals(myNickname)) return;

        Vector<Tile> hand = opponentHands.get(nickname);
        if (hand == null) return;

        // 상대방 패에 '뒷면' 타일 추가 (숫자는 모르므로 -999)
        Tile secretTile = new Tile(color, -999);
        
        // ★ 서버에서 보낸 실제 정렬 순서의 인덱스에 타일 추가
        // 서버에서 정렬된 순서를 그대로 유지 (색상 기준 재정렬하지 않음)
        // 인덱스 안전 장치 (혹시 모를 범위 초과 방지)
        if (index < 0) index = 0;
        if (index > hand.size()) index = hand.size();
        
        hand.add(index, secretTile);
        
        // ★ 색상 기준으로 재정렬하지 않음 - 서버에서 보낸 정렬 순서 유지
        
        repaint(); // 화면 갱신
}

    // [UPDATE] 타일 공개 알림 처리
    public void handleReveal(String targetID, int index, Tile tile) {
        // 1. 데이터 갱신
        if(targetID.equals(myNickname)) {
            // 내 타일이 공개됨 (틀렸거나, 상대가 맞춤)
            // ★ 조커는 정렬에서 제외되므로, 공개된 조커는 인덱스 업데이트하지 않음 ★
            if(tile.isJoker()) {
                // 조커는 정렬에서 제외되므로, 원래 위치의 조커를 찾아서 공개 처리
                if(index < myTiles.size() && myTiles.get(index).isJoker()) {
                    myTiles.get(index).setRevealed(true);
                } else {
                    // 조커를 찾아서 공개 처리
                    for(Tile t : myTiles) {
                        if(t.isJoker() && t.getColor().equals(tile.getColor())) {
                            t.setRevealed(true);
                            break;
                        }
                    }
                }
                repaint();
                return; // 조커는 여기서 처리 완료
            }
            
            // 일반 타일 처리: 정렬 후 인덱스가 변경되었을 수 있으므로, 타일 객체를 찾아서 업데이트
            if(index < myTiles.size()) {
                Tile existingTile = myTiles.get(index);
                // 같은 타일인지 확인 (숫자와 색상으로 비교)
                if(existingTile.getNumber() == tile.getNumber() && 
                   existingTile.getColor().equals(tile.getColor()) && !existingTile.isJoker()) {
                    existingTile.setRevealed(true);
                } else {
                    // 인덱스가 변경되었을 수 있으므로, 타일을 찾아서 업데이트
                    for(Tile t : myTiles) {
                        if(t.getNumber() == tile.getNumber() && 
                           t.getColor().equals(tile.getColor()) && !t.isJoker()) {
                            t.setRevealed(true);
                            break;
                        }
                    }
                }
            }
        } else {
            // 상대방 타일이 공개됨
            Vector<Tile> opHand = opponentHands.get(targetID);
            if(opHand != null) {
                while(opHand.size() <= index) opHand.add(null);
                // 인덱스에 타일이 있으면 교체, 없으면 추가
                if(opHand.get(index) != null) {
                    opHand.set(index, tile); // 진짜 정보로 교체
                } else {
                    opHand.add(index, tile);
                }
            }
        }
        
        // 내 턴일 때 로직
        if (lblTurnInfo.getText().contains("나의 턴") || b_guess.isEnabled()) {
            if (!targetID.equals(myNickname)) {
                // ★ [수정] 상대방 타일을 맞춰서 공개된 경우에만 '턴 종료' 버튼 활성화
                appendChat("[System] 추리 성공! 계속하거나 턴을 종료할 수 있습니다.");
                setButtonsEnabled(false, true, true); // (Draw X, Guess O, EndTurn O)
            } else {
                // 내가 틀려서 내 타일이 공개됨 -> 곧 서버가 턴을 넘김 (버튼 조작 불필요)
            }
        }
        repaint();
    }
    
    public void appendChat(String msg) {
        chatArea.append(msg + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    // --- 렌더링 (핵심) ---
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }

        // 1. 내 타일 그리기 (Player 1 위치: 하단)
        drawTileList(g, myTiles, playerLabels[0].getX() - 50, playerLabels[0].getY() - 70, true);

        // 2. 상대방 타일 그리기
        // userPositionMap을 순회하며 그리기
        for(Map.Entry<String, Integer> entry : userPositionMap.entrySet()) {
            String nick = entry.getKey();
            int posIdx = entry.getValue();
            if(posIdx == 0) continue; // 나는 이미 그림

            Vector<Tile> hand = opponentHands.get(nick);
            if(hand == null) continue;

            JLabel lbl = playerLabels[posIdx];
            // 위치에 따라 좌표 조정 (단순화: 라벨 근처에 그리기)
            int tx = lbl.getX();
            int ty = lbl.getY();
            
            // 상단(2)은 라벨 아래, 좌우(1,3)는 라벨 아래
            drawTileList(g, hand, tx, ty + 50, false); // false = 상대방(비공개 처리 포함)
        }
    }

    private void drawTileList(Graphics g, Vector<Tile> tiles, int startX, int startY, boolean isMe) {
        int w = 40, h = 60, gap = 5;
        for (int i = 0; i < tiles.size(); i++) {
            Tile t = tiles.get(i);
            int x = startX + i * (w + gap);
            int y = startY;

            if (isMe) {
                // 내 Tile
                if (t != null) {
                    drawSingleTile(g, x, y, w, h, t.getColor(), t.getNumber(), t.isRevealed(), i);
                }
            } else {
                // 타일 객체는 있지만 아직 공개 안 된 경우 (색깔은 앎)
                if (t.isRevealed()) {
                    // 공개됨 -> 숫자 보임
                    drawSingleTile(g, x, y, w, h, t.getColor(), t.getNumber(), true, i);
                } else {
                    // 비공개 -> 색깔만 보임 (여기서 수정된 함수 호출!)
                    drawBackTile(g, x, y, w, h, t.getColor(), i);
                }
            }
        }
    }

    private void drawSingleTile(Graphics g, int x, int y, int w, int h, String colorStr, int number, boolean isRev, int index) {
        Color c = colorStr.equals("BLACK") ? Color.BLACK : Color.WHITE;
        Color fontC = c == Color.BLACK ? Color.WHITE : Color.BLACK;

        g.setColor(c);
        g.fillRect(x, y, w, h);
        g.setColor(Color.GRAY);
        g.drawRect(x, y, w, h);

        // 숫자 그리기
        String numStr = (number == -1) ? "-" : String.valueOf(number);
        g.setColor(fontC);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString(numStr, x + 10, y + 35);
        
        // 인덱스 표시 (작게)
        g.setFont(new Font("Arial", Font.PLAIN, 10));
        g.setColor(Color.RED);
        g.drawString("" + index, x + 15, y + 55);

        // 공개 여부 표시 (누워있는 효과 대신 텍스트로)
        if(isRev) {
            g.setColor(Color.RED);
            g.drawRect(x-1, y-1, w+2, h+2); // 빨간 테두리
        }
    }

 // [수정] 색상을 알 수 있는 뒷면 그리기
    private void drawBackTile(Graphics g, int x, int y, int w, int h, String colorStr, int index) {
        // 1. 색상 설정 (Black 타일이면 배경 검정/글자 흰색, White 타일이면 배경 흰색/글자 검정)
        boolean isBlack = "BLACK".equals(colorStr);
        
        Color bgColor = isBlack ? Color.BLACK : Color.WHITE;
        Color lineColor = isBlack ? Color.WHITE : Color.BLACK; // 테두리 및 글자색

        // 2. 타일 배경
        g.setColor(bgColor);
        g.fillRect(x, y, w, h);

        // 3. 타일 테두리 (입체감을 위해 Dark Gray 사용하거나 대비되는 색 사용)
        g.setColor(Color.DARK_GRAY);
        g.drawRect(x, y, w, h);

        // 4. 뒷면 무늬 표시 (물음표 ?)
        g.setColor(lineColor); // 배경색과 대비되는 색
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("?", x + 15, y + 35); // 중앙 쯤에 물음표

        // 5. 인덱스 표시 (하단 작게)
        g.setFont(new Font("Arial", Font.PLAIN, 10));
        g.drawString("[" + index + "]", x + 10, y + 55);
    }
    
    // [NEW] 버튼 상태를 한 번에 제어하는 헬퍼 메서드
    private void setButtonsEnabled(boolean canDraw, boolean canGuess, boolean canEndTurn) {
        // 드로우 버튼 제어
        b_getBlack.setEnabled(canDraw);
        b_getWhite.setEnabled(canDraw);
        
        // 추측 관련 버튼 제어
        b_guess.setEnabled(canGuess);
        t_guessTarget.setEnabled(canGuess);
        t_guessIndex.setEnabled(canGuess);
        t_guessValue.setEnabled(canGuess);
        
        // 턴 종료 버튼 제어
        b_endTurn.setEnabled(canEndTurn);
    }
}

