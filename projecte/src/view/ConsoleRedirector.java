package view;

import java.io.OutputStream;
import java.io.IOException;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class ConsoleRedirector extends OutputStream {

    private JTextArea textArea;

    public ConsoleRedirector(JTextArea textArea) {
        this.textArea = textArea;
    }

    @Override
    public void write(int b) throws IOException {
        char c = (char) b;
        SwingUtilities.invokeLater(() -> {
            textArea.append(String.valueOf(c));
            textArea.setCaretPosition(textArea.getDocument().getLength());
        });
    }
}
