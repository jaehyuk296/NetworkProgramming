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
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import protocol.Message;

public class Waiting extends JPanel {
    
    private Image backgroundImage;
    private JLabel player1, player2, player3, player4;
    private JLabel[] playerLabels;
    private JTextArea chatArea;
    private JTextField t_input;
    private JButton b_send, b_start, b_ready, b_exit;
    
    private GameClient gameClient;
    
    // 윈도우 종료 이벤트를 감지할 리스너 변수
    private WindowAdapter exitListener;
    
    public Waiting(GameClient gameClient) {
        this.gameClient = gameClient;
        
        try {
            backgroundImage = ImageIO.read(new File("./imgs/game_img.jpg")); // 출처: Canva AI 이미지 생성
        } catch (IOException e) {
            System.out.println("이미지 로드 실패: ./imgs/game_img.jpg");
        }

        setLayout(null); 
        buildGUI();
        
        playerLabels = new JLabel[] {player1, player2, player3, player4};
    }
    
    private void buildGUI() {
        setPlayer();
        setButtons();
        setChat();
    }
    
    private void setPlayer() {
        // Player 1 (나/하단)
        player1 = new JLabel("Player1 (나)");
        player1.setBounds(400, 580, 200, 100); 
        player1.setFont(new Font("Malgun Gothic", Font.BOLD, 22));
        player1.setHorizontalAlignment(JLabel.CENTER);
        player1.setOpaque(true); 
        player1.setBackground(new Color(50, 50, 60, 240));
        player1.setForeground(new Color(220, 220, 220));
        // **외부 참조** BorderFactory
        player1.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100, 100, 120), 2, true),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        
        // Player 2 (상단) - 두 번째 플레이어
        player2 = new JLabel("Player2");
        player2.setBounds(400, 40, 200, 100); 
        player2.setFont(new Font("Malgun Gothic", Font.BOLD, 22));
        player2.setHorizontalAlignment(JLabel.CENTER);
        player2.setOpaque(true);
        player2.setBackground(new Color(50, 50, 60, 240));
        player2.setForeground(new Color(220, 220, 220));
        // **외부 참조** BorderFactory
        player2.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100, 100, 120), 2, true),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        
        // Player 3 (좌측) - 세 번째 플레이어
        player3 = new JLabel("Player3");
        player3.setBounds(80, 310, 200, 100); 
        player3.setFont(new Font("Malgun Gothic", Font.BOLD, 22));
        player3.setHorizontalAlignment(JLabel.CENTER);
        player3.setOpaque(true);
        player3.setBackground(new Color(50, 50, 60, 240));
        player3.setForeground(new Color(220, 220, 220));
        // **외부 참조** BorderFactory
        player3.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100, 100, 120), 2, true),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        
        // Player 4 (우측) - 네 번째 플레이어
        player4 = new JLabel("Player4");
        player4.setBounds(720, 310, 200, 100); 
        player4.setFont(new Font("Malgun Gothic", Font.BOLD, 22));
        player4.setHorizontalAlignment(JLabel.CENTER);
        player4.setOpaque(true);
        player4.setBackground(new Color(50, 50, 60, 240));
        player4.setForeground(new Color(220, 220, 220));
        // **외부 참조** BorderFactory
        player4.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100, 100, 120), 2, true),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));

        add(player1);
        add(player2);
        add(player3);
        add(player4);
    }
    
    private void setButtons() {
        // 시작하기 버튼 (방정 전용)
        b_start = new JButton("게임 시작");
        b_start.setBounds(400, 700, 200, 50);
        b_start.setFont(new Font("Malgun Gothic", Font.BOLD, 19));
        b_start.setForeground(new Color(220, 220, 220));
        b_start.setBackground(new Color(40, 100, 70));
        b_start.setFocusPainted(false);
        // **외부 참조** BorderFactory
        b_start.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 130, 90), 2, true),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        b_start.setVisible(false);
        b_start.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                gameClient.send(new Message(Message.REQ_START_GAME));
            }
        });
        
        // **외부 참조** MouseAdapter
        // 호버 효과
        b_start.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                b_start.setBackground(new Color(50, 120, 90));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                b_start.setBackground(new Color(40, 100, 70));
            }
        });

        add(b_start);

        // 준비하기 버튼 (일반 플레이어)
        b_ready = new JButton("준비하기");
        b_ready.setBounds(400, 700, 200, 50);
        b_ready.setFont(new Font("Malgun Gothic", Font.BOLD, 19));
        b_ready.setForeground(new Color(220, 220, 220));
        b_ready.setBackground(new Color(50, 70, 100));
        b_ready.setFocusPainted(false);
        // **외부 참조** BorderFactory
        b_ready.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 100, 130), 2, true),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        b_ready.setVisible(false);
        b_ready.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                gameClient.send(new Message(Message.REQ_READY, "READY"));
            }
        });
        
        // **외부 참조** MouseAdapter
        // 호버 효과
        b_ready.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                b_ready.setBackground(new Color(70, 90, 130));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                b_ready.setBackground(new Color(50, 70, 100));
            }
        });
        
        add(b_ready);
        
        // 나가기 버튼
        b_exit = new JButton("나가기");
        b_exit.setBounds(20, 20, 120, 45);
        b_exit.setFont(new Font("Malgun Gothic", Font.BOLD, 16));
        b_exit.setForeground(new Color(220, 220, 220));
        b_exit.setBackground(new Color(150, 40, 50));
        b_exit.setFocusPainted(false);
        // **외부 참조** BorderFactory
        b_exit.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 60, 70), 2, true),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        b_exit.addActionListener(e -> {
            gameClient.send(new Message(Message.REQ_EXIT_ROOM));
            gameClient.showLobby();
        });
        
        // **외부 참조** MouseAdapter
        // 호버 효과
        b_exit.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                b_exit.setBackground(new Color(170, 50, 60));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                b_exit.setBackground(new Color(150, 40, 50));
            }
        });

        add(b_exit);
    }
    
    private void setChat() {
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setBackground(new Color(40, 40, 50, 240));
        chatArea.setForeground(new Color(200, 200, 200));
        chatArea.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        // **외부 참조** BorderFactory
        chatArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 채팅창 스크롤바
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setBounds(20, 550, 320, 150);
        // **외부 참조** BorderFactory
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100, 100, 120), 2, true),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        
        // 입력 필드
        t_input = new JTextField(30);
        t_input.setBounds(20, 710, 250, 35);
        t_input.setFont(new Font("Malgun Gothic", Font.PLAIN, 13));
        t_input.setBackground(new Color(40, 40, 50, 240));
        t_input.setForeground(new Color(200, 200, 200));
        // **외부 참조** BorderFactory
        t_input.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100, 100, 120), 2, true),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        t_input.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                sendMessage();
            }
        });
        
        // 전송 버튼
        b_send = new JButton("전송");
        b_send.setFont(new Font("Malgun Gothic", Font.BOLD, 13));
        b_send.setBounds(275, 710, 65, 35);
        b_send.setForeground(new Color(220, 220, 220));
        b_send.setBackground(new Color(50, 70, 100));
        b_send.setFocusPainted(false);
        // **외부 참조** BorderFactory
        b_send.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 100, 130), 2, true),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        b_send.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                sendMessage();
            }
        });
        
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
        
        add(t_input);
        add(b_send);
        add(scrollPane);
    }
    
    // 메시지 전송 기능
    private void sendMessage() {
        String msgText = t_input.getText().trim();
        if(msgText.length() > 0) {
            gameClient.send(new Message(Message.CHAT_MSG, msgText));
            t_input.setText("");
            t_input.requestFocus();
        }
    }
    
    // 서버에서 받은 채팅을 화면에 보여주는 메서드
    public void appendChat(String msg) {
        chatArea.append(msg + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength()); // 자동 스크롤
    }
    
    // 플레이어 업데이트 메서드
    public void updatePlayers(Vector<String> userInfos, String myNickname) {
        if (userInfos == null) {
            for (int i = 1; i < playerLabels.length; i++) {
                playerLabels[i].setText("대기 중...");
                playerLabels[i].setBackground(new Color(50, 50, 60, 240));
                playerLabels[i].setForeground(new Color(220, 220, 220));
            }
            return;
        }

        // 모든 라벨 초기화 (P1을 제외하고 P2, P3, P4만 초기화)
        for (int i = 1; i < playerLabels.length; i++) {
            playerLabels[i].setText("대기 중...");
            playerLabels[i].setBackground(new Color(50, 50, 60, 240));
            playerLabels[i].setForeground(new Color(220, 220, 220));
        }
        
        // 0: 나 (하단), 1: 상단, 2: 좌측, 3: 우측
        int[] positionMapping = {0, 1, 2, 3};
        int playerOrder = 0; // 플레이어 순서 카운터 (나를 제외한 순서)

        boolean amIHost = false;
        int totalPlayers = 0;
        int readyPlayers = 0;
        
        // 유저 정보 설정 및 상태 카운트
        for (String info : userInfos) {
            if (info == null || info.isEmpty()) continue; 
            
            String[] parts = info.split(":");
            if (parts.length < 3) continue;

            String nickname = parts[0];
            String isReady = parts[1];
            String isHost = parts[2];
            
            totalPlayers++; // 방에 있는 모든 유저 카운트
            if (isReady.equals("ON") || isHost.equals("HOST")) {
                readyPlayers++;
            }
            if (nickname.equals(myNickname) && isHost.equals("HOST")) {
                amIHost = true;
            }
            
            int labelIndex; // 실제로 닉네임을 설정할 JLabel 인덱스
            
            if (nickname.equals(myNickname)) {
                // 본인은 무조건 하단에 배치
                labelIndex = 0;
            } else {
                // 나머지 유저들은 순서대로 배치)
                playerOrder++;
                if (playerOrder <= 3) {
                    labelIndex = positionMapping[playerOrder];
                } else {
                    continue; 
                }
            }
            
            JLabel targetLabel = playerLabels[labelIndex];

            String status;
            if (isHost.equals("HOST")) {
                 status = " (방장)";
            } else {
                status = isReady.equals("ON") ? " (준비)" : " (대기)";
            }

            targetLabel.setText(nickname + status);
            
            if (isReady.equals("ON") || isHost.equals("HOST")) {
                targetLabel.setBackground(new Color(150, 100, 50, 240)); // 준비 완료 - 어두운 오렌지
                targetLabel.setForeground(new Color(220, 220, 220));
            } else {
                targetLabel.setBackground(new Color(50, 70, 50, 240)); // 대기 중 - 어두운 초록
                targetLabel.setForeground(new Color(220, 220, 220));
            }
        }
        
        // 방장/준비 버튼 가시성 설정
        if (amIHost) {
            b_start.setVisible(true);
            b_ready.setVisible(false);
            
            // 시작 버튼 활성화 조건 확인
            boolean allReady = (totalPlayers >= 2 && totalPlayers == readyPlayers);
            
            // 버튼 활성화/비활성화
            b_start.setEnabled(allReady);
            
        } else {
            b_start.setVisible(false);
            b_ready.setVisible(true);
            b_ready.setEnabled(true);
        }
    }
    
    // **외부 참조** WindowAdapter
    @Override
    public void addNotify() {
        super.addNotify();
        
        // 현재 이 패널을 담고 있는 부모 윈도우(JFrame)를 가져옴
        Window window = SwingUtilities.getWindowAncestor(this);
        
        if (window != null) {
            // 리스너가 중복 생성되지 않도록 초기화
            if (exitListener == null) {
                exitListener = new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        // X 버튼을 눌렀을 때
                        gameClient.send(new Message(Message.REQ_EXIT_ROOM));
                    }
                };
            }
            // 윈도우에 리스너 부착
            window.addWindowListener(exitListener);
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }
}