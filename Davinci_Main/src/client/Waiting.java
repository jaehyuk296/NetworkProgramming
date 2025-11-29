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

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
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
    
    // [핵심] 통신을 담당하는 메인 클라이언트 참조
    private GameClient gameClient;
    
    // 생성자에서 GameClient를 전달받습니다.
    public Waiting(GameClient gameClient) {
        this.gameClient = gameClient;
        
        try {
            backgroundImage = ImageIO.read(new File("./imgs/main_img.jpg"));
        } catch (IOException e) {
            // 원본 코드는 game_img.jpg였으나, 실제 파일명은 main_img.jpg로 가정합니다.
            System.out.println("이미지 로드 실패: ./imgs/main_img.jpg");
        }

        setLayout(null); 
        buildGUI();
        
        // player1~4 객체가 setPlayer()에서 생성된 후 배열 초기화
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
        player1.setFont(new Font("Malgun Gothic", Font.BOLD, 24));
        player1.setHorizontalAlignment(JLabel.CENTER);
        player1.setOpaque(true); 
        player1.setBackground(Color.lightGray); // 초기 대기
        
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
    
    private void setButtons() {
        // 시작하기 버튼 (HOST 전용)
        b_start = new JButton("게임 시작");
        b_start.setBounds(400, 700, 200, 40);
        b_start.setFont(new Font("Malgun Gothic", Font.BOLD, 18));
        b_start.setVisible(false); // 처음에는 숨김
        b_start.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // 서버로 시작 요청을 보냅니다. (서버에서 최종적으로 모두 준비되었는지 확인해야 합니다.)
                gameClient.send(new Message(Message.REQ_READY, "START"));
            }
        });

        add(b_start);

        // 준비하기 버튼 (일반 플레이어)
        b_ready = new JButton("준비하기");
        b_ready.setBounds(400, 700, 200, 40);
        b_ready.setFont(new Font("Malgun Gothic", Font.BOLD, 18));
        b_ready.setVisible(false); // 처음에는 숨김
        b_ready.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                gameClient.send(new Message(Message.REQ_READY, "READY"));
            }
        });
        add(b_ready);
        
        b_exit = new JButton("나가기");
        b_exit.setBounds(20, 20, 100, 40);
        b_exit.setFont(new Font("Malgun Gothic", Font.BOLD, 16));

        b_exit.addActionListener(e -> {
            // 1. 서버에 퇴장 알림
            gameClient.send(new Message(Message.REQ_EXIT_ROOM));

            // 2. 클라이언트 화면 전환 요청
            gameClient.showLobby();
        });

        add(b_exit);
    }
    
    private void setChat() {
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setBackground(new Color(230, 230, 230));
        
        // 채팅창 스크롤바
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setBounds(20, 550, 320, 150);
        
        // 입력 필드
        t_input = new JTextField(30);
        t_input.setBounds(20, 710, 250, 30);
        t_input.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                sendMessage();
            }
        });
        
        // 전송 버튼
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
     * 서버로부터 받은 유저 정보를 파싱하여 JLabel에 설정합니다.
     * @param userInfos 유저 정보 리스트 (닉네임:READY여부:방장여부 형식의 String Vector)
     * @param myNickname 현재 클라이언트의 닉네임
     */
    public void updatePlayers(Vector<String> userInfos, String myNickname) {
        
        if (userInfos == null) {
            System.err.println("[Client] ROOM_UPDATE payload (userInfos) is null.");
            // P2, P3, P4만 초기화하고 함수 종료
            for (int i = 1; i < playerLabels.length; i++) {
                playerLabels[i].setText("대기 중...");
                playerLabels[i].setBackground(Color.lightGray);
            }
            return;
        }

        // 1. 모든 라벨 초기화 (핵심 수정: P1을 제외하고 P2, P3, P4만 초기화)
        for (int i = 1; i < playerLabels.length; i++) {
            playerLabels[i].setText("대기 중...");
            playerLabels[i].setBackground(Color.lightGray);
        }
        
        // 0: 나 (하단), 1: 좌측, 2: 상단, 3: 우측
        int currentPositionIndex = 1; // 내 위치(0)를 제외하고 P2(1)부터 순서대로 할당 시작

        // 🎯 [추가] 시작 버튼 활성화 판단을 위한 변수
        boolean amIHost = false;
        int totalPlayers = 0;
        int readyPlayers = 0;
        
        // 2. 유저 정보 설정 및 상태 카운트
        for (String info : userInfos) {
            if (info == null || info.isEmpty()) continue; 
            
            String[] parts = info.split(":");
            if (parts.length < 3) continue;

            String nickname = parts[0];
            String isReady = parts[1];
            String isHost = parts[2];
            
            // 🎯 카운트 로직 추가
            totalPlayers++; // 방에 있는 모든 유저 카운트
            if (isReady.equals("ON") || isHost.equals("HOST")) {
                 // 방장은 항상 준비 상태로 간주
                readyPlayers++;
            }
            if (nickname.equals(myNickname) && isHost.equals("HOST")) {
                amIHost = true;
            }
            
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
                    continue; 
                }
            }

            // 5. 라벨 업데이트
            JLabel targetLabel = playerLabels[labelIndex];
            
            // 방장은 "준비" 대신 "(방장)"을 표시하는 것이 자연스러움
            String status;
            if (isHost.equals("HOST")) {
                 status = " (방장)";
            } else {
                status = isReady.equals("ON") ? " (준비)" : " (대기)";
            }

            targetLabel.setText(nickname + status);
            
            // 배경색 업데이트
            if (isReady.equals("ON") || isHost.equals("HOST")) {
                targetLabel.setBackground(Color.ORANGE);
            } else {
                targetLabel.setBackground(new Color(150, 255, 150)); 
            }
        }
        
        // ✅ 버튼 표시 및 활성화 로직
        
        // 1. 방장/준비 버튼 가시성 설정
        if (amIHost) {
            b_start.setVisible(true);
            b_ready.setVisible(false);
            
            // 2. 시작 버튼 활성화 조건 확인
            // 조건: 최소 2명 이상 AND (총 유저 수 == 준비된 유저 수)
            boolean allReady = (totalPlayers >= 2 && totalPlayers == readyPlayers);
            
            // 3. 버튼 활성화/비활성화
            b_start.setEnabled(allReady);
            
        } else {
            b_start.setVisible(false);
            b_ready.setVisible(true);
            b_ready.setEnabled(true); // 일반 유저는 항상 준비 가능
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