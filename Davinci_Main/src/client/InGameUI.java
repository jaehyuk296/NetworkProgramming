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
import protocol.Tile; 

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
    
    // ★ [변경] 텍스트 필드 -> 콤보박스 교체
    private JComboBox<String> cb_guessTarget; // 대상 ID 선택
    private JComboBox<String> cb_guessIndex;  // 인덱스 선택
    private JComboBox<String> cb_guessValue;  // 숫자(0~11, JOKER) 선택
    
    private JButton b_guess;
    
    private JLabel lblTimer;
    private Timer turnTimer;
    private int remainingTime = 300;

    // --- 게임 데이터 ---
    private String myNickname;
    private Vector<Tile> myTiles = new Vector<>();
    private Map<String, Vector<Tile>> opponentHands = new HashMap<>();
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
        // 1. 플레이어 라벨
        playerLabels = new JLabel[4];
        int[][] pos = { {400, 550}, {50, 250}, {400, 30}, {750, 250} }; 
        
        for(int i=0; i<4; i++) {
            playerLabels[i] = new JLabel("", JLabel.CENTER);
            playerLabels[i].setBounds(pos[i][0], pos[i][1], 200, 40);
            playerLabels[i].setFont(new Font("Malgun Gothic", Font.BOLD, 16));
            playerLabels[i].setOpaque(true);
            playerLabels[i].setBackground(new Color(255, 255, 255, 150));
            playerLabels[i].setVisible(false); 
            add(playerLabels[i]);
        }
        playerLabels[0].setText("나 (Player1)"); 

        // 2. 채팅창
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

        // 4. 타이머
        lblTimer = new JLabel("대기 중...", JLabel.CENTER);
        lblTimer.setBounds(400, 300, 200, 30);
        lblTimer.setFont(new Font("Arial", Font.BOLD, 20));
        lblTimer.setForeground(Color.YELLOW);
        add(lblTimer);
        
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
        gameClient.send(new Message(Message.TURN_TIMEOUT, myNickname));
    }

    private void buildActionPanel() {
        actionPanel = new JPanel();
        actionPanel.setLayout(null);
        actionPanel.setBounds(600, 600, 360, 170);
        actionPanel.setBackground(new Color(0, 0, 0, 150)); 
        
        // 턴 표시
        lblTurnInfo = new JLabel("게임을 시작합니다.");
        lblTurnInfo.setBackground(Color.BLACK);
        lblTurnInfo.setForeground(Color.WHITE);
        lblTurnInfo.setBounds(10, 5, 300, 20);
        actionPanel.add(lblTurnInfo);

        // 타일 가져오기 버튼
        b_getBlack = new JButton("Black");
        b_getBlack.setBackground(Color.BLACK);
        b_getBlack.setForeground(Color.WHITE);
        b_getBlack.setBounds(10, 30, 80, 30);
        b_getBlack.addActionListener(e -> requestDraw("BLACK"));
        actionPanel.add(b_getBlack);

        b_getWhite = new JButton("White");
        b_getWhite.setBackground(Color.WHITE);
        b_getWhite.setForeground(Color.BLACK);
        b_getWhite.setBounds(100, 30, 80, 30);
        b_getWhite.addActionListener(e -> requestDraw("WHITE"));
        actionPanel.add(b_getWhite);
        
        // 턴 종료 버튼
        b_endTurn = new JButton("턴 종료");
        b_endTurn.setBackground(Color.RED);
        b_endTurn.setForeground(Color.WHITE);
        b_endTurn.setBounds(250, 30, 100, 30); 
        b_endTurn.setEnabled(false);
        b_endTurn.addActionListener(e -> requestEndTurn());
        actionPanel.add(b_endTurn);

        // --- 추측 하기 UI (좌표 재조정으로 공간 확보) ---
        
        // 1. 대상 선택 (ID)
        JLabel l2 = new JLabel("ID:");
        l2.setForeground(Color.WHITE);
        l2.setBounds(10, 80, 20, 20);
        actionPanel.add(l2);
        
        cb_guessTarget = new JComboBox<>();
        // 너비를 90 -> 80으로 줄임
        cb_guessTarget.setBounds(30, 80, 80, 20); 
        cb_guessTarget.addActionListener(e -> updateIndexComboBox());
        actionPanel.add(cb_guessTarget);
        
        // 2. 인덱스 선택 (Idx)
        JLabel l3 = new JLabel("Idx:");
        l3.setForeground(Color.WHITE);
        // 위치를 왼쪽으로 5px 당김
        l3.setBounds(115, 80, 25, 20); 
        actionPanel.add(l3);

        cb_guessIndex = new JComboBox<>();
        // 위치 당김
        cb_guessIndex.setBounds(140, 80, 45, 20); 
        actionPanel.add(cb_guessIndex);

        // 3. 숫자 선택 (Num)
        JLabel l4 = new JLabel("Num:");
        l4.setForeground(Color.WHITE);
        // 위치 당김
        l4.setBounds(190, 80, 35, 20); 
        actionPanel.add(l4);
        
        String[] values = {"0","1","2","3","4","5","6","7","8","9","10","11","JOKER"};
        cb_guessValue = new JComboBox<>(values);
        // 위치 당김, 너비 65 -> 60으로 미세 조정
        cb_guessValue.setBounds(225, 80, 60, 20); 
        actionPanel.add(cb_guessValue);

        // 4. 추측 버튼 (문제 해결 부분)
        b_guess = new JButton("추측");
        // ★ [핵심 1] 너비를 50 -> 60으로 늘림 (위치는 290으로 당김)
        b_guess.setBounds(290, 80, 60, 25);
        // ★ [핵심 2] 폰트를 조금 줄이고 진하게 설정
        b_guess.setFont(new Font("Malgun Gothic", Font.BOLD, 12));
        // ★ [핵심 3] 버튼 내부의 불필요한 여백 제거 (이게 제일 효과 큽니다)
        b_guess.setMargin(new Insets(0, 0, 0, 0));
        
        b_guess.addActionListener(e -> requestGuess());
        actionPanel.add(b_guess);

        add(actionPanel);
        setButtonsEnabled(false, false, false);
    }
    
    // ★ [추가] 대상 유저의 타일 개수에 맞춰 인덱스 콤보박스를 갱신하는 메서드
    private void updateIndexComboBox() {
        String target = (String) cb_guessTarget.getSelectedItem();
        if (target == null) return;
        
        Vector<Tile> hand = opponentHands.get(target);
        if (hand == null) return;
        
        cb_guessIndex.removeAllItems();
        for(int i = 0; i < hand.size(); i++) {
            cb_guessIndex.addItem(String.valueOf(i));
        }
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
        gameClient.send(new Message(Message.REQ_DRAW, color + ":-1"));
    }
    
    private void requestEndTurn() {
        gameClient.send(new Message(Message.REQ_END_TURN, myNickname));
    }

    // ★ [수정] 콤보박스 값 읽어서 추측 요청
    private void requestGuess() {
        String target = (String) cb_guessTarget.getSelectedItem();
        String idxStr = (String) cb_guessIndex.getSelectedItem();
        String val = (String) cb_guessValue.getSelectedItem();

        if(target == null || idxStr == null || val == null) {
            appendChat("[System] 추측 정보를 모두 선택하세요.");
            return;
        }
        
        // 조커 처리
        if(val.equals("JOKER")) {
            val = "-1";
        }
        
        gameClient.send(new Message(Message.REQ_GUESS, target + ":" + idxStr + ":" + val));
    }

    // --- 게임 로직 및 UI 업데이트 ---

    public void startGame(Vector<Tile> myHand, Vector<String> players, Map<String, Vector<String>> otherColors, String myNick) {
        this.myTiles = new Vector<>(myHand);
        this.myNickname = myNick;
        this.opponentHands.clear();
        this.userPositionMap.clear();

        // 1. 타일 및 위치 데이터 초기화
        int posIndex = 1;
        for(String pName : players) {
            if(pName.equals(myNickname)) {
                userPositionMap.put(pName, 0); 
                playerLabels[0].setText(pName + " (나)");
                playerLabels[0].setVisible(true);
            } else {
                if(posIndex <= 3) {
                    userPositionMap.put(pName, posIndex);
                    playerLabels[posIndex].setText(pName);
                    playerLabels[posIndex].setVisible(true);
                    
                    Vector<Tile> dummyHand = new Vector<>();
                    Vector<String> colorList = otherColors.get(pName);
                    for(int k = 0; k < colorList.size(); k++) { // 4 대신 size() 사용
                        String c = colorList.get(k);
                        Tile t = new Tile(c, -999);
                        dummyHand.add(t);
                    }
                    opponentHands.put(pName, dummyHand);
                    
                    posIndex++;
                }
            }
        }
        
        // ★ [추가] 게임 시작 시 추측 대상(Target) 콤보박스 세팅 (나는 제외)
        cb_guessTarget.removeAllItems();
        for(String pName : players) {
            if(!pName.equals(myNickname)) {
                cb_guessTarget.addItem(pName);
            }
        }
        // 첫 번째 유저 선택 시 인덱스 자동 갱신됨 (ActionListener 덕분)

        appendChat("=== 게임 시작 ===");
        repaint();
    }

    public void updateTurn(String currentNick) {
        boolean isMyTurn = currentNick.equals(myNickname);
        
        if (isMyTurn) {
            lblTurnInfo.setText("나의 턴! 타일을 가져오세요.");
            lblTurnInfo.setForeground(Color.RED);
            // 내 턴 시작: 드로우만 가능
            setButtonsEnabled(true, false, false);
            
        } else {
            lblTurnInfo.setText(currentNick + "의 턴입니다.");
            lblTurnInfo.setForeground(Color.WHITE);
            // 남의 턴: 아무것도 못함
            setButtonsEnabled(false, false, false);
        }
        
        lblTurnInfo.setBackground(Color.BLACK);
        remainingTime = 300;
        lblTimer.setText("300sec Remain");
        turnTimer.restart();
    }

    public void handleDrawResult(Tile tile, int index) {
        if (tile.getNumber() == -1) {
            String input = JOptionPane.showInputDialog(this, "조커! 삽입할 인덱스 입력:", "JOKER", JOptionPane.QUESTION_MESSAGE);
            int insertIndex = myTiles.size();
            try {
                if(input != null && !input.trim().isEmpty()) insertIndex = Integer.parseInt(input.trim());
            } catch (NumberFormatException e) { }

            if(insertIndex < 0) insertIndex = 0;
            if(insertIndex > myTiles.size()) insertIndex = myTiles.size();

            myTiles.add(insertIndex, tile);
            sortTilesExcludingJokers(myTiles);
            
            gameClient.send(new Message(Message.REQ_JOKER_POSITION, tile.getColor() + ":" + insertIndex));
        } else {
            myTiles.add(tile);
            sortTilesExcludingJokers(myTiles);
        }
        
        repaint();
        appendChat("[System] 타일을 가져왔습니다.");
        
        // ★ 드로우 후: 추측만 가능 (턴 종료 불가 - 룰 적용)
        setButtonsEnabled(false, true, false); 
        lblTurnInfo.setText("타일을 배치했습니다. 반드시 추리를 시도해야 합니다.");
    }
    
    private void sortTilesExcludingJokers(Vector<Tile> tiles) {
        Vector<Integer> jokerIndices = new Vector<>();
        Vector<Tile> jokers = new Vector<>();
        for (int i = tiles.size() - 1; i >= 0; i--) {
            Tile t = tiles.get(i);
            if (t.isJoker()) {
                jokerIndices.add(0, i); 
                jokers.add(0, tiles.remove(i));
            }
        }
        Collections.sort(tiles);
        for (int i = jokers.size() - 1; i >= 0; i--) {
            int originalIndex = jokerIndices.get(i);
            if (originalIndex > tiles.size()) originalIndex = tiles.size();
            tiles.add(originalIndex, jokers.get(i));
        }
    }
    
    public void handleOpponentDraw(String nickname, String color, int index) {
        if (nickname.equals(myNickname)) return;

        Vector<Tile> hand = opponentHands.get(nickname);
        if (hand == null) return;

        Tile secretTile = new Tile(color, -999);
        if (index < 0) index = 0;
        if (index > hand.size()) index = hand.size();
        
        hand.add(index, secretTile);
        
        // ★ [추가] 만약 현재 콤보박스에서 선택된 유저라면, 인덱스 목록 갱신 (개수가 늘었으니까)
        String selected = (String) cb_guessTarget.getSelectedItem();
        if(selected != null && selected.equals(nickname)) {
            updateIndexComboBox();
        }

        repaint();
    }

    public void handleReveal(String targetID, int index, Tile tile) {
        if(targetID.equals(myNickname)) {
            // 내 타일 공개 (조커 처리 포함)
            if(tile.isJoker()) {
                for(Tile t : myTiles) {
                    if(t.isJoker() && t.getColor().equals(tile.getColor())) {
                        t.setRevealed(true);
                        break;
                    }
                }
            } else {
                for(Tile t : myTiles) {
                    if(t.getNumber() == tile.getNumber() && t.getColor().equals(tile.getColor()) && !t.isJoker()) {
                        t.setRevealed(true);
                        break;
                    }
                }
            }
        } else {
            // 상대 타일 공개
            Vector<Tile> opHand = opponentHands.get(targetID);
            if(opHand != null) {
                while(opHand.size() <= index) opHand.add(null);
                if(opHand.get(index) != null) opHand.set(index, tile);
                else opHand.add(index, tile);
            }
        }
        
        // ★ 추리 성공 시 '턴 종료' 버튼 활성화 (룰 적용)
        if (lblTurnInfo.getText().contains("나의 턴") || b_guess.isEnabled()) {
            if (!targetID.equals(myNickname)) {
                appendChat("[System] 정답! 계속 추리하거나 턴을 종료하세요.");
                setButtonsEnabled(false, true, true); // (Draw X, Guess O, EndTurn O)
            }
        }
        repaint();
    }
    
    public void appendChat(String msg) {
        chatArea.append(msg + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }

        // 내 타일
        drawTileList(g, myTiles, playerLabels[0].getX() - 50, playerLabels[0].getY() - 70, true);

        // 상대 타일
        for(Map.Entry<String, Integer> entry : userPositionMap.entrySet()) {
            String nick = entry.getKey();
            int posIdx = entry.getValue();
            if(posIdx == 0) continue; 

            Vector<Tile> hand = opponentHands.get(nick);
            if(hand == null) continue;

            JLabel lbl = playerLabels[posIdx];
            drawTileList(g, hand, lbl.getX(), lbl.getY() + 50, false);
        }
    }

    private void drawTileList(Graphics g, Vector<Tile> tiles, int startX, int startY, boolean isMe) {
        int w = 40, h = 60, gap = 5;
        for (int i = 0; i < tiles.size(); i++) {
            Tile t = tiles.get(i);
            int x = startX + i * (w + gap);
            int y = startY;

            if (isMe) {
                if (t != null) drawSingleTile(g, x, y, w, h, t.getColor(), t.getNumber(), t.isRevealed(), i);
            } else {
                if (t.isRevealed()) drawSingleTile(g, x, y, w, h, t.getColor(), t.getNumber(), true, i);
                else drawBackTile(g, x, y, w, h, t.getColor(), i);
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

        String numStr = (number == -1) ? "-" : String.valueOf(number);
        g.setColor(fontC);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString(numStr, x + 10, y + 35);
        
        g.setFont(new Font("Arial", Font.PLAIN, 10));
        g.setColor(Color.RED);
        g.drawString("" + index, x + 15, y + 55);

        if(isRev) {
            g.setColor(Color.RED);
            g.drawRect(x-1, y-1, w+2, h+2); 
        }
    }

    private void drawBackTile(Graphics g, int x, int y, int w, int h, String colorStr, int index) {
        boolean isBlack = "BLACK".equals(colorStr);
        Color bgColor = isBlack ? Color.BLACK : Color.WHITE;
        Color lineColor = isBlack ? Color.WHITE : Color.BLACK; 

        g.setColor(bgColor);
        g.fillRect(x, y, w, h);
        g.setColor(Color.DARK_GRAY);
        g.drawRect(x, y, w, h);

        g.setColor(lineColor);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("?", x + 15, y + 35); 

        g.setFont(new Font("Arial", Font.PLAIN, 10));
        g.drawString("[" + index + "]", x + 10, y + 55);
    }
    
    // ★ [수정] 버튼 및 콤보박스 활성화/비활성화 제어
    private void setButtonsEnabled(boolean canDraw, boolean canGuess, boolean canEndTurn) {
        b_getBlack.setEnabled(canDraw);
        b_getWhite.setEnabled(canDraw);
        
        b_guess.setEnabled(canGuess);
        cb_guessTarget.setEnabled(canGuess);
        cb_guessIndex.setEnabled(canGuess);
        cb_guessValue.setEnabled(canGuess);
        
        b_endTurn.setEnabled(canEndTurn);
    }
}