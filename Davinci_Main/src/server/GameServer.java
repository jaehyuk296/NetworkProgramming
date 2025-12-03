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

    public static GameServer instance;

    // UI 컴포넌트
    private JTabbedPane tabbedPane;
    private JComboBox<String> filterCombo;
    
    // [핵심 변경] 방의 로그 데이터와 GUI를 묶어서 관리하는 클래스 정의
    private class RoomData {
        JTextArea area;
        ArrayList<String> history = new ArrayList<>(); // 이 방만의 로그 기록
        
        public RoomData(JTextArea area) {
            this.area = area;
        }
    }

    // 메인 로그용
    private JTextArea mainLogArea;
    private ArrayList<String> mainLogHistory = new ArrayList<>();

    // 방 관리용 맵 (ID -> RoomData)
    private Map<Integer, RoomData> roomDataMap = new HashMap<>();
    
    private SimpleDateFormat sdf = new SimpleDateFormat("[HH:mm:ss] ");

    public GameServer() {
        super("다빈치코드 서버 (관리자 모드)");
        instance = this;

        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // [상단] 필터링 패널
        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("로그 필터 (모든 탭 적용): "));
        
        String[] filters = {"ALL (전체)", "SYS (시스템)", "CONN (접속/퇴장)", "CHAT (채팅)", "GAME (게임로직)", "ERR (에러)"};
        filterCombo = new JComboBox<>(filters);
        
        // 필터 변경 시 모든 탭 새로고침
        filterCombo.addActionListener(e -> refreshAllLogs());
        
        topPanel.add(filterCombo);
        add(topPanel, BorderLayout.NORTH);

        // [중앙] 탭 패널
        tabbedPane = new JTabbedPane();

        // 1. 메인 로그 탭
        mainLogArea = createLogArea();
        tabbedPane.addTab("메인 로그", new JScrollPane(mainLogArea));

        add(tabbedPane, BorderLayout.CENTER);
        
        setVisible(true);

        redirectSystemStreams();

        new Thread(this::runServer).start();
    }

    private JTextArea createLogArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 14)); 
        return area;
    }

    // 방 탭 추가 (데이터 구조 변경됨)
    public void addRoomTab(int roomId, String title) {
        SwingUtilities.invokeLater(() -> {
            if (roomDataMap.containsKey(roomId)) return;

            JTextArea roomLogArea = createLogArea();
            
            // 데이터 객체 생성 및 맵에 저장
            RoomData data = new RoomData(roomLogArea);
            roomDataMap.put(roomId, data);
            
            // 탭 추가
            tabbedPane.addTab("Room " + roomId, new JScrollPane(roomLogArea));
            
            // 초기 로그 기록
            String initMsg = "=== [" + title + "] 방이 생성되었습니다 ===\n";
            data.history.add(initMsg);
            roomLogArea.append(initMsg);
        });
    }

    // 방 탭 제거
    public void removeRoomTab(int roomId) {
        SwingUtilities.invokeLater(() -> {
            int index = tabbedPane.indexOfTab("Room " + roomId);
            if (index != -1) {
                tabbedPane.removeTabAt(index);
            }
            roomDataMap.remove(roomId);
        });
    }

    // 방 로그 추가 (필터링 적용)
    public void appendRoomLog(int roomId, String msg) {
        RoomData data = roomDataMap.get(roomId);
        if (data != null) {
            SwingUtilities.invokeLater(() -> {
                String timeMsg = sdf.format(new Date()) + msg + "\n";
                
                // 1. 역사에 저장 (무조건)
                data.history.add(timeMsg);
                
                // 2. 현재 필터에 맞으면 화면에도 출력
                if (shouldShow(timeMsg)) {
                    data.area.append(timeMsg);
                    data.area.setCaretPosition(data.area.getDocument().getLength());
                }
            });
        }
    }

    // --- [로그 시스템] ---

    // 메인 로그 추가
    private void addMainLog(String text) {
        String timeLog = text.startsWith("[") ? text : sdf.format(new Date()) + text;
        
        mainLogHistory.add(timeLog);
        
        if (shouldShow(timeLog)) {
            mainLogArea.append(timeLog);
            mainLogArea.setCaretPosition(mainLogArea.getDocument().getLength());
        }
    }

    // ★ [핵심] 모든 로그 새로고침 (필터 변경 시 호출)
    private void refreshAllLogs() {
        // 1. 메인 로그 갱신
        refreshTextArea(mainLogArea, mainLogHistory);
        
        // 2. 모든 방 로그 갱신
        for (RoomData data : roomDataMap.values()) {
            refreshTextArea(data.area, data.history);
        }
    }

    // 특정 텍스트 에어리어 갱신 로직
    private void refreshTextArea(JTextArea area, ArrayList<String> history) {
        area.setText(""); // 싹 지우고
        for (String log : history) {
            if (shouldShow(log)) { // 필터 통과한 것만 다시 씀
                area.append(log);
            }
        }
        if (area.getDocument().getLength() > 0) {
            area.setCaretPosition(area.getDocument().getLength());
        }
    }

    // 필터 판별 로직
    private boolean shouldShow(String log) {
        String filter = (String) filterCombo.getSelectedItem();
        if (filter == null || filter.startsWith("ALL")) return true;
        
        String keyword = filter.split(" ")[0]; 
        return log.contains("[" + keyword + "]");
    }

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

    private void redirectSystemStreams() {
        OutputStream out = new OutputStream() {
            private ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            @Override
            public void write(int b) throws IOException {
                if (b == '\n') {
                    String line = buffer.toString("UTF-8"); 
                    buffer.reset();
                    SwingUtilities.invokeLater(() -> addMainLog(line + "\n"));
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