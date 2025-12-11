package client;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.BorderFactory;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class Lobby extends JPanel {
    
    private Image backgroundImage;
    private JButton create, enter;
    private JLabel myInfo;
    private JTable table;
    private JScrollPane scrollPane;
    private DefaultTableModel model;
    
    // 생성자에서 LoginPanel 대신 ActionLi	tener(버튼 동작)를 받습니다.
    public Lobby(ActionListener createAction, ActionListener enterAction) {
        
        try {
            backgroundImage = ImageIO.read(new File("./imgs/main_img.jpg"));
        } catch (IOException e) {
            System.out.println("이미지 로드 실패: ./imgs/main_img.jpg");
        }

        // 절대 레이아웃
        setLayout(null); 
        
        buildGUI(createAction, enterAction);
    }
    
    private void buildGUI(ActionListener createAction, ActionListener enterAction) {
        setList();
        setButton(createAction, enterAction); // 리스너 전달
        setLabel();
    }
    
    /** 방 목록 테이블 */
   private void setList() {
    	// 빈 데이터로 시작
    	String[] headers = {"방 번호", "방 제목", "인원", "상태"};
        model = new DefaultTableModel(null, headers) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 모든 셀 수정 불가
            }
        };

        table = new JTable(model);
        table.setFont(new Font("Malgun Gothic", Font.PLAIN, 14)); // 폰트 설정
        table.setRowHeight(45);
        table.setSelectionBackground(new Color(80, 120, 180, 220));
        table.setSelectionForeground(new Color(220, 220, 220));
        table.setBackground(new Color(40, 40, 50, 240));
        table.setForeground(new Color(200, 200, 200));
        table.setGridColor(new Color(100, 100, 120, 150));
        
        // 가운데 정렬용 렌더러 생성
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                setOpaque(true);
                if (isSelected) {
                    setBackground(new Color(80, 120, 180, 220));
                    setForeground(new Color(220, 220, 220));
                } else {
                    setBackground(row % 2 == 0 ? new Color(40, 40, 50, 240) : new Color(50, 50, 60, 240));
                    setForeground(new Color(200, 200, 200));
                }
                return this;
            }
        };

        // 모든 컬럼에 가운데 정렬 적용
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
        
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setResizingAllowed(false);
        table.getTableHeader().setFont(new Font("Malgun Gothic", Font.BOLD, 15)); // 헤더 폰트
        table.getTableHeader().setBackground(new Color(50, 60, 80));
        table.getTableHeader().setForeground(new Color(220, 220, 220));
        table.getTableHeader().setPreferredSize(new java.awt.Dimension(0, 40));

        scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100, 100, 120), 2, true),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        // 테이블 위치 반드시 지정해야 보임
        scrollPane.setBounds(250, 250, 500, 300);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        add(scrollPane);
    }

    /** 버튼 영역 */
    private void setButton(ActionListener createAction, ActionListener enterAction) {
        create = new JButton("방 만들기");
        create.setBounds(325, 600, 150, 55);
        create.setFont(new Font("Malgun Gothic", Font.BOLD, 17));
        create.setForeground(new Color(220, 220, 220));
        create.setBackground(new Color(50, 70, 100));
        create.setFocusPainted(false);
        create.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 100, 130), 2, true),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        create.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        create.addActionListener(createAction);
        
        // 호버 효과
        create.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                create.setBackground(new Color(70, 90, 130));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                create.setBackground(new Color(50, 70, 100));
            }
        });

        enter = new JButton("입장하기");
        enter.setBounds(525, 600, 150, 55);
        enter.setFont(new Font("Malgun Gothic", Font.BOLD, 17));
        enter.setForeground(new Color(220, 220, 220));
        enter.setBackground(new Color(40, 100, 70)); // 어두운 그린
        enter.setFocusPainted(false);
        enter.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 130, 90), 2, true),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        enter.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        enter.addActionListener(enterAction);
        
        // 호버 효과
        enter.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                enter.setBackground(new Color(50, 120, 90));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                enter.setBackground(new Color(40, 100, 70));
            }
        });
        
        add(create);
        add(enter);
    }
    
    private void setLabel() {
        myInfo = new JLabel("안녕하세요, 플레이어님"); 	
        myInfo.setFont(new Font("Malgun Gothic", Font.BOLD, 20));
        myInfo.setBounds(350, 30, 300, 55);
        myInfo.setOpaque(true);
        myInfo.setBackground(new Color(40, 40, 50, 240));
        myInfo.setForeground(new Color(220, 220, 220));
        myInfo.setHorizontalAlignment(JLabel.CENTER);
        myInfo.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100, 100, 120), 2, true),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        add(myInfo);
    }

    // 로그인 성공 후 닉네임을 갱신하는 메소드 -> 미리 패널 부착해서 변경함
    public void updateGreeting(String nickname) {
        myInfo.setText("안녕하세요, " + nickname + "님");
    }

    // 서버로부터 받은 새로운 방 목록으로 테이블을 갱신
    public void updateRoomList(Vector<Vector<String>> roomData) {
        // 1. 기존 테이블 데이터 싹 지우기
        model.setRowCount(0); 

        // 2. 새로운 데이터 채워넣기
        if (roomData != null) {
            for (Vector<String> row : roomData) {
                model.addRow(row);
            }
        }
    }
    
    // 선택된 방의 번호(ID)를 반환하는 메소드
    public String getSelectedRoomId() {
        int row = table.getSelectedRow();
        if (row == -1) return null; // 선택된 행이 없음
        return (String) table.getValueAt(row, 0); // 0번 컬럼(방 번호) 반환
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }
}