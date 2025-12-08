package client;

import java.awt.*;
import java.awt.event.*;
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

        b_guess = new JButton("추측");
        b_guess.setBounds(270, 80, 70, 25);
        b_guess.addActionListener(e -> requestGuess());
        actionPanel.add(b_guess);

        add(actionPanel);
        setMyTurn(false); // 기본 비활성화
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

    private void requestGuess() {
        String target = t_guessTarget.getText().trim();
        String idxStr = t_guessIndex.getText().trim();
        String val = t_guessValue.getText().trim();

        if(target.isEmpty() || idxStr.isEmpty() || val.isEmpty()) {
            appendChat("[시스템] 추측 정보를 모두 입력하세요.");
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

                        // 숫자는 모름 -> -1 사용, 색깔만 넣기
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

    // 2. 턴 시작 알림
    public void updateTurn(String currentNick) {
        boolean isMyTurn = currentNick.equals(myNickname);
        setMyTurn(isMyTurn);
        
        String msg = isMyTurn ? "나의 턴! 타일을 가져오거나 추측하세요." : currentNick + "의 턴입니다.";
        lblTurnInfo.setText(msg);
        lblTurnInfo.setBackground(Color.BLACK);
        
        // 타이머 리셋
        remainingTime = 30;
        lblTimer.setText("30sec Remain");
        turnTimer.restart();
    }

    // 3. 타일 뽑기 결과 (내가 뽑았을 때)
    public void handleDrawResult(Tile tile, int index) {
    	
    	// 조커(Joker) 처리: number == -1 이면 조커로 취급
        if (tile.getNumber() == -1) {
            String input = JOptionPane.showInputDialog(
                    this, 
                    "조커가 나왔습니다!\n삽입할 인덱스를 입력하세요.",
                    "JOKER 삽입",
                    JOptionPane.QUESTION_MESSAGE
            );
            
            int insertIndex = myTiles.size(); // 기본: 끝에 삽입
            try {
                if(input != null && !input.trim().isEmpty()) {
                    insertIndex = Integer.parseInt(input.trim());
                }
            } catch (NumberFormatException e) {
                appendChat("[시스템] 숫자가 아닙니다. 자동으로 마지막 위치에 삽입합니다.");
            }

            // 인덱스 보정
            if(insertIndex < 0) insertIndex = 0;
            if(insertIndex > myTiles.size()) insertIndex = myTiles.size();

            myTiles.add(insertIndex, tile);
            repaint();
            appendChat("[시스템] 조커 타일을 " + insertIndex + "번 위치에 삽입했습니다.");
            return;
        }

        // --- 일반 타일은 자동으로 뒤에 붙이기 ---
        myTiles.add(tile);
        
        Collections.sort(myTiles);
        
        repaint();
        appendChat("[시스템] 타일을 가져왔습니다. (" + tile.toString() + ")");
    }

    // 4. 타일 공개 알림 (누군가 맞췄거나 틀렸을 때)
    public void handleReveal(String targetID, int index, Tile tile) {
        if(targetID.equals(myNickname)) {
            // 내 타일이 공개됨
            if(index < myTiles.size()) {
                myTiles.get(index).setRevealed(true);
            }
        } else {
            // 상대방 타일이 공개됨
            Vector<Tile> opHand = opponentHands.get(targetID);
            if(opHand != null) {
                // 인덱스 확장 (혹시 모를 오류 방지)
                while(opHand.size() <= index) opHand.add(null);
                // 해당 위치 타일을 진짜 정보로 교체
                opHand.set(index, tile);
            }
        }
        repaint();
    }
    
    public void appendChat(String msg) {
        chatArea.append(msg + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    private void setMyTurn(boolean myTurn) {
        b_getBlack.setEnabled(myTurn);
        b_getWhite.setEnabled(myTurn);
        b_guess.setEnabled(myTurn);
        t_guessTarget.setEnabled(myTurn);
        t_guessIndex.setEnabled(myTurn);
        t_guessValue.setEnabled(myTurn);
        
        lblTurnInfo.setForeground(myTurn ? Color.RED : Color.WHITE);
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
}