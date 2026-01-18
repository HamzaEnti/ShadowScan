package view;

import java.awt.*;
import javax.swing.*;

// Classe abstracta base per a tots els panels de l'aplicacio.
public abstract class BasePanel extends JPanel {
    
    // colors que farem servir a tota l'aplicacio
    // aixi si volem canviar-los, nomes ho fem aqui
    protected static final Color COLOR_VERD = new Color(144, 238, 144);
    protected static final Color COLOR_VERMELL = new Color(255, 69, 0);
    protected static final Color COLOR_TARONJA = new Color(255, 165, 0);
    protected static final Color COLOR_BLAU = new Color(0, 153, 204);
    protected static final Color COLOR_SALMO = new Color(255, 160, 122);
    protected static final Color COLOR_VERD_FOSC = new Color(0, 128, 0);
    
    // fonts estandard
    protected static final Font FONT_TITOL = new Font("Arial", Font.BOLD, 18);
    protected static final Font FONT_NORMAL = new Font("Segoe UI", Font.PLAIN, 12);
    protected static final Font FONT_BOTO = new Font("Segoe UI", Font.BOLD, 12);
    
    // referencia a la finestra principal per si necessitem accedir-hi
    protected MainFrame parentFrame;
    
    // Constructor base. Totes les subclasses han de cridar super(parent) al seu constructor.
    public BasePanel(MainFrame parent) {
        this.parentFrame = parent;
        initComponents();
    }
    
    // Metode abstracte que cada panel ha d'implementar. Aqui es on es creen i configuren tots els components.
    protected abstract void initComponents();
    
    // Metode auxiliar per crear botons amb un estil unificat.Aixi tots els botons de l'app es veuen iguals.
    protected JButton crearBoto(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOTO);
        btn.setFocusPainted(false);
        return btn;
    }
    
    //Sobrecarrega que permet especificar el color de fons.
    protected JButton crearBoto(String text, Color colorFons) {
        JButton btn = crearBoto(text);
        btn.setBackground(colorFons);
        return btn;
    }
    
    //Crea una etiqueta amb el titol del panel.
    protected JLabel crearTitol(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_TITOL);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        return lbl;
    }
    
    //Crea un camp de text amb mida estandard.
    protected JTextField crearCampText(int columnes) {
        JTextField txt = new JTextField(columnes);
        return txt;
    }
    
    //Mostra un dialeg per seleccionar un fitxer.Retorna null si l'usuari cancela.
    protected java.io.File seleccionarFitxer(String titol) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(titol);
        int result = fc.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            return fc.getSelectedFile();
        }
        return null;
    }
    
    //Mostra un dialeg per guardar un fitxer. Retorna null si l'usuari cancela.
    protected java.io.File guardarFitxer(String titol, String nomPerDefecte) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(titol);
        fc.setSelectedFile(new java.io.File(nomPerDefecte));
        int result = fc.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            return fc.getSelectedFile();
        }
        return null;
    }
}
