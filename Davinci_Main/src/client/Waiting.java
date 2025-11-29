package client;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class Waiting extends JPanel {
    
    private Image backgroundImage;
    private JLabel player1, player2, player3, player4;
    
    public Waiting() {
        
        try {
            backgroundImage = ImageIO.read(new File("./imgs/main_img.jpg"));
        } catch (IOException e) {
            System.out.println("이미지 로드 실패: ./imgs/main_img.jpg");
        }

        // 절대 레이아웃
        setLayout(null); 
        
        buildGUI();
    }
    
    private void buildGUI() {
        setPlayer();
//        setChat();
    }
    
    /** 방 목록 테이블 */
   private void setPlayer() {
    	player1 = new JLabel("Player1");
    	player1.setBounds(300, 600, 400, 200);
    	player1.setFont(new Font("Malgun Gothic", Font.BOLD, 30));
    }

    /** 버튼 영역 */
//    private void setChat() {
//        create = new JButton("방 만들기");
//        create.setBounds(325, 600, 150, 50);
//        create.setFont(new Font("Malgun Gothic", Font.BOLD, 16));
//        create.addActionListener(createAction); // [수정 2] 버튼에 동작 연결
//
//        enter = new JButton("입장하기");
//        enter.setBounds(525, 600, 150, 50);
//        enter.setFont(new Font("Malgun Gothic", Font.BOLD, 16));
//        enter.addActionListener(enterAction);  // [수정 2] 버튼에 동작 연결
//        
//        add(create);
//        add(enter);
//    }}
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }
}