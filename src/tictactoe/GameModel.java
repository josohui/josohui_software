package tictactoe;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

/** 확장 틱택토의 규칙과 상태를 관리한다. GUI와 분리되어 있으며 단일 스레드에서 사용한다.
 * 게임판은 3, 6, 10, 17 크기로 확장되며 승리 조건은 3, 4, 5, 6개 연속이다.
 */
public final class GameModel {
    private static final int[] SIZES = {3, 6, 10, 17};
    private static final int[] TARGETS = {3, 4, 5, 6};
    private static final long TURN_TIME = 30_000_000_000L;
    /** 한 번의 수 요청을 처리한 결과. */
    public enum MoveResult {
        /** 정상 배치 후 차례 변경. */ PLACED,
        /** 기존 표시를 보존하며 게임판 확장. */ EXPANDED,
        /** 승리로 경기 종료. */ WON,
        /** 마지막 단계에서 무승부. */ DRAW,
        /** 범위 밖 또는 이미 사용 중인 칸. */ INVALID,
        /** 제한시간이 지나 수 없이 차례 변경. */ TIMED_OUT,
        /** 종료 또는 일시 정지 상태이므로 입력 무시. */ INACTIVE
    }
    private final LongSupplier clock;
    private final Player[] players;
    private int[][] board;
    private int stage, turn, winner;
    private boolean finished, paused;
    private long deadline, savedTime;
    private final List<int[]> winningLine = new ArrayList<>();

