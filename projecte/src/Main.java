import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import view.MainFrame;

public class Main {
    public static void main(String[] args) {
        // 1. Intentem carregar l'estil visual del sistema operatiu (Windows/Linux). 
        // Així l'aplicació es veu moderna i integrada, evitant el disseny "Java" per defecte que es veu bastant antic
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println("No s'ha pogut carregar l'estil del sistema.");
        }

        // 2. Iniciem la finestra dins del fil d'esdeveniments de Swing. Hem posat un try-catch general per si falla alguna cosa crítica en crear la finestra principal 
        // (on s'ajunta la feina de tot l'equip).
        SwingUtilities.invokeLater(() -> {
            try {
                // Aquesta línia carrega tota la feina de l'Oscar, Nico i Hamza
                MainFrame finestra = new MainFrame();
                finestra.setVisible(true);

                System.out.println(">>> Aplicació iniciada. Esperant ordres...");

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}