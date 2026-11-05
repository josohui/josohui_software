package tictactoe;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.Font;

/** 확장 틱택토 베타 버전의 실행 진입점. */
public final class Main {
    private Main() { }
    /** Swing 이벤트 처리 스레드에서 게임 창을 연다.
     * @param args 사용하지 않는 명령행 인수
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) { /* 기본 Swing 모양으로 실행 */ }
            FontUIResource font = new FontUIResource(new Font("Malgun Gothic", Font.PLAIN, 15));
            for (Object key : UIManager.getDefaults().keySet().toArray())
                if (UIManager.get(key) instanceof FontUIResource) UIManager.put(key, font);
            new GameFrame().setVisible(true);
        });
    }
}
