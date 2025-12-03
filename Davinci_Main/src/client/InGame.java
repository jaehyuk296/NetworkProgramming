package client;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Vector;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.SwingUtilities;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import protocol.Message;

public class InGame extends JPanel {
    
    private Image backgroundImage;
    private JLabel player1, player2, player3, player4;
    private JLabel[] playerLabels;
    private JTextArea chatArea;
    private JTextField t_input;
    private JButton b_send; // b_start, b_ready, b_exit는 게임 화면에 필요 없으므로 제거했습니다.
    
    private GameClient gameClient;
    
    // 윈도우 종료 이벤트를 감지할 리스너 변수
    private WindowAdapter exitListener;
    
    // ⭐️ [추가] 유저 정보를 저장할 필드
    private Vector<String> initialUserInfos; 
    
    public InGame(GameClient gameClient) {
        this.gameClient = gameClient;
        
        try {
            // [수정] InGame.java는 game_img.jpg로 가정합니다.
            backgroundImage = ImageIO.read(new File("./imgs/game_img.jpg"));
        } catch (IOException e) {
            System.out.println("이미지 로드 실패: ./imgs/game_img.jpg");
        }

        setLayout(null); 
        buildGUI();
        
        // player1~4 객체가 setPlayer()에서 생성된 후 배열 초기화
        playerLabels = new JLabel[] {player1, player2, player3, player4};
    }
    
    // ⭐️ [추가] 초기 유저 정보를 설정하는 생성자 또는 메서드를 GameClient에서 호출해야 합니다.
    // 여기서는 메서드로 구현합니다. 이 메서드는 게임 시작 시 GameClient에서 호출됩니다.
    public void setInitialPlayers(Vector<String> userInfos, String myNickname) {
        this.initialUserInfos = userInfos; // 나중에 필요할 경우 저장
        updatePlayerLabels(userInfos, myNickname); // 바로 라벨 업데이트
    }
    
    private void buildGUI() {
        setPlayer();
//        setButtons(); // InGame 화면에서는 버튼 제거
        setChat();
    }
    
    private void setPlayer() {
        // Player 1 (나/하단)
        player1 = new JLabel("Player1 (나)");
        player1.setBounds(400, 580, 200, 100); 
        player1.setFont(new Font("Malgun Gothic", Font.BOLD, 24));
        player1.setHorizontalAlignment(JLabel.CENTER);
        player1.setOpaque(true); 
        player1.setBackground(Color.lightGray); 
        
        // Player 2 (좌측)
        player2 = new JLabel("Player2");
        player2.setBounds(80, 310, 200, 100); 
        player2.setFont(new Font("Malgun Gothic", Font.BOLD, 24));
        player2.setHorizontalAlignment(JLabel.CENTER);
        player2.setOpaque(true);
        player2.setBackground(Color.lightGray);
        
        // Player 3 (상대방/상단)
        player3 = new JLabel("Player3");
        player3.setBounds(400, 40, 200, 100); 
        player3.setFont(new Font("Malgun Gothic", Font.BOLD, 24));
        player3.setHorizontalAlignment(JLabel.CENTER);
        player3.setOpaque(true);
        player3.setBackground(Color.lightGray);
        
        // Player 4 (우측)
        player4 = new JLabel("Player4");
        player4.setBounds(720, 310, 200, 100); 
        player4.setFont(new Font("Malgun Gothic", Font.BOLD, 24));
        player4.setHorizontalAlignment(JLabel.CENTER);
        player4.setOpaque(true);
        player4.setBackground(Color.lightGray);

        add(player1);
        add(player2);
        add(player3);
        add(player4);
    }
    
    // setButtons()는 InGame에서 불필요하므로 주석 처리된 채로 두거나 삭제합니다.
    
