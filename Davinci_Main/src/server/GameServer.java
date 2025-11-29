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

    // ★ [핵심 1] 어디서든 접근 가능한 정적 인스턴스 (싱글톤 효과)
    public static GameServer instance;

    // UI 컴포넌트
    private JTabbedPane tabbedPane; // 탭 관리자
    private JTextArea mainLogArea;  // 메인(전체) 로그 (기존 logArea)
    private JComboBox<String> filterCombo;
    
    // 방 번호(ID)와 해당 방의 로그창(TextArea)을 매핑
    private Map<Integer, JTextArea> roomLogs = new HashMap<>();
    
    // 모든 로그 기록을 저장할 리스트 (메인 로그용)
    private ArrayList<String> logHistory = new ArrayList<>();
    
    // 날짜 포맷
    private SimpleDateFormat sdf = new SimpleDateFormat("[HH:mm:ss] ");

    public GameServer() {
        super("다빈치코드 서버 (관리자 모드)");
        instance = this; // ★ 나 자신을 전역 변수에 할당

        setSize(900, 600); // 탭 공간 확보를 위해 조금 더 키움
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // [상단] 필터링 패널
        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("메인 로그 필터: "));
        
        // 필터 종류 설정
        String[] filters = {"ALL (전체)", "SYS (시스템)", "CONN (접속/퇴장)", "CHAT (채팅)", "GAME (게임로직)", "ERR (에러)"};
        filterCombo = new JComboBox<>(filters);
        
        // 필터 선택 시 화면 갱신 이벤트
        filterCombo.addActionListener(e -> refreshLogArea());
        
        topPanel.add(filterCombo);
        add(topPanel, BorderLayout.NORTH);

        // [중앙] 탭 패널 구성
        tabbedPane = new JTabbedPane();

        // 1. 메인 로그 탭 생성
        mainLogArea = createLogArea();
        tabbedPane.addTab("메인 로그", new JScrollPane(mainLogArea));

        add(tabbedPane, BorderLayout.CENTER);
        
        setVisible(true);

        redirectSystemStreams();

        // 서버 로직 스레드 시작
        new Thread(this::runServer).start();
    }

    // [유틸] 로그창(TextArea) 생성 헬퍼 메서드 (통일된 디자인)
    private JTextArea createLogArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        // 가독성을 위해 고정폭 글꼴 사용 권장
        area.setFont(new Font("Monospaced", Font.PLAIN, 14)); 
        return area;
    }

    // ★ [기능 1] 새로운 방 탭 추가 (Lobby에서 방 만들 때 호출)
    public void addRoomTab(int roomId, String title) {
        SwingUtilities.invokeLater(() -> {
            // 이미 존재하면 생성 안 함
            if (roomLogs.containsKey(roomId)) return;

            JTextArea roomLog = createLogArea();
            roomLogs.put(roomId, roomLog);
            
            // 탭 추가: "Room [번호]"
            tabbedPane.addTab("Room " + roomId, new JScrollPane(roomLog));
            
            // 탭 내부에 기본 정보 출력
            roomLog.append("=== [" + title + "] 방이 생성되었습니다 ===\n");
            
            // (선택사항) 방금 만든 탭으로 자동 이동하고 싶으면 아래 주석 해제
            // tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
        });
    }

    // ★ [기능 2] 방 탭 삭제 (방 폭파 시 호출)
    public void removeRoomTab(int roomId) {
        SwingUtilities.invokeLater(() -> {
            int index = tabbedPane.indexOfTab("Room " + roomId);
            if (index != -1) {
                tabbedPane.removeTabAt(index);
            }
            roomLogs.remove(roomId);
        });
    }

    // ★ [기능 3] 특정 방 로그창에 글쓰기 (GameRoom에서 호출)
    public void appendRoomLog(int roomId, String msg) {
        JTextArea area = roomLogs.get(roomId);
        if (area != null) {
            SwingUtilities.invokeLater(() -> {
                String timeMsg = sdf.format(new Date()) + msg + "\n";
                area.append(timeMsg);
                area.setCaretPosition(area.getDocument().getLength()); // 자동 스크롤
            });
        }
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

    // 로그 처리 및 필터링 로직 (메인 로그용)

    private void addLog(String text) {
        String timeLog = sdf.format(new Date()) + text;
        
        // 기록에 저장
        logHistory.add(timeLog);
        
        // 현재 필터에 맞으면 화면에 출력
        if (shouldShow(timeLog)) {
            mainLogArea.append(timeLog);
            // 자동 스크롤
            mainLogArea.setCaretPosition(mainLogArea.getDocument().getLength());
        }
    }

    private void refreshLogArea() {
        mainLogArea.setText(""); // 화면 지우기
        for (String log : logHistory) {
            if (shouldShow(log)) {
                mainLogArea.append(log);
            }
        }
        // 스크롤 맨 아래로
        if (mainLogArea.getDocument().getLength() > 0) {
            mainLogArea.setCaretPosition(mainLogArea.getDocument().getLength());
        }
    }

    private boolean shouldShow(String log) {
        String filter = (String) filterCombo.getSelectedItem();
        if (filter.startsWith("ALL")) return true;
        
        // 필터링 키워드 추출 (SYS, CONN, CHAT 등)
        String keyword = filter.split(" ")[0]; 
        return log.contains("[" + keyword + "]");
    }

    // 시스템 출력 (System.out -> Main Log)
    private void redirectSystemStreams() {
        OutputStream out = new OutputStream() {
            private ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            @Override
            public void write(int b) throws IOException {
                if (b == '\n') {
                    String line = buffer.toString("UTF-8"); // 한글 깨짐 방지
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