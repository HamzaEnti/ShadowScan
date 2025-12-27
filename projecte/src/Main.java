import javax.swing.SwingUtilities;
import view.MainFrame;

public class Main {
    public static void main(String[] args) {
        // arrenquem la finestra de manera segura
        SwingUtilities.invokeLater(() -> {
            MainFrame finestra = new MainFrame();
            finestra.setVisible(true);
        });
    }
}