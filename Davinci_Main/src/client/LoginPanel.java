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
    private JButton lockButton;
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
        nicknameField.setOpaque(false); // 배경 투명
        nicknameField.setForeground(Color.GRAY); // 안내 문구
        nicknameField.setCaretColor(Color.black);
        nicknameField.setBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, Color.WHITE));
        
        // 위치 지정
        nicknameField.setBounds(350, 220, 300, 40); 

        // 닉네임 포커스 리스너
        nicknameField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (nicknameField.getText().equals(PLACEHOLDER)) {
                    nicknameField.setText("");
                    nicknameField.setForeground(Color.black);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (nicknameField.getText().isEmpty()) {
                    nicknameField.setText(PLACEHOLDER);
                    nicknameField.setForeground(Color.GRAY);
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

        // 자물쇠 버튼
        lockButton = new JButton();
        lockButton.setContentAreaFilled(false);
        lockButton.setBorderPainted(false);
        lockButton.setFocusPainted(false);
        lockButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lockButton.setBounds(400, 300, 200, 240); 
        
        // 버튼 클릭 시 검사 수행
        lockButton.addActionListener(validationAction); 
        
        add(lockButton);

        // 하단 패널
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.setBackground(new Color(0, 0, 0, 150));
        statusLabel = new JLabel("서버 연결 대기 중...");
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("Malgun Gothic", Font.PLAIN, 16));
        bottomPanel.add(statusLabel);
        bottomPanel.setBounds(0, 725, 1000, 40);
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