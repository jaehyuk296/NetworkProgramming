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
    public static final int NOTE_ROOM_LIST = 2002;  // 서버 -> 클라: "방 목록이 갱신 (리스트 데이터)"

    // --- 데이터 ---
    private int mode;       // 위 상수 중 하나
    private Object data1;   // 보낼 데이터

    public Message(int mode, Object data1) {
        this.mode = mode;
        this.data1 = data1;
    }

    public Message(int mode) {
        this(mode, null);
    }

    public int getMode() { return mode; }
    public Object getData1() { return data1; }
}