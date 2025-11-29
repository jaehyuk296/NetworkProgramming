package protocol;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    // --- 프로토콜 상수 (대화의 종류) ---
    public static final int REQ_LOGIN = 1000;    // 클라 -> 서버: "로그인 요청 (닉네임)"
    public static final int LOGIN_SUCCESS = 1001;// 서버 -> 클라: "로그인 성공 (로비로 이동)"
    public static final int LOGIN_FAIL = 1002;   // 서버 -> 클라: "로그인 실패"

    public static final int REQ_CREATE_ROOM = 2000; // 클라 -> 서버: "방 만들어줘 (방제목)"
    public static final int RES_CREATE_SUCCESS = 2001; // 서버 -> 클라: "방 만들어짐 (대기방으로 이동해)"
    public static final int REQ_JOIN_ROOM = 2002;  // 서버 -> 클라: "방 목록이 갱신 (리스트 데이터)"
    public static final int SRES_ROOM_LIST = 2003;  // 서버 -> 클라: "방 목록 데이터 갱신"  

    // 대기방 전용 프로토콜
    public static final int RES_JOIN_SUCCESS = 3000; // 방 입장 성공 (대기방 화면으로 이동)
    public static final int ROOM_UPDATE = 3001;      // 방 상태 갱신 (유저 리스트, 준비 상태 등)
    public static final int CHAT_MSG = 3002;         // 대화 메시지
    public static final int REQ_READY = 3003;        // 준비 버튼 클릭
    public static final int REQ_EXIT_ROOM = 3004;    // 방 나가기

    // --- 데이터 ---
    private int mode;       // 위 상수 중 하나
    private Object data1;   // (보낼 데이터 닉네임, 방제목, 방번호 등)
    private Object payload; // 추가 데이터 (방 목록 Vector, 게임 객체 등)
    
    public Message(int mode, Object data1) {
        this.mode = mode;
        this.data1 = data1;
    }

    public Message(int mode) {
        this(mode, null);
    }

    public Message(int mode, Object data1, Object payload) {
        this.mode = mode;
        this.data1 = data1;
        this.payload = payload;
    }

    public int getMode() { return mode; }
    public Object getData1() { return data1; }

    public void setData1(Object data1) {
        this.data1 = data1;
    }
    // 방 목록을 꺼낼 때 이 메소드가 필요합니다.
    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }
}