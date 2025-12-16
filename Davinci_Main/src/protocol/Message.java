package protocol;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    // --- 프로토콜 상수 (대화의 종류) ---
    public static final int REQ_LOGIN = 1000;    // 클라 -> 서버: "로그인 요청 (닉네임)"
    public static final int LOGIN_SUCCESS = 1001;// 서버 -> 클라: "로그인 성공 (로비로 이동)"
    public static final int LOGIN_FAIL = 1002;   // 서버 -> 클라: "로그인 실패"

    public static final int REQ_CREATE_ROOM = 2000; // 클라 -> 서버: "방제목"
    public static final int RES_CREATE_SUCCESS = 2001; // 서버 -> 클라: "방 만들어짐 (대기방으로 이동)"
    public static final int REQ_JOIN_ROOM = 2002;  // 서버 -> 클라: "방 목록이 갱신 (리스트 데이터)"
    public static final int SRES_ROOM_LIST = 2003;  // 서버 -> 클라: "방 목록 데이터 갱신"  

    // 대기방 전용 프로토콜
    public static final int RES_JOIN_SUCCESS = 3000; // 방 입장 성공 (대기방 화면으로 이동)
    public static final int ROOM_UPDATE = 3001;      // 방 상태 갱신 (유저 리스트, 준비 상태 등)
    public static final int CHAT_MSG = 3002;         // 대화 메시지
    public static final int REQ_READY = 3003;        // 준비 버튼 클릭
    public static final int REQ_EXIT_ROOM = 3004;    // 방 나가기
    public static final int REQ_START_GAME = 3005;   // 게임 시작 요청 (방장만 가능)
	public static final int RES_JOIN_FAIL = 3006;	// 방 입장 실패 (게임 중이거나 인원 초과 등)

    // 게임 전용 프로토콜
    public static final int GAME_START = 4000;     // 서버 -> 클라: "게임 시작 (내 타일 정보 포함)"
    public static final int TURN_START = 4001;     // 서버 -> 클라(전체): 누구의 턴입니다 (닉네임 전송)
    public static final int TURN_TIMEOUT = 4002;   // 클라 -> 서버: 300초 초과 후 다음 턴 요구
    public static final int REQ_DRAW = 4003;       // 클라 -> 서버: "타일 뽑기 요청" (데이터: "색상:인덱스")
    public static final int RES_DRAW = 4004;       // 서버 -> 클라: "타일 뽑기 결과" (데이터: 뽑은 타일 객체)
    public static final int REQ_GUESS = 4005;      // 클라 -> 서버: "추리 요청" (데이터: "타겟ID:인덱스:추측값")
    public static final int BCAST_REVEAL = 4006;   // 서버 -> 클라(전체): "타일 공개 알림" (데이터: 공개된 타일 정보)
    public static final int GAME_OVER = 4007;      // 서버 -> 클라(전체): "게임 종료" (데이터: 승자 이름)
    public static final int REQ_END_TURN = 4008;   // 클라 -> 서버: "추리 그만하고 턴 넘길래"
	public static final int BCAST_DRAW = 4009;	   // 서버 -> 클라(전체): "타일 드로우 및 삽입 알림" (데이터: [ID, Color, Index])
	public static final int REQ_JOKER_POSITION = 4010; // 클라 -> 서버: "조커 위치 설정" (데이터: "색상:인덱스")
    
    // --- 데이터 ---
	private int mode;
	    private Object data1;
	    private Object data2;
	    private Object data3; // 추가 데이터
	
	    // --- 생성자 ---
	    public Message(int mode) {
	        this(mode, null, null, null);
	    }
	
	    public Message(int mode, Object data1) {
	        this(mode, data1, null, null);
	    }
	
	    public Message(int mode, Object data1, Object data2) {
	        this(mode, data1, data2, null);
	    }
	
	    public Message(int mode, Object data1, Object data2, Object data3) {
	        this.mode = mode;
	        this.data1 = data1;
	        this.data2 = data2;
	        this.data3 = data3;
	    }
	
	    // --- Getter & Setter ---
	    public int getMode() { return mode; }
	    
	    public Object getData1() { return data1; }
	    public void setData1(Object data1) { this.data1 = data1; }
	
	    public Object getData2() { return data2; }
	    public void setData2(Object data2) { this.data2 = data2; }
	
	    public Object getData3() { return data3; }
	    public void setData3(Object data3) { this.data3 = data3; }
	
	    // [호환성 유지]
	    public Object getPayload() { return data2; }
	    public void setPayload(Object payload) { this.data2 = payload; }
	}