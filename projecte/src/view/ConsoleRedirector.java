package view; // Paquet

import java.io.IOException;
import java.io.OutputStream;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class ConsoleRedirector extends OutputStream {
  private JTextArea textArea; 

  public ConsoleRedirector(JTextArea textArea) {
    this.textArea = textArea; 
  }

  @Override
  //mostra el text en temps real a la UI
  public void write(int b) throws IOException {
    char c = (char) b;
    SwingUtilities.invokeLater(() -> {
      textArea.append(String.valueOf(c)); 
      textArea.setCaretPosition(textArea.getDocument().getLength()); 
    });
  }
}
