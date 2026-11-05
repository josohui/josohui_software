package tictactoe;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;

/** 외부 라이브러리 없이 수행하는 규칙 및 시간 회귀 검사. */
public final class GameModelTest {
    private static int checks;
    private static void check(boolean ok,String message) {
        checks++; if(!ok)throw new AssertionError(message);
    }
    private static void set(GameModel model,String name,Object value)throws Exception {
        Field field=GameModel.class.getDeclaredField(name);field.setAccessible(true);field.set(model,value);
    }
    private static GameModel fixture(int stage)throws Exception {
        GameModel model=new GameModel("O","X",()->0L);set(model,"stage",stage);
        set(model,"board",new int[model.getSize()][model.getSize()]);return model;
    }
    /** @param args 사용하지 않음 @throws Exception 검사 실패 또는 리플렉션 오류 */
    public static void main(String[] args)throws Exception {
        AtomicLong time=new AtomicLong();GameModel m=new GameModel(" ","",time::get);
        check(m.getPlayer(0).getName().equals("플레이어 1"),"default name");
        check(m.getSize()==3&&m.getTarget()==3&&m.getTurn()==0,"initial rules");
        check(m.play(0,0)==GameModel.MoveResult.PLACED,"place O");
        check(m.play(0,0)==GameModel.MoveResult.INVALID&&m.getTurn()==1,"occupied unchanged");
        check(m.play(-1,0)==GameModel.MoveResult.INVALID,"bounds");
        time.set(23_000_000_000L);check(m.getSecondsLeft()==7,"warning time");
        m.pause();time.addAndGet(100_000_000_000L);
        check(m.getSecondsLeft()==7&&!m.checkTimeout(),"pause freezes");
        check(m.play(1,1)==GameModel.MoveResult.INACTIVE,"pause rejects input");
        m.resume();check(m.getSecondsLeft()==7,"resume time");time.addAndGet(7_000_000_000L);
        check(m.play(1,1)==GameModel.MoveResult.TIMED_OUT&&m.getCell(1,1)==0,"deadline input");
        check(m.getTurn()==0&&m.getSecondsLeft()==30&&!m.checkTimeout(),"one pass");
        m.newGame();int[][] draw={{0,0},{0,1},{0,2},{1,1},{1,0},{1,2},{2,1},{2,0},{2,2}};
        for(int[] p:draw)m.play(p[0],p[1]);
        check(m.getSize()==6&&m.getTarget()==4&&m.getTurn()==1,"3 to 6 legal draw");
        int[][] original={{1,2,1},{1,2,2},{2,1,1}};
        for(int r=0;r<6;r++)for(int c=0;c<6;c++)
            check(m.getCell(r,c)==(r>=1&&r<=3&&c>=1&&c<=3?original[r-1][c-1]:0),"asymmetric first expansion");
        for(int stage=1;stage<=3;stage++) {
            m=fixture(stage);int n=m.getSize();int[][] board=new int[n][n];
            for(int r=0;r<n;r++)for(int c=0;c<n;c++)board[r][c]=(r/2+c)%2+1;
            int last=board[n-1][n-1];board[n-1][n-1]=0;set(m,"board",board);set(m,"turn",last-1);
            GameModel.MoveResult result=m.play(n-1,n-1);
            if(stage==3) {
                check(result==GameModel.MoveResult.DRAW&&m.isFinished(),"final draw");
                check(m.getPlayer(0).getDraws()==1&&m.getPlayer(1).getDraws()==1,"draw stats");
            } else {
                check(result==GameModel.MoveResult.EXPANDED,"expansion");
                int shift=stage==1?2:3;int wanted=stage==1?10:17;check(m.getSize()==wanted,"stage size");
                for(int r=0;r<wanted;r++)for(int c=0;c<wanted;c++) {
                    int expected=r>=shift&&r<shift+n&&c>=shift&&c<shift+n?board[r-shift][c-shift]:0;
                    check(m.getCell(r,c)==expected,"preserve marks and empty borders");
                }
                check(m.getPlayer(0).getDraws()==0,"expansion not a draw");
            }
        }
        for(int stage=0;stage<4;stage++)for(int player=0;player<2;player++)
            for(int[] direction:new int[][]{{1,0},{0,1},{1,1},{1,-1}}) {
                m=fixture(stage);set(m,"turn",player);int n=m.getSize(),goal=m.getTarget();int[][] board=new int[n][n];
                int start=direction[1]<0?goal-1:0;
                for(int k=0;k<goal-1;k++)board[k*direction[0]][start+k*direction[1]]=player+1;
                set(m,"board",board);int r=(goal-1)*direction[0],c=start+(goal-1)*direction[1];
                check(m.play(r,c)==GameModel.MoveResult.WON,"all directions and players win");
                check(m.getWinner()==player&&m.isWinningCell(r,c),"winner highlight");
                check(m.getPlayer(player).getWins()==1&&m.getPlayer(1-player).getLosses()==1,"stats");
                check(m.play(r,c)==GameModel.MoveResult.INACTIVE&&m.getPlayer(player).getWins()==1,"no duplicate result");
                m.newGame();check(m.getSize()==3&&m.getTurn()==0&&m.getPlayer(player).getWins()==1,"restart preserves stats");
            }
        m=fixture(3);int[][] board=new int[17][17];
        for(int r=0;r<17;r++)for(int c=0;c<17;c++)board[r][c]=(r/2+c)%2+1;
        for(int c=11;c<16;c++)board[16][c]=1;board[16][16]=0;set(m,"board",board);
        check(m.play(16,16)==GameModel.MoveResult.WON,"last cell win precedes draw");
        System.out.println("PASS: "+checks+" checks");
    }
}
