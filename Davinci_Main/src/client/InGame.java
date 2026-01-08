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

public class InGame extends JPanel { 
    private Image backgroundImage; 
    private GameClient gameClient; 
    
    // UI 컴포넌트
    private JLabel[] playerLabels;
    private JTextArea chatArea; 
    private JTextField t_chatInput; 
    
    // 액션 패널 컴포넌트
    private JPanel actionPanel;
    private JLabel lblTurnInfo;
    private JButton b_getBlack, b_getWhite;
    private JButton b_endTurn;
    private JButton b_guess;
    
    // 콤보박스
    private JComboBox<String> cb_guessTarget; // 대상 ID 선택
    private JComboBox<String> cb_guessIndex;  // 인덱스 선택
    private JComboBox<String> cb_guessValue;  // 숫자(0~11, JOKER) 선택
    
    // 타이머
    private JLabel lblTimer;
    private Timer turnTimer;
    private int remainingTime = 300;

    // 게임 데이터
    private String myNickname;
    private Vector<Tile> myTiles = new Vector<>();
    private Map<String, Vector<Tile>> opponentHands = new HashMap<>();
    private Map<String, Integer> userPositionMap = new HashMap<>();

    public InGame(GameClient gameClient) { 
        this.gameClient = gameClient; 
        try { 
            backgroundImage = ImageIO.read(new File("./imgs/game_img.jpg")); // 출처: Canva AI 이미지 생성
        } catch (IOException e) { 
            System.out.println("이미지 로드 실패: ./imgs/game_img.jpg"); 
        } 
        setLayout(null);
        buildGUI(); 
    } 

    private void buildGUI() { 
    	setPlayer(); // 플레이어 라벨
    	setChat(); // 채팅창
    	buildActionPanel(); // 액션 패널 (우측 하단)
    	setTimer(); // 타이머 설정
    }
    