    /** 이름을 설정하고 첫 경기를 시작한다. 빈 이름에는 기본 이름을 사용한다.
     * @param first O 플레이어 이름
     * @param second X 플레이어 이름
     */
    public GameModel(String first, String second) { this(first, second, System::nanoTime); }
    GameModel(String first, String second, LongSupplier clock) {
        this.clock = clock;
        players = new Player[]{new Player(normalize(first, "플레이어 1"), 'O'),
                new Player(normalize(second, "플레이어 2"), 'X')};
        newGame();
    }
    private static String normalize(String text, String fallback) {
        return text == null || text.isBlank() ? fallback : text.strip();
    }
    /** 이름과 성적을 유지하면서 빈 3×3 판, O 차례, 30초로 새 경기를 시작한다. */
    public void newGame() {
        stage = 0; turn = 0; winner = -1; finished = false; paused = false;
        board = new int[3][3]; winningLine.clear(); resetClock();
    }
    /** 현재 차례에 수를 둔다. 시간 만료를 먼저 검사하고, 승리를 판 확장보다 먼저 판정한다.
     * @param row 0부터 시작하는 행
     * @param col 0부터 시작하는 열
     * @return 배치, 확장, 승리, 무승부, 입력 오류 또는 시간 초과 결과
     */
    public MoveResult play(int row, int col) {
        if (finished || paused) return MoveResult.INACTIVE;
        if (checkTimeout()) return MoveResult.TIMED_OUT;
        if (row < 0 || col < 0 || row >= getSize() || col >= getSize() || board[row][col] != 0)
            return MoveResult.INVALID;
        board[row][col] = turn + 1;
        if (findWinningLine(row, col)) { endGame(turn); return MoveResult.WON; }
        boolean full = true;
        for (int[] line : board) for (int cell : line) if (cell == 0) full = false;
        if (full && stage == SIZES.length - 1) { endGame(-1); return MoveResult.DRAW; }
        if (full) expand();
        turn = 1 - turn; resetClock();
        return full ? MoveResult.EXPANDED : MoveResult.PLACED;
    }
    private boolean findWinningLine(int row, int col) {
        int mark = board[row][col];
        for (int[] direction : new int[][]{{1,0}, {0,1}, {1,1}, {1,-1}}) {
            List<int[]> line = new ArrayList<>(); line.add(new int[]{row, col});
            for (int sign : new int[]{-1, 1}) {
                int r = row + direction[0] * sign, c = col + direction[1] * sign;
                while (r >= 0 && c >= 0 && r < getSize() && c < getSize() && board[r][c] == mark) {
                    line.add(new int[]{r,c}); r += direction[0] * sign; c += direction[1] * sign;
                }
            }
            if (line.size() >= getTarget()) winningLine.addAll(line);
        }
        return !winningLine.isEmpty();
    }
    private void expand() {
        int oldSize = getSize(); stage++;
        int shift = (getSize() - oldSize) / 2;
        int[][] bigger = new int[getSize()][getSize()];
        for (int r = 0; r < oldSize; r++)
            System.arraycopy(board[r], 0, bigger[r + shift], shift, oldSize);
        board = bigger;
    }
    private void endGame(int winner) {
        finished = true; this.winner = winner;
        if (winner == -1) { players[0].recordDraw(); players[1].recordDraw(); }
        else { players[winner].recordWin(); players[1-winner].recordLoss(); }
    }
    private void resetClock() { deadline = clock.getAsLong() + TURN_TIME; }
    /** 30초가 지났으면 표시를 놓지 않고 상대 차례로 변경한다.
     * @return 이번 호출에서 차례가 변경되었으면 true
     */
    public boolean checkTimeout() {
        if (finished || paused || clock.getAsLong() < deadline) return false;
        turn = 1 - turn; resetClock(); return true;
    }
    /** 확인 대화상자가 표시되는 동안 남은 시간을 보관하고 입력을 정지한다. */
    public void pause() {
        if (finished || paused) return;
        checkTimeout(); savedTime = Math.max(0, deadline - clock.getAsLong()); paused = true;
    }
    /** 일시 정지 직전의 남은 시간부터 경기를 재개한다. */
    public void resume() {
        if (!paused || finished) return;
        deadline = clock.getAsLong() + savedTime; paused = false;
    }
    /** 남은 시간을 올림한 초, 종료 후에는 0을(를) 조회한다.
     * @return 남은 시간을 올림한 초, 종료 후에는 0
     */
    public int getSecondsLeft() {
        if (finished) return 0;
        long left = paused ? savedTime : Math.max(0, deadline - clock.getAsLong());
        return (int)((left + 999_999_999L) / 1_000_000_000L);
    }
    /** 현재 게임판의 한 변 크기을(를) 조회한다.
     * @return 현재 게임판의 한 변 크기
     */
    public int getSize() { return SIZES[stage]; }
    /** 승리에 필요한 연속 표시 개수을(를) 조회한다.
     * @return 승리에 필요한 연속 표시 개수
     */
    public int getTarget() { return TARGETS[stage]; }
    /** 1부터 시작하는 단계을(를) 조회한다.
     * @return 1부터 시작하는 단계
     */
    public int getStage() { return stage + 1; }
    /** 현재 차례의 플레이어 번호, O는 0이고 X는 1을(를) 조회한다.
     * @return 현재 차례의 플레이어 번호, O는 0이고 X는 1
     */
    public int getTurn() { return turn; }
    /** 승자 번호, 무승부 또는 진행 중이면 -1을(를) 조회한다.
     * @return 승자 번호, 무승부 또는 진행 중이면 -1
     */
    public int getWinner() { return winner; }
    /** 경기가 종료되었으면 true을(를) 조회한다.
     * @return 경기가 종료되었으면 true
     */
    public boolean isFinished() { return finished; }
    /** 확인 대기 중이면 true을(를) 조회한다.
     * @return 확인 대기 중이면 true
     */
    public boolean isPaused() { return paused; }
    /** 칸의 표시를 읽는다.
     * @param row 유효한 행 번호
     * @param col 유효한 열 번호
     * @return 빈칸 0, O 1, X 2
     */
    public int getCell(int row, int col) { return board[row][col]; }
    /** 플레이어를 조회한다.
     * @param index O는 0, X는 1
     * @return 해당 플레이어
     */
    public Player getPlayer(int index) { return players[index]; }
    /** 승리한 줄에 포함된 칸인지 확인한다.
     * @param row 행 번호
     * @param col 열 번호
     * @return 승리 줄의 칸이면 true
     */
    public boolean isWinningCell(int row, int col) {
        return winningLine.stream().anyMatch(p -> p[0] == row && p[1] == col);
    }
}
