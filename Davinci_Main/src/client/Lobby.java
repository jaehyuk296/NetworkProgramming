package client;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class Lobby extends JPanel {
    
    private Image backgroundImage;
    private JButton create, enter;
    private JLabel myInfo;
    private JTable table;
    private JScrollPane scrollPane;
    
    // [수정 1] 생성자에서 LoginPanel 대신 ActionLi	tener(버튼 동작)를 받습니다.
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
        DefaultTableModel model = new DefaultTableModel(
            new Object[][] {
                {"1", "코난방", "2/4", "대기"},
                {"2", "다빈치갓겜방", "4/4", "시작"},
                {"3", "호영방", "3/4", "대기"},
            },
            new String[] {"방 번호", "방 제목", "인원", "상태"}
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 모든 셀 수정 불가
            }
        };

        table = new JTable(model);
        table.setFont(new Font("Malgun Gothic", Font.PLAIN, 14)); // 폰트 설정
        
        // 가운데 정렬용 렌더러 생성
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        // 모든 컬럼에 가운데 정렬 적용
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
        table.setRowHeight(40);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setResizingAllowed(false);
        table.getTableHeader().setFont(new Font("Malgun Gothic", Font.BOLD, 14)); // 헤더 폰트

        scrollPane = new JScrollPane(table);

        // ⭐ 테이블 위치 반드시 지정해야 보임
        scrollPane.setBounds(250, 250, 500, 300);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBackground(new Color(255, 255, 255, 150)); // 마지막 값 0~255 = 투명도

        add(scrollPane);
    }

    /** 버튼 영역 */
    private void setButton(ActionListener createAction, ActionListener enterAction) {
        create = new JButton("방 만들기");
        create.setBounds(325, 600, 150, 50);
        create.setFont(new Font("Malgun Gothic", Font.BOLD, 16));
        create.addActionListener(createAction); // [수정 2] 버튼에 동작 연결

        enter = new JButton("입장하기");
        enter.setBounds(525, 600, 150, 50);
        enter.setFont(new Font("Malgun Gothic", Font.BOLD, 16));
        enter.addActionListener(enterAction);  // [수정 2] 버튼에 동작 연결
        
        add(create);
        add(enter);
    }
    
    private void setLabel() {
        myInfo = new JLabel("안녕하세요, 플레이어님"); 	
        myInfo.setFont(new Font("Malgun Gothic", Font.BOLD, 18));
        myInfo.setBounds(350, 30, 300, 50); // y좌표 살짝 내림 (30)
        myInfo.setOpaque(true);
        myInfo.setBackground(new Color(255, 255, 255, 200)); // 배경 반투명
        myInfo.setHorizontalAlignment(JLabel.CENTER);
        add(myInfo);
    }

    // [수정 3] 로그인 성공 후 닉네임을 갱신하는 메소드 -> 미리 패널 부착해서 변경함
    public void updateGreeting(String nickname) {
        myInfo.setText("안녕하세요, " + nickname + "님");
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }
}