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
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import javax.swing.*;

public class GameServer extends JFrame {

    // 어디서든 접근 가능한 정적 인스턴스
    public static GameServer instance;

    // UI 컴포넌트
    private JTabbedPane tabbedPane;
    private JTextArea mainLogArea;
    private JComboBox<String> filterCombo;
    
    private Map<Integer, JTextArea> roomLogs = new HashMap<>();
    private ArrayList<String> logHistory = new ArrayList<>();
    private SimpleDateFormat sdf = new SimpleDateFormat("[HH:mm:ss] ");

    public GameServer() {
        super("다빈치코드 서버 (관리자 모드)");
        instance = this;

        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // [상단] 필터링 패널
        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("메인 로그 필터: "));
        String[] filters = {"ALL (전체)", "SYS (시스템)", "CONN (접속/퇴장)", "CHAT (채팅)", "GAME (게임로직)", "ERR (에러)"};
        filterCombo = new JComboBox<>(filters);
        filterCombo.addActionListener(e -> refreshLogArea());
        topPanel.add(filterCombo);
        add(topPanel, BorderLayout.NORTH);

        // [중앙] 탭 패널 구성
        tabbedPane = new JTabbedPane();
        mainLogArea = createLogArea();
        tabbedPane.addTab("메인 로그", new JScrollPane(mainLogArea));
        add(tabbedPane, BorderLayout.CENTER);
        
        setVisible(true); // ★ 창을 화면에 띄움 ★

        // 콘솔 출력을 GUI로 납치
        redirectSystemStreams();

        // 서버 로직 스레드 시작
        new Thread(this::runServer).start();
    }

    private JTextArea createLogArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 14)); 
        return area;
    }

    // 방 탭 추가
    public void addRoomTab(int roomId, String title) {
        SwingUtilities.invokeLater(() -> {
            if (roomLogs.containsKey(roomId)) return;

            JTextArea roomLog = createLogArea();
            roomLogs.put(roomId, roomLog);
            tabbedPane.addTab("Room " + roomId, new JScrollPane(roomLog));
            roomLog.append("=== [" + title + "] 방이 생성되었습니다 ===\n");
        });
    }

    // 방 탭 삭제
    public void removeRoomTab(int roomId) {
        SwingUtilities.invokeLater(() -> {
            int index = tabbedPane.indexOfTab("Room " + roomId);
            if (index != -1) {
                tabbedPane.removeTabAt(index);
            }
            roomLogs.remove(roomId);
        });
    }

    // 방 로그 추가
    public void appendRoomLog(int roomId, String msg) {
        JTextArea area = roomLogs.get(roomId);
        if (area != null) {
            SwingUtilities.invokeLater(() -> {
                String timeMsg = sdf.format(new Date()) + msg + "\n";
                area.append(timeMsg);
                area.setCaretPosition(area.getDocument().getLength());
            });
        }
    }

    // 서버 실행 로직
    private void runServer() {
        Lobby serverLobby = new Lobby();
        int port = 9999;

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

    // 로그 처리
    private void addLog(String text) {
        String timeLog = text.startsWith("[") ? text : sdf.format(new Date()) + text;
        logHistory.add(timeLog);
        
        if (shouldShow(timeLog)) {
            mainLogArea.append(timeLog);
            mainLogArea.setCaretPosition(mainLogArea.getDocument().getLength());
        }
    }

    private void refreshLogArea() {
        mainLogArea.setText("");
        for (String log : logHistory) {
            if (shouldShow(log)) {
                mainLogArea.append(log);
            }
        }
    }

    private boolean shouldShow(String log) {
        String filter = (String) filterCombo.getSelectedItem();
        if (filter == null || filter.startsWith("ALL")) return true;
        String keyword = filter.split(" ")[0]; 
        return log.contains("[" + keyword + "]");
    }

    private void redirectSystemStreams() {
        OutputStream out = new OutputStream() {
            private ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            @Override
            public void write(int b) throws IOException {
                if (b == '\n') {
                    String line = buffer.toString("UTF-8"); 
                    buffer.reset();
                    SwingUtilities.invokeLater(() -> addLog(line + "\n"));
                } else {
                    buffer.write(b);
                }
            }
        };
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