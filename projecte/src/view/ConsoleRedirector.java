package view;

import java.io.IOException;
import java.io.OutputStream;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

// Classe que redirigeix la sortida de System.out cap a un JTextArea.

public class ConsoleRedirector extends OutputStream {
    
    // el JTextArea on volem que surtin els missatges
    private JTextArea textArea;

    public ConsoleRedirector(JTextArea textArea) {
        this.textArea = textArea;
    }

    // Metode que es crida cada vegada que s'escriu un byte.
    @Override
    public void write(int b) throws IOException {
        // convertim el byte a caracter
        char c = (char) b;
        
        // actualitzem la UI en el thread correcte
        SwingUtilities.invokeLater(() -> {
            textArea.append(String.valueOf(c));
            // movem el cursor al final per veure sempre el mes recent
            textArea.setCaretPosition(textArea.getDocument().getLength());
        });
    }
    
    //Versio optimitzada per escriure mes d'un byte alhora. Aixi va mes rapid que cridar write(int) per cada byte.
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        String text = new String(b, off, len);
        SwingUtilities.invokeLater(() -> {
            textArea.append(text);
            textArea.setCaretPosition(textArea.getDocument().getLength());
        });
    }
}
