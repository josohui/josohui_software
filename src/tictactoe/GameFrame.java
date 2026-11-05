package tictactoe;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** 이름 입력, 게임판, 타이머, 성적과 확인 대화상자를 제공하는 Swing 창.
 * @serial exclude
 */
public final class GameFrame extends JFrame {
    private GameModel model;
    private final CardLayout pages = new CardLayout();
    private final JPanel content = new JPanel(pages);
    private final JTextField firstName = new JTextField("플레이어 1", 16);
    private final JTextField secondName = new JTextField("플레이어 2", 16);
    private final JLabel ruleLabel = new JLabel();
    private final JLabel turnLabel = new JLabel();
    private final JLabel timeLabel = new JLabel();
    private final JLabel notice = new JLabel(" ");
    private final JPanel boardPanel = new JPanel();
    private final DefaultTableModel scoreTable = new DefaultTableModel(
            new Object[]{"표시", "이름", "승", "패", "무승부"}, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private final Timer timer = new Timer(100, event -> updateClock());
    private JButton[][] squares;
    private int drawnSize;

    /** 시작 화면과 게임 화면을 만들고 종료 요청을 확인하도록 설정한다. */
    public GameFrame() {
        super("확장 틱택토 Beta 1.0");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { requestExit(); }
        });
        content.add(createWelcome(), "welcome"); content.add(createGame(), "game");
        setContentPane(content); setSize(940, 900); setMinimumSize(new Dimension(740, 720));
        setLocationRelativeTo(null);
    }
    private JPanel createWelcome() {
        JPanel outer = new JPanel(new GridBagLayout());
        JPanel form = new JPanel(); form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("확장 틱택토"); title.setFont(title.getFont().deriveFont(Font.BOLD, 32));
        form.add(title); form.add(Box.createVerticalStrut(22));
        form.add(new JLabel("같은 기기에서 두 명이 번갈아 플레이합니다."));
        form.add(Box.createVerticalStrut(18));
        form.add(new JLabel("O 플레이어 이름")); form.add(firstName);
        form.add(Box.createVerticalStrut(12)); form.add(new JLabel("X 플레이어 이름")); form.add(secondName);
        form.add(Box.createVerticalStrut(22));
        form.add(new JLabel("게임판: 3×3 → 6×6 → 10×10 → 17×17"));
        form.add(new JLabel("승리 조건: 3 → 4 → 5 → 6개 연속 / 차례당 30초"));
        form.add(Box.createVerticalStrut(22));
        JButton start = new JButton("게임 시작"); start.addActionListener(e -> startGame());
        form.add(start); outer.add(form); return outer;
    }
    private JPanel createGame() {
        JPanel panel = new JPanel(new BorderLayout(12,12)); panel.setBorder(new EmptyBorder(18,18,18,18));
        JPanel info = new JPanel(new GridLayout(3,1,0,8));
        turnLabel.setFont(turnLabel.getFont().deriveFont(Font.BOLD,20));
        info.add(ruleLabel); info.add(turnLabel); info.add(timeLabel); panel.add(info,BorderLayout.NORTH);
        JPanel holder = new JPanel(new GridBagLayout()); holder.add(boardPanel);
        JScrollPane scroll = new JScrollPane(holder);
        scroll.setBorder(BorderFactory.createTitledBorder("게임판 · 큰 판은 스크롤하여 확인하세요"));
        scroll.getVerticalScrollBar().setUnitIncrement(40); scroll.getHorizontalScrollBar().setUnitIncrement(40);
        panel.add(scroll,BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(8,10)); bottom.add(notice,BorderLayout.NORTH);
        JTable table = new JTable(scoreTable); table.setRowHeight(30); table.setFocusable(false);
        table.getColumnModel().getColumn(1).setPreferredWidth(220);
        JScrollPane scores = new JScrollPane(table); scores.setPreferredSize(new Dimension(600,92));
        bottom.add(scores,BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton restart = new JButton("새 경기"); restart.addActionListener(e -> requestNewGame());
        JButton exit = new JButton("종료"); exit.addActionListener(e -> requestExit());
        actions.add(restart); actions.add(exit); bottom.add(actions,BorderLayout.SOUTH);
        panel.add(bottom,BorderLayout.SOUTH); return panel;
    }
    private void startGame() {
        model = new GameModel(firstName.getText(), secondName.getText());
        pages.show(content,"game"); notice.setText("빈칸을 선택하세요. O가 먼저 시작합니다.");
        refresh(); timer.start();
    }
    private void makeBoard() {
        drawnSize = model.getSize(); boardPanel.removeAll();
        boardPanel.setLayout(new GridLayout(drawnSize,drawnSize,3,3));
        int side = drawnSize <= 6 ? 420 : drawnSize * 44;
        boardPanel.setPreferredSize(new Dimension(side,side)); squares = new JButton[drawnSize][drawnSize];
        for (int r=0;r<drawnSize;r++) for (int c=0;c<drawnSize;c++) {
            final int row=r,col=c;
            JButton cell = new JButton() {
                @Override protected void paintComponent(Graphics g) {
                    g.setColor(getBackground()); g.fillRect(0,0,getWidth(),getHeight()); super.paintComponent(g);
                }
            };
            cell.setContentAreaFilled(false); cell.setOpaque(true); cell.setFocusPainted(false);
            cell.setMargin(new Insets(0,0,0,0)); cell.setFont(cell.getFont().deriveFont(Font.BOLD,drawnSize>=10?22:30));
            cell.setToolTipText((r+1)+"행 "+(c+1)+"열");
            cell.addActionListener(e -> play(row,col)); squares[r][c]=cell; boardPanel.add(cell);
        }
        boardPanel.revalidate(); boardPanel.getParent().revalidate();
    }
    private void play(int row,int col) {
        GameModel.MoveResult result=model.play(row,col);
        notice.setText(switch(result) {
            case INVALID -> "이미 표시가 있는 칸입니다. 남은 시간 안에 빈칸을 선택하세요.";
            case TIMED_OUT -> "시간 초과! 표시를 놓지 않고 차례가 넘어갔습니다.";
            case EXPANDED -> "게임판 확장! 기존 표시는 유지되며 승리 조건이 한 개 증가했습니다.";
            case WON -> model.getPlayer(model.getWinner()).getName()+" WIN / "+model.getPlayer(1-model.getWinner()).getName()+" LOSE";
            case DRAW -> "최종 무승부 · 두 플레이어 모두 DRAW";
            case PLACED -> "상대 플레이어의 차례입니다.";
            case INACTIVE -> notice.getText();
        });
        refresh();
    }
    private void updateClock() {
        if (model == null) return;
        if (model.checkTimeout()) { notice.setText("시간 초과! 상대 플레이어의 차례입니다."); refresh(); }
        else showTime();
    }
    private void showTime() {
        int seconds=model.getSecondsLeft();
        timeLabel.setText(model.isFinished()?"경기 종료":model.isPaused()?"확인 중 · 타이머 정지":"남은 시간: "+seconds+"초"+(seconds<=7?"  · 서둘러 주세요!":""));
        timeLabel.setForeground(!model.isFinished()&&seconds<=7?new Color(180,35,35):Color.DARK_GRAY);
    }
    private void refresh() {
        if (model.getSize()!=drawnSize) makeBoard();
        ruleLabel.setText(model.getStage()+"단계  |  "+model.getSize()+"×"+model.getSize()+"  |  승리 조건: "+model.getTarget()+"개 이상 연속");
        Player current=model.getPlayer(model.getTurn());
        turnLabel.setText(model.isFinished()?(model.getWinner()<0?"DRAW · 최종 무승부":model.getPlayer(model.getWinner()).getName()+" 승리!"):
                "현재 차례: "+current.getName()+" ("+current.getMark()+")");
        for(int r=0;r<drawnSize;r++)for(int c=0;c<drawnSize;c++) {
            JButton cell=squares[r][c]; int value=model.getCell(r,c);
            cell.setText(value==0?"":value==1?"O":"X");
            cell.setForeground(value==2?new Color(35,85,160):Color.BLACK);
            cell.setBackground(model.isWinningCell(r,c)?new Color(255,217,130):new Color(247,248,250));
            cell.setEnabled(!model.isFinished()&&!model.isPaused());
        }
        scoreTable.setRowCount(0);
        for(int p=0;p<2;p++) {Player player=model.getPlayer(p);scoreTable.addRow(new Object[]{player.getMark(),player.getName(),player.getWins(),player.getLosses(),player.getDraws()});}
        showTime(); if(model.isFinished())timer.stop(); boardPanel.repaint();
    }
    /** 같은 이름과 누적 성적으로 새 경기를 요청한다. 진행 중이면 확인 후 초기화한다. */
    public void requestNewGame() {
        if(model==null)return;
        if(!model.isFinished()) {
            model.pause();timer.stop();refresh();
            int answer=JOptionPane.showConfirmDialog(this,"현재 경기는 기록하지 않고 새로 시작할까요?","새 경기",JOptionPane.YES_NO_OPTION);
            if(answer!=JOptionPane.YES_OPTION){model.resume();refresh();timer.start();return;}
        }
        model.newGame();notice.setText("새 경기입니다. 이름과 누적 기록은 유지됩니다.");refresh();timer.start();
    }
    /** 종료 확인 후 창을 닫는다. 취소하면 기존 경기와 남은 시간을 유지한다. */
    public void requestExit() {
        if(model!=null){model.pause();timer.stop();refresh();}
        int answer=JOptionPane.showConfirmDialog(this,"종료하면 진행 중인 경기와 누적 기록이 사라집니다. 종료할까요?","종료",JOptionPane.YES_NO_OPTION);
        if(answer==JOptionPane.YES_OPTION){timer.stop();dispose();}
        else if(model!=null){model.resume();refresh();if(!model.isFinished())timer.start();}
    }
}