    private void setChat() {
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setBackground(new Color(230, 230, 230));
        
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setBounds(20, 550, 320, 150);
        
        t_input = new JTextField(30);
        t_input.setBounds(20, 710, 250, 30);
        t_input.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                sendMessage();
            }
        });
        
        b_send = new JButton("Send");
        b_send.setFont(new Font("Arial", Font.BOLD, 12));
        b_send.setBounds(275, 710, 65, 30);
        b_send.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                sendMessage();
            }
        });
        
        add(t_input);
        add(b_send);
        add(scrollPane);
    }
    
    // [메시지 전송 기능]
    private void sendMessage() {
        String msgText = t_input.getText().trim();
        if(msgText.length() > 0) {
            gameClient.send(new Message(Message.CHAT_MSG, msgText));
            t_input.setText("");
            t_input.requestFocus();
        }
    }
    
    // [외부 호출용] 서버에서 받은 채팅을 화면에 보여주는 메서드
    public void appendChat(String msg) {
        chatArea.append(msg + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength()); // 자동 스크롤
    }
    
    /**
     * ⭐️ [Waiting.java에서 복사/수정] 서버로부터 받은 유저 정보를 파싱하여 JLabel에 설정합니다.
     * InGame에서는 오직 플레이어들의 닉네임과 위치만 설정합니다.
     * @param userInfos 유저 정보 리스트 (닉네임:READY여부:방장여부 형식의 String Vector)
     * @param myNickname 현재 클라이언트의 닉네임
     */
    public void updatePlayerLabels(Vector<String> userInfos, String myNickname) {
        
        if (userInfos == null) {
            System.err.println("[Client] Initial player payload (userInfos) is null.");
            return;
        }

        // 1. 모든 라벨 초기화 (P1~P4 모두 초기화)
        for (int i = 0; i < playerLabels.length; i++) {
             // ⭐️ InGame에서는 닉네임을 비우고 배경색을 기본으로 설정
            playerLabels[i].setText("자리 비었음");
            playerLabels[i].setBackground(Color.lightGray);
        }
        
        // 0: 나 (하단), 1: 좌측, 2: 상단, 3: 우측
        int currentPositionIndex = 1; // 내 위치(0)를 제외하고 P2(1)부터 순서대로 할당 시작

        // 2. 유저 정보 설정
        for (String info : userInfos) {
            if (info == null || info.isEmpty()) continue; 
            
            String[] parts = info.split(":");
            if (parts.length < 3) continue;

            String nickname = parts[0];
            String isHost = parts[2];
            
            int labelIndex; // 실제로 닉네임을 설정할 JLabel의 인덱스
            
            if (nickname.equals(myNickname)) {
                // 3. 나 자신은 무조건 player1(0번 인덱스, 하단)에 배치
                labelIndex = 0;
            } else {
                // 4. 나머지 유저들은 1, 2, 3번 인덱스에 순서대로 배치
                if (currentPositionIndex <= 3) {
                    labelIndex = currentPositionIndex; 
                    currentPositionIndex++;
                } else {
                    continue; // 4명 초과 시 무시
                }
            }

            // 5. 라벨 업데이트
            JLabel targetLabel = playerLabels[labelIndex];
            
            // InGame에서는 '준비', '대기' 대신 '방장' 여부만 표시하거나 아예 표시하지 않아도 됩니다.
            String status = isHost.equals("HOST") ? " (방장)" : "";
            
            targetLabel.setText(nickname + status);
            
            // 게임 중에는 배경색을 통일시키거나, 게임 상태에 따라 다르게 지정할 수 있습니다.
            // 일단 활성화된 플레이어는 녹색 계열로 설정
            targetLabel.setBackground(new Color(150, 255, 150)); 
        }
        
        // ⭐️ InGame에서는 버튼 로직 (b_start, b_ready)이 불필요하므로 제거했습니다.
    }
    
    // ⭐️ [수정] Window Listener는 GameClient의 WindowListener가 처리해야 하지만, 
    // Waiting.java의 코드를 살리기 위해 남겨둡니다. 다만 게임 중에는 'REQ_EXIT_ROOM'이 아닌 
    // '게임 종료/나가기'에 대한 새로운 프로토콜이 필요할 수 있습니다. 
    // 여기서는 일단 Waiting과 동일하게 EXIT_ROOM 프로토콜을 사용합니다.
    @Override
    public void addNotify() {
        super.addNotify();
        
        Window window = SwingUtilities.getWindowAncestor(this);
        
        if (window != null) {
            if (exitListener == null) {
                exitListener = new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        // X 버튼 종료 시 방 퇴장 요청
                        gameClient.send(new Message(Message.REQ_EXIT_ROOM));
                    }
                };
            }
            window.addWindowListener(exitListener);
        }
    }

    @Override
    public void removeNotify() {
        Window window = SwingUtilities.getWindowAncestor(this);
        
        if (window != null && exitListener != null) {
            window.removeWindowListener(exitListener);
        }
        
        super.removeNotify();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }
}