    private void setPlayer() {
    	playerLabels = new JLabel[4];
        int[][] pos = { {400, 550}, {400, 30}, {100, 250}, {700, 250} }; 
        
        for(int i=0; i<4; i++) {
            playerLabels[i] = new JLabel("", JLabel.CENTER);
            playerLabels[i].setBounds(pos[i][0], pos[i][1], 200, 45);
            playerLabels[i].setFont(new Font("Malgun Gothic", Font.BOLD, 17));
            playerLabels[i].setOpaque(true);
            playerLabels[i].setBackground(new Color(50, 50, 60, 240));
            playerLabels[i].setForeground(new Color(220, 220, 220));
            // **외부 참조** BorderFactory
            playerLabels[i].setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 120), 2, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
            ));
            playerLabels[i].setVisible(false); 
            add(playerLabels[i]);
        }
        playerLabels[0].setText("나 (Player1)");
    }
    
    private void setChat() {
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setBackground(new Color(40, 40, 50, 240));
        chatArea.setForeground(new Color(200, 200, 200));
        chatArea.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        // **외부 참조** BorderFactory
        chatArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JScrollPane scroll = new JScrollPane(chatArea);
        scroll.setBounds(20, 580, 300, 120);
        // **외부 참조** BorderFactory
        scroll.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100, 100, 120), 2, true),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        add(scroll);

        t_chatInput = new JTextField();
        t_chatInput.setBounds(20, 710, 220, 35);
        t_chatInput.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        t_chatInput.setBackground(new Color(40, 40, 50, 240));
        t_chatInput.setForeground(new Color(200, 200, 200));
        // **외부 참조** BorderFactory
        t_chatInput.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100, 100, 120), 2, true),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        t_chatInput.addActionListener(e -> sendMessage());
        add(t_chatInput);

        JButton b_send = new JButton("전송");
        b_send.setBounds(250, 710, 70, 35);
        b_send.setFont(new Font("Malgun Gothic", Font.BOLD, 13));
        b_send.setForeground(new Color(220, 220, 220));
        b_send.setBackground(new Color(50, 70, 100));
        b_send.setFocusPainted(false);
        // **외부 참조** BorderFactory
        b_send.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 100, 130), 2, true),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        b_send.addActionListener(e -> sendMessage());
        
        // **외부 참조** MouseAdapter
        // 호버 효과
        b_send.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                b_send.setBackground(new Color(70, 90, 130));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                b_send.setBackground(new Color(50, 70, 100));
            }
        });
        
        add(b_send);
    }

    private void buildActionPanel() {
        actionPanel = new JPanel();
        actionPanel.setLayout(null);
        actionPanel.setBounds(620, 600, 380, 180);
        actionPanel.setBackground(new Color(30, 30, 40, 220));
        // **외부 참조** BorderFactory
        actionPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100, 100, 120), 2, true),
            BorderFactory.createEmptyBorder(10, 10, 10, 20) // 오른쪽 여백 10 -> 20으로 증가
        ));
        
        // 턴 표시
        lblTurnInfo = new JLabel("게임을 시작합니다.");
        lblTurnInfo.setFont(new Font("Malgun Gothic", Font.BOLD, 14));
        lblTurnInfo.setForeground(new Color(220, 220, 180));
        lblTurnInfo.setBounds(10, 5, 340, 25);
        actionPanel.add(lblTurnInfo);

        // 타일 가져오기 버튼
        b_getBlack = new JButton("Black");
        b_getBlack.setFont(new Font("Malgun Gothic", Font.BOLD, 13));
        b_getBlack.setBackground(new Color(30, 30, 30));
        b_getBlack.setForeground(Color.WHITE);
        b_getBlack.setFocusPainted(false);
        // **외부 참조** BorderFactory
        b_getBlack.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 50, 60), 2, true),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        b_getBlack.setBounds(10, 35, 85, 35);
        b_getBlack.addActionListener(e -> requestDraw("BLACK"));
        
        // **외부 참조** MouseAdapter
        // 호버 효과
        b_getBlack.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                b_getBlack.setBackground(new Color(50, 50, 50));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                b_getBlack.setBackground(new Color(30, 30, 30));
            }
        });
        
        actionPanel.add(b_getBlack);

        b_getWhite = new JButton("White");
        b_getWhite.setFont(new Font("Malgun Gothic", Font.BOLD, 13));
        b_getWhite.setBackground(new Color(120, 120, 130));
        b_getWhite.setForeground(new Color(220, 220, 220));
        b_getWhite.setFocusPainted(false);
        // **외부 참조** BorderFactory
        b_getWhite.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(150, 150, 160), 2, true),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        b_getWhite.setBounds(105, 35, 85, 35);
        b_getWhite.addActionListener(e -> requestDraw("WHITE"));
        
        // **외부 참조** MouseAdapter
        // 호버 효과
        b_getWhite.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                b_getWhite.setBackground(new Color(140, 140, 150));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                b_getWhite.setBackground(new Color(120, 120, 130));
            }
        });
        
        actionPanel.add(b_getWhite);
        
        // 턴 종료 버튼
        b_endTurn = new JButton("턴 종료");
        b_endTurn.setFont(new Font("Malgun Gothic", Font.BOLD, 13));
        b_endTurn.setBackground(new Color(150, 40, 50));
        b_endTurn.setForeground(new Color(220, 220, 220));
        b_endTurn.setFocusPainted(false);
        // **외부 참조** BorderFactory
        b_endTurn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 60, 70), 2, true),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        b_endTurn.setBounds(250, 35, 100, 35); // 버튼 크기와 위치는 그대로
        b_endTurn.setEnabled(false);
        b_endTurn.addActionListener(e -> requestEndTurn());
        
        // **외부 참조** MouseAdapter
        // 호버 효과
        b_endTurn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (b_endTurn.isEnabled()) {
                    b_endTurn.setBackground(new Color(170, 50, 60));
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (b_endTurn.isEnabled()) {
                    b_endTurn.setBackground(new Color(150, 40, 50));
                }
            }
        });
        
        actionPanel.add(b_endTurn);
        
        // 추측하기 UI
        // ID 선택
        JLabel id = new JLabel("ID:");
        id.setForeground(new Color(220, 220, 220));
        id.setFont(new Font("Malgun Gothic", Font.BOLD, 12));
        id.setBounds(10, 85, 25, 20);
        actionPanel.add(id);
        
        cb_guessTarget = new JComboBox<>();
        cb_guessTarget.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
        cb_guessTarget.setBackground(new Color(40, 40, 50, 240));
        cb_guessTarget.setForeground(new Color(200, 200, 200));
        cb_guessTarget.setBounds(35, 82, 80, 25); 
        cb_guessTarget.addActionListener(e -> updateIndexComboBox());
        actionPanel.add(cb_guessTarget);
        
        // 인덱스 선택
        JLabel idx = new JLabel("Idx:");
        idx.setForeground(new Color(220, 220, 220));
        idx.setFont(new Font("Malgun Gothic", Font.BOLD, 12));
        idx.setBounds(120, 85, 30, 20); 
        actionPanel.add(idx);

        cb_guessIndex = new JComboBox<>();
        cb_guessIndex.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
        cb_guessIndex.setBackground(new Color(40, 40, 50, 240));
        cb_guessIndex.setForeground(new Color(200, 200, 200));
        cb_guessIndex.setBounds(150, 82, 45, 25); 
        actionPanel.add(cb_guessIndex);

        // 숫자 선택
        JLabel num = new JLabel("Num:");
        num.setForeground(new Color(220, 220, 220));
        num.setFont(new Font("Malgun Gothic", Font.BOLD, 12));
        num.setBounds(200, 85, 35, 20); 
        actionPanel.add(num);
        
        String[] values = {"0","1","2","3","4","5","6","7","8","9","10","11","JOKER"};
        cb_guessValue = new JComboBox<>(values);
        cb_guessValue.setFont(new Font("Malgun Gothic", Font.PLAIN, 12));
        cb_guessValue.setBackground(new Color(40, 40, 50, 240));
        cb_guessValue.setForeground(new Color(200, 200, 200));
        cb_guessValue.setBounds(235, 82, 60, 25); 
        actionPanel.add(cb_guessValue);

        // 추측 버튼
        b_guess = new JButton("추측");
        b_guess.setBounds(290, 80, 60, 28);
        b_guess.setFont(new Font("Malgun Gothic", Font.BOLD, 13));
        b_guess.setForeground(new Color(220, 220, 220));
        b_guess.setBackground(new Color(40, 100, 70));
        b_guess.setFocusPainted(false);
        // **외부 참조** BorderFactory
        b_guess.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 130, 90), 2, true),
            BorderFactory.createEmptyBorder(3, 8, 3, 8)
        ));
        
        // **외부 참조** MouseAdapter
        // 호버 효과
        b_guess.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (b_guess.isEnabled()) {
                    b_guess.setBackground(new Color(50, 120, 90));
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (b_guess.isEnabled()) {
                    b_guess.setBackground(new Color(40, 100, 70));
                }
            }
        });
        
        b_guess.addActionListener(e -> requestGuess());
        actionPanel.add(b_guess);

        add(actionPanel);
        setButtonsEnabled(false, false, false);
    }
    
    private void setTimer() {
        lblTimer = new JLabel("대기 중...", JLabel.CENTER);
        lblTimer.setBounds(400, 300, 200, 40);
        lblTimer.setFont(new Font("Malgun Gothic", Font.BOLD, 22));
        lblTimer.setForeground(new Color(255, 200, 100));
        lblTimer.setOpaque(true);
        lblTimer.setBackground(new Color(30, 30, 40, 220));
        // **외부 참조** BorderFactory
        lblTimer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(150, 120, 80), 2, true),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
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
    
    // 대상 유저의 타일 개수에 맞춰 인덱스 콤보박스를 갱신하는 메서드
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

    private void sendMessage() {
        String msg = t_chatInput.getText().trim();
        if(!msg.isEmpty()) {
            gameClient.send(new Message(Message.CHAT_MSG, msg));
            t_chatInput.setText("");
        }
    }

    // 타일 뽑기 요청
    private void requestDraw(String color) {
        gameClient.send(new Message(Message.REQ_DRAW, color + ":-1"));
    }
    
    // 턴 종료 요청
    private void requestEndTurn() {
        gameClient.send(new Message(Message.REQ_END_TURN, myNickname));
    }

    // 콤보박스 값 읽어서 추측 요청
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

    // 게임 로직
    public void startGame(Vector<Tile> myHand, Vector<String> players, Map<String, Vector<String>> otherColors, String myNick) {
        this.myTiles = new Vector<>(myHand);
        this.myNickname = myNick;
        this.opponentHands.clear();
        this.userPositionMap.clear();

        // 타일 및 위치 데이터 초기화
        int[] positionMapping = {0, 1, 2, 3};
        int playerOrder = 0;
        
        for(String pName : players) {
            if(pName.equals(myNickname)) {
                userPositionMap.put(pName, 0); 
                playerLabels[0].setText(pName + " (나)");
                playerLabels[0].setVisible(true);
            } else {
                playerOrder++;
                if(playerOrder <= 3) {
                    int positionIndex = positionMapping[playerOrder];
                    userPositionMap.put(pName, positionIndex);
                    playerLabels[positionIndex].setText(pName);
                    playerLabels[positionIndex].setVisible(true);
                    
                    Vector<Tile> dummyHand = new Vector<>();
                    Vector<String> colorList = otherColors.get(pName);
                    for(int k = 0; k < colorList.size(); k++) {
                        String c = colorList.get(k);
                        Tile t = new Tile(c, -999);
                        dummyHand.add(t);
                    }
                    opponentHands.put(pName, dummyHand);
                }
            }
        }
        
        // 추측 대상 콤보박스 세팅
        cb_guessTarget.removeAllItems();
        for(String pName : players) {
            if(!pName.equals(myNickname)) {
                cb_guessTarget.addItem(pName);
            }
        }
        
        appendChat("=== 게임 시작 ===");
        repaint();
    }

    // 턴 갱신
    public void updateTurn(String currentNick) {
        boolean isMyTurn = currentNick.equals(myNickname);
        
        if (isMyTurn) {
            lblTurnInfo.setText("나의 턴! 타일을 가져오세요.");
            lblTurnInfo.setForeground(new Color(255, 150, 150));
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

    // 타일 뽑기 결과
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
        
        setButtonsEnabled(false, true, false); 
        lblTurnInfo.setText("타일을 배치했습니다. 반드시 추리를 시도해야 합니다.");
    }
    
    // 조커를 제외한 정렬 메서드
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
    
    // 상대 타일 뽑기 정렬 (색상만)
    public void handleOpponentDraw(String nickname, String color, int index) {
        if (nickname.equals(myNickname)) return;

        Vector<Tile> hand = opponentHands.get(nickname);
        if (hand == null) return;

        Tile secretTile = new Tile(color, -999);
        if (index < 0) index = 0;
        if (index > hand.size()) index = hand.size();
        
        hand.add(index, secretTile);
        
        // 만약 현재 콤보박스에서 선택된 유저라면, 인덱스 목록 갱신
        String selected = (String) cb_guessTarget.getSelectedItem();
        if(selected != null && selected.equals(nickname)) {
            updateIndexComboBox();
        }

        repaint();
    }

    // 타일 공개
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
        
        // 추리 성공 시 '턴 종료' 버튼 활성화
        if (lblTurnInfo.getText().contains("나의 턴") || b_guess.isEnabled()) {
            if (!targetID.equals(myNickname)) {
                appendChat("[System] 정답! 계속 추리하거나 턴을 종료하세요.");
                setButtonsEnabled(false, true, true);
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

        // 내 타일 - 플레이어 라벨 기준 가운데 정렬
        if (playerLabels[0] != null && playerLabels[0].isVisible()) {
            int labelCenterX = playerLabels[0].getX() + playerLabels[0].getWidth() / 2;
            drawTileList(g, myTiles, labelCenterX, playerLabels[0].getY() - 70, true);
        }

        // 상대 타일 - 각 플레이어 라벨 기준 가운데 정렬
        for(Map.Entry<String, Integer> entry : userPositionMap.entrySet()) {
            String nick = entry.getKey();
            int posIdx = entry.getValue();
            if(posIdx == 0) continue; 

            Vector<Tile> hand = opponentHands.get(nick);
            if(hand == null) continue;

            JLabel lbl = playerLabels[posIdx];
            if (lbl != null && lbl.isVisible()) {
                int labelCenterX = lbl.getX() + lbl.getWidth() / 2;
                drawTileList(g, hand, labelCenterX, lbl.getY() + 50, false);
            }
        }
    }

    private void drawTileList(Graphics g, Vector<Tile> tiles, int centerX, int startY, boolean isMe) {
        if (tiles == null || tiles.isEmpty()) return;
        
        int w = 40, h = 60, gap = 5;
        int totalWidth = tiles.size() * w + (tiles.size() - 1) * gap;
        int startX = centerX - totalWidth / 2;
        
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
    
    // 버튼 및 콤보박스 활성화/비활성화 제어
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