package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class LoginPanel extends JPanel {

    private Image backgroundImage;
    private JTextField nicknameField;
    private JButton enter;
    private JLabel statusLabel;

    // 안내 문구 정의
    private final String PLACEHOLDER = "CODE NAME을 입력하세요";

    public LoginPanel(ActionListener originalConnectAction) {
        try {
            backgroundImage = ImageIO.read(new File("./imgs/main_img.jpg"));
        } catch (IOException e) {
            System.out.println("이미지 로드 실패: ./imgs/main.jpg 파일을 확인하세요.");
        }

        // 레이아웃 설정
        setLayout(null); 

        // 입력창
        nicknameField = new JTextField(PLACEHOLDER); // 초기 문구 설정
        nicknameField.setFont(new Font("Malgun Gothic", Font.BOLD, 18));
        nicknameField.setHorizontalAlignment(JTextField.CENTER);
        nicknameField.setOpaque(true);
        nicknameField.setBackground(new Color(40, 40, 50, 240)); 
        nicknameField.setForeground(new Color(180, 180, 180)); 
        nicknameField.setCaretColor(new Color(200, 200, 200));
        nicknameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100, 100, 120), 2, true),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        
        nicknameField.setBounds(350, 220, 300, 45); 

        // 닉네임 포커스 리스너
        nicknameField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (nicknameField.getText().equals(PLACEHOLDER)) {
                    nicknameField.setText("");
                    nicknameField.setForeground(new Color(220, 220, 220));
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (nicknameField.getText().isEmpty()) {
                    nicknameField.setText(PLACEHOLDER);
                    nicknameField.setForeground(new Color(140, 140, 140));
                }
            }
        });
        add(nicknameField);

        // 접속 시도 액션 리스너
        ActionListener validationAction = e -> {
            String currentText = nicknameField.getText().trim();

            // 1. 닉네임이 비어있거나 안내 문구 그대로인 경우
            if (currentText.isEmpty() || currentText.equals(PLACEHOLDER)) {
                JOptionPane.showMessageDialog(this, 
                    "닉네임을 입력해주세요!", 
                    "알림", 
                    JOptionPane.WARNING_MESSAGE);
                
                // 안내 문구 리셋 및 포커스 강제 이동
                nicknameField.requestFocus(); 
                return; // 서버 연결 진행 중단
            }

            // 2. 유효하면 원래 접속 동작 실행
            originalConnectAction.actionPerformed(e);
        };

        // 엔터키 입력
        nicknameField.addActionListener(validationAction);

        // 접속하기 버튼
        enter = new JButton("접속하기");
        enter.setFont(new Font("Malgun Gothic", Font.BOLD, 20));
        enter.setForeground(new Color(220, 220, 220));
        enter.setBackground(new Color(50, 70, 100)); // 어두운 블루
        enter.setContentAreaFilled(true);
        enter.setBorderPainted(true);
        enter.setFocusPainted(false);
        enter.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 100, 130), 2, true),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        enter.setBounds(400, 300, 200, 50);
        
        // 버튼 호버 효과
        enter.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                enter.setBackground(new Color(70, 90, 130));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                enter.setBackground(new Color(50, 70, 100));
            }
        }); 
        
        // 버튼 클릭 시 검사 수행
        enter.addActionListener(validationAction); 
        
        add(enter);

        // 하단 패널
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 10));
        bottomPanel.setBackground(new Color(20, 20, 30, 220));
        statusLabel = new JLabel("서버 연결 대기 중...");
        statusLabel.setForeground(new Color(200, 200, 200));
        statusLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 16));
        bottomPanel.add(statusLabel);
        bottomPanel.setBounds(0, 750, 1000, 50);
        add(bottomPanel);
        
        // 초기 포커스 설정
        this.setFocusable(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }

    public String getNickname() {
        String text = nicknameField.getText().trim();
        if (text.equals(PLACEHOLDER)) {
            return "";
        }
        return text;
    }

    public void setStatus(String status) {
        statusLabel.setText(status);
    }
}