package tictactoe;

/** 한 플레이어의 이름, 표시 및 현재 실행 중의 누적 성적을 보관한다. */
public final class Player {
    private final String name;
    private final char mark;
    private int wins, losses, draws;

    /** 플레이어를 생성한다.
     * @param name 화면에 표시할 이름
     * @param mark O 또는 X 표시
     */
    public Player(String name, char mark) {
        this.name = name;
        this.mark = mark;
    }
    /** 플레이어 이름을(를) 조회한다.
     * @return 플레이어 이름
     */
    public String getName() { return name; }
    /** 플레이어의 표시을(를) 조회한다.
     * @return 플레이어의 표시
     */
    public char getMark() { return mark; }
    /** 누적 승리 횟수을(를) 조회한다.
     * @return 누적 승리 횟수
     */
    public int getWins() { return wins; }
    /** 누적 패배 횟수을(를) 조회한다.
     * @return 누적 패배 횟수
     */
    public int getLosses() { return losses; }
    /** 누적 무승부 횟수을(를) 조회한다.
     * @return 누적 무승부 횟수
     */
    public int getDraws() { return draws; }
    void recordWin() { wins++; }
    void recordLoss() { losses++; }
    void recordDraw() { draws++; }
}
