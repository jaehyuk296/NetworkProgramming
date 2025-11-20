# NetworkProgramming

### 최종 폴더 구조 예시
<img width="470" height="363" alt="image" src="https://github.com/user-attachments/assets/bf1565b2-92f8-4637-a489-246d7a28c385" />


### 🎯 Commit Convention

> 1. **커밋 메시지의 형식은 통일해 주세요.**
> 2. **예시: 🎨 Design: 푸터에 맞게 디자인 수정**
> 3. **깃모지를 사용해 주세요.**
>
> <li> 🎉 Start: Start New Project [:tada]
> <li> ✨ Feat: 새로운 기능을 추가 [:sparkles]
> <li> 🐛 Fix: 버그 수정 [:bug]
> <li> 🎨 Design: CSS 등 사용자 UI 디자인 변경 [:art]
> <li> ♻️ Refactor: 코드 리팩토링 [:recycle]
> <li> 🔧 Settings: Changing configuration files [:wrench]
> <li> 🗃️ Comment: 필요한 주석 추가 및 변경 [:card_file_box]
> <li> ➕ Dependency/Plugin: Add a dependency/plugin [:heavy_plus_sign]
> <li> 📝 Docs: 문서 수정 [:memo]
> <li> 🔀 Merge: Merge branches [:twisted_rightwards_arrows:]
> <li> 🚀 Deploy: Deploying stuff [:rocket]
> <li> 🚚 Rename: 파일 혹은 폴더명을 수정하거나 옮기는 작업만인 경우 [:truck]
> <li> 🔥 Remove: 파일을 삭제하는 작업만 수행한 경우 [:fire]
> <li> ⏪️ Revert: 전 버전으로 롤백 [:rewind]

### 1. 🔌 접속 화면 (Login)
목표: server.txt를 읽어 서버에 연결하고 ID를 받는다.

[필수] : 닉네임 설정

[필수] server.txt 로딩: 프로그램 시작 시 자동으로 파일에서 IP/Port 읽기. (실패 시 에러 팝업)

[필수] 접속 버튼: 누르면 소켓 연결 시도 (new Socket()).

[필수] 상태 메시지: "서버 접속 중...", "접속 실패" 등을 보여주는 작은 라벨.

[동작 흐름]: 접속 성공 시 서버가 ASSIGN_ID를 보내주면 -> 창을 닫고 로비 화면을 띄움.

### 2. 🏢 로비 화면 (Lobby)
목표: 방을 만들거나, 만들어진 방에 들어간다.

[필수] 방 목록 리스트 (JList or JTable):

보여줄 정보: [방번호] 방제목 (인원 2/4)

서버의 UPDATE_LOBBY 메시지를 받으면 리스트를 싹 지우고 다시 그리기.

[필수] 방 만들기 버튼:

누르면 작은 팝업(Dialog)으로 방 제목 입력받음 -> REQ_CREATE_ROOM 전송.

[필수] 방 입장 버튼:

리스트에서 방을 클릭하고 이 버튼을 누르면 -> REQ_JOIN_ROOM 전송.

[필수] 내 정보 표시: "안녕하세요, 플레이어 A님" (상단 라벨).

### 3. 🏠 게임 대기방 (Waiting Room) - 중요
목표: 4명이 모여서 채팅하고, 모두 '준비'하면 시작한다.

[필수] 접속자 슬롯 (4칸):

A (방장), B, C, D 처럼 텍스트로 표시.

누가 들어오고 나갔는지 ROOM_UPDATE 메시지로 즉시 갱신.

[필수] 준비(Ready) 상태 표시:

아이디 옆에 [준비완료](초록색) / [대기중](회색) 텍스트나 아이콘 표시.

[필수] Ready 버튼:

누르면 내 상태가 토글됨 (REQ_READY).

[필수] Game Start 버튼 (방장 전용):

방장이 아닌 사람에겐 비활성화(Gray out).

방장이라도 다른 사람들이 모두 Ready가 아니면 비활성화.

[필수] 채팅창: JTextArea(로그) + JTextField(입력).

[필수] 나가기 버튼: 누르면 로비로 이동.

### 4. 🎲 게임 플레이 화면 (In-Game) - 핵심
목표: 다빈치코드를 플레이한다. (조커 룰 포함)

[필수] 내 타일 패널 (하단):

내 타일은 숫자와 색깔이 다 보여야 함.

중요: 서버가 정렬해주지 않으므로, 받은 순서대로(또는 내가 꽂은 순서대로) 잘 나열돼야 함.

[필수] 상대방 타일 패널 (상단/좌우):

기본적으로 '뒷면'만 보임.

서버에서 BCAST_REVEAL 메시지가 오면 해당 타일만 숫자로 바꿔서 다시 그림.

[필수] 액션 패널 (우측):

턴 표시: "나의 턴"일 때만 버튼들이 활성화됨.

타일 가져오기: [검정], [흰색] 버튼.

[중요] 삽입 위치 입력: 조커 룰 때문에 타일을 가져올 때 "몇 번째 칸에 꽂을지(Index)" 입력하는 텍스트 필드 필수.

추측 하기: [대상ID], [위치Index], [숫자/JOKER] 입력 필드 3개 + [추측] 버튼.

[필수] 타이머: "남은 시간: 30초" (서버 시간과 싱크 안 맞아도 되니 일단 30초 카운트다운 표시).

[필수] 게임 로그: "A님이 타일을 가져왔습니다", "B님이 추측에 성공했습니다" 등 서버 메시지 출력.

