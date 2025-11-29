package protocol;

import java.io.Serializable;

public class Tile implements Serializable, Comparable<Tile> {
    
    // 직렬화 버전 ID (통신 시 클래스 버전 불일치 에러 방지)
    private static final long serialVersionUID = 1L;

    private String color; // "BLACK" 또는 "WHITE"
    private int number;   // 0 ~ 11, 조커는 -1
    private boolean isRevealed; // true면 공개됨(누워있음), false면 비공개(서있음)

    /**
     * 타일 생성자
     * @param color 색상 ("BLACK", "WHITE")
     * @param number 숫자 (0~11, 조커는 -1)
     */
    public Tile(String color, int number) {
        this.color = color;
        this.number = number;
        this.isRevealed = false; // 기본값은 비공개
    }

    // --- Getter & Setter ---

    public String getColor() {
        return color;
    }

    public int getNumber() {
        return number;
    }

    public boolean isRevealed() {
        return isRevealed;
    }

    public void setRevealed(boolean isRevealed) {
        this.isRevealed = isRevealed;
    }

    // --- 유틸리티 메서드 ---

    /**
     * 이 타일이 조커인지 확인
     */
    public boolean isJoker() {
        return this.number == -1;
    }

    /**
     * 다빈치코드 정렬 규칙: 
     * 1. 숫자 오름차순 (0 -> 11)
     * 2. 숫자가 같으면 검은색이 왼쪽 (Black < White)
     * (조커는 정렬에서 제외하거나 맨 뒤로 보냄 - 로직에 따라 다름)
     */
    @Override
    public int compareTo(Tile other) {
        if (this.number == other.number) {
            // 숫자가 같으면 색상 비교 (BLACK이 WHITE보다 작음/왼쪽)
            if (this.color.equals("BLACK") && other.color.equals("WHITE")) {
                return -1;
            } else if (this.color.equals("WHITE") && other.color.equals("BLACK")) {
                return 1;
            } else {
                return 0;
            }
        }
        return Integer.compare(this.number, other.number);
    }

    /**
     * 디버깅용 출력 (예: [B 7] or [W -])
     */
    @Override
    public String toString() {
        String numStr = isJoker() ? "J" : String.valueOf(number);
        String colStr = color.equals("BLACK") ? "B" : "W";
        String state = isRevealed ? "(Open)" : "(Hide)";
        return "[" + colStr + " " + numStr + "]" + state;
    }
}