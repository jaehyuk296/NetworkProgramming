package server;

import java.awt.BorderLayout;
import java.awt.Font;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;
import javax.swing.*;

public class GameServer extends JFrame {

    private JTextArea logArea;
    private JComboBox<String> filterCombo;
    
    // 모든 로그 기록을 저장할 리스트
    private ArrayList<String> logHistory = new ArrayList<>();
    
    // 날짜 포맷
    private SimpleDateFormat sdf = new SimpleDateFormat("[HH:mm:ss] ");

    public GameServer() {
        super("다빈치코드 서버 (관리자 모드)");
        setSize(800, 600); // 화면 키움
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // [상단] 필터링 패널
        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("로그 필터: "));
        
        // 필터 종류 설정
        String[] filters = {"ALL (전체)", "SYS (시스템)", "CONN (접속/퇴장)", "CHAT (채팅)", "GAME (게임로직)", "ERR (에러)"};
        filterCombo = new JComboBox<>(filters);
        
        // 필터 선택 시 화면 갱신 이벤트
        filterCombo.addActionListener(e -> refreshLogArea());
        
        topPanel.add(filterCombo);
        add(topPanel, BorderLayout.NORTH);

        // [중앙] 로그 영역
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Malgun Gothic", Font.PLAIN, 14));
        add(new JScrollPane(logArea), BorderLayout.CENTER);
        
        setVisible(true);

        redirectSystemStreams();

        // 서버 로직 스레드 시작
        new Thread(this::runServer).start();
    }

    // 서버 실행 로직
    private void runServer() {
        Lobby serverLobby = new Lobby();
        int port = 9999;

        // server.txt 읽기
        try {
            File file = new File("server.txt");
            if (file.exists()) {
                Scanner sc = new Scanner(file);
                if (sc.hasNext()) sc.next(); 
                if (sc.hasNextInt()) {
                    port = sc.nextInt();
                    System.out.println("[SYS] 설정 파일 로드 완료 (Port: " + port + ")");
                }
                sc.close();
            } else {
                System.out.println("[SYS] server.txt 없음. 기본 포트(9999) 사용");
            }
        } catch (Exception e) {
            System.out.println("[ERR] 설정 파일 읽기 실패. 기본 포트 사용");
        }

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[SYS] 서버 시작됨 (Port: " + port + ")");
            
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("[CONN] 클라이언트 접속: " + socket.getInetAddress());
                new ClientHandler(socket, serverLobby).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 로그 처리 및 필터링 로직

    private void addLog(String text) {
        String timeLog = sdf.format(new Date()) + text;
        
        // 기록에 저장
        logHistory.add(timeLog);
        
        // 현재 필터에 맞으면 화면에 출력
        if (shouldShow(timeLog)) {
            logArea.append(timeLog);
            // 자동 스크롤
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }
    }

    private void refreshLogArea() {
        logArea.setText(""); // 화면 지우기
        for (String log : logHistory) {
            if (shouldShow(log)) {
                logArea.append(log);
            }
        }
        // 스크롤 맨 아래로
        if (logArea.getDocument().getLength() > 0) {
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }
    }

    private boolean shouldShow(String log) {
        String filter = (String) filterCombo.getSelectedItem();
        if (filter.startsWith("ALL")) return true;
        
        // 필터링 키워드 추출 (SYS, CONN, CHAT 등)
        String keyword = filter.split(" ")[0]; 
        return log.contains("[" + keyword + "]");
    }

    // 시스템 출력
    private void redirectSystemStreams() {
        OutputStream out = new OutputStream() {
            // 라인 단위로 로그를 끊어서 처리하기 위한 버퍼
            private ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            @Override
            public void write(int b) throws IOException {
                if (b == '\n') {
                    // 엔터를 만나면 한 줄 완성 -> 로그 추가
                    String line = buffer.toString("UTF-8"); // 한글 깨짐 방지
                    buffer.reset();
                    
                    // GUI 스레드에서 처리
                    SwingUtilities.invokeLater(() -> addLog(line + "\n"));
                } else {
                    buffer.write(b);
                }
            }
        };

        // System.out과 System.err 모두 이쪽으로 연결
        try {
            PrintStream ps = new PrintStream(out, true, "UTF-8");
            System.setOut(ps);
            System.setErr(ps);
        } catch (Exception e) {}
    }

    public static void main(String[] args) {
        new GameServer();
    }
}