package view;

import controller.PortScanMode;
import controller.ScanController;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.ResultatHost;
import utils.JsonExporter;

// Panel per a l'escaneig de xarxa (Network Discovery).

public class DiscoveryPanel extends BasePanel {
    
    // components del formulari
    private JTextField txtIpInici;
    private JTextField txtIpFi;
    private JRadioButton rbPortsComuns;
    private JRadioButton rbTotsPorts;
    
    // botons d'accio
    private JButton btnStart;
    private JButton btnStop;
    private JButton btnExport;
    
    // taula de resultats
    private JTable taula;
    private DefaultTableModel modelTaula;
    
    // controlador i dades
    private ScanController controller;
    private List<ResultatHost> resultats;
    
    public DiscoveryPanel(MainFrame parent) {
        super(parent);
    }
    
    @Override
    protected void initComponents() {
        // inicialitzem dades
        this.resultats = new ArrayList<>();
        this.controller = new ScanController(parentFrame);
        
        // layout principal
        this.setLayout(new BorderLayout());
        
        // creem els diferents panels
        JPanel pnlNord = crearPanelNord();
        JPanel pnlCentre = crearPanelTaula();
        
        this.add(pnlNord, BorderLayout.NORTH);
        this.add(pnlCentre, BorderLayout.CENTER);
        
        // configurem els listeners
        configurarListeners();
    }
    
    //He fet aquest metode amb l'ajuda d'IA.
    //Crea el panel superior amb la configuracio de xarxa i botons.
    private JPanel crearPanelNord() {
        JPanel pnlNord = new JPanel(new BorderLayout());
        
        // panel de configuracio d'IP
        JPanel pnlConfig = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlConfig.setBorder(BorderFactory.createTitledBorder("Configuracio de Xarxa"));
        
        pnlConfig.add(new JLabel("IP Inici:"));
        txtIpInici = crearCampText(10);
        pnlConfig.add(txtIpInici);
        
        pnlConfig.add(new JLabel("IP Fi:"));
        txtIpFi = crearCampText(10);
        pnlConfig.add(txtIpFi);
        
        // botons
        btnStart = crearBoto("Escanejar", COLOR_VERD);
        btnStop = crearBoto("Aturar", COLOR_SALMO);
        btnExport = crearBoto("Guardar JSON");
        
        pnlConfig.add(btnStart);
        pnlConfig.add(btnStop);
        pnlConfig.add(btnExport);
        
        // panel de mode d'escaneig
        JPanel pnlMode = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlMode.setBorder(BorderFactory.createTitledBorder("Mode d'escaneig"));
        
        rbPortsComuns = new JRadioButton("Ports comuns (rapid)", true);
        rbTotsPorts = new JRadioButton("Tots els ports (lent)");
        
        // agrupem els radio buttons perque nomes un pugui estar seleccionat
        ButtonGroup grup = new ButtonGroup();
        grup.add(rbPortsComuns);
        grup.add(rbTotsPorts);
        
        pnlMode.add(rbPortsComuns);
        pnlMode.add(rbTotsPorts);
        
        // juntem tot
        pnlNord.add(pnlConfig, BorderLayout.NORTH);
        pnlNord.add(pnlMode, BorderLayout.SOUTH);
        
        return pnlNord;
    }
    
    // Crea el panel central amb la taula de resultats.
    private JPanel crearPanelTaula() {
        JPanel pnl = new JPanel(new BorderLayout());
        
        // definim les columnes de la taula
        String[] columnes = {"IP Adreca", "Estat", "Ports Detectats"};
        modelTaula = new DefaultTableModel(columnes, 0);
        taula = new JTable(modelTaula);
        
        // posem la taula dins un scroll per si hi ha molts resultats
        JScrollPane scroll = new JScrollPane(taula);
        pnl.add(scroll, BorderLayout.CENTER);
        
        return pnl;
    }
    
    // Configura tots els listeners dels botons. Ho tinc separat per no barrejar creacio de UI amb logica.
    private void configurarListeners() {
        // boto escanejar
        btnStart.addActionListener(e -> iniciarEscaneig());
        
        // boto aturar
        btnStop.addActionListener(e -> controller.aturar());
        
        // boto exportar
        btnExport.addActionListener(e -> exportarResultats());
    }
    
    // Inicia l'escaneig de xarxa.
    private void iniciarEscaneig() {
        String ip = txtIpInici.getText().trim();
        
        // validacio basica
        if (!ip.contains(".")) {
            System.out.println(">>> [ERROR] Format IP incorrecte");
            return;
        }
        
        // extraiem el prefix de xarxa (ex: "192.168.1." de "192.168.1.100")
        String xarxa = ip.substring(0, ip.lastIndexOf(".") + 1);
        
        // determinem el mode
        PortScanMode mode = rbPortsComuns.isSelected() ? 
                          PortScanMode.PARCIAL : PortScanMode.FULL;
        
        System.out.println(">>> [SCAN] Iniciant escombrat a " + xarxa + "0/24...");
        
        // netegem resultats anteriors
        resultats.clear();
        modelTaula.setRowCount(0);
        
        // cridem al controlador
        controller.escanearRang(xarxa, mode);
    }
    
    //Exporta els resultats a un fitxer JSON.
    private void exportarResultats() {
        File fitxer = guardarFitxer("Guardar resultats", "resultats.json");
        if (fitxer != null) {
            if (JsonExporter.saveToJSON(resultats, fitxer.getAbsolutePath())) {
                JOptionPane.showMessageDialog(this, "Resultats guardats correctament!");
            } else {
                JOptionPane.showMessageDialog(this, "Error guardant els resultats", 
                                             "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    //Afegeix un resultat a la taula.
    public synchronized void afegirResultat(ResultatHost host) {
        resultats.add(host);
        modelTaula.addRow(new Object[] {
            host.getIp(),
            "[" + host.getEstat() + "]",
            host.getPortsOberts().toString()
        });
    }
    
    //Permet establir la IP inicial des de fora (per autodeteccio).
    public void setIpInici(String ip) {
        txtIpInici.setText(ip);
    }
    
    //Permet establir la IP final des de fora.
    public void setIpFi(String ip) {
        txtIpFi.setText(ip);
    }
    
    //Retorna el controlador per si MainFrame el necessita.
    public ScanController getController() {
        return controller;
    }
    
    //Retorna la llista de resultats.
    public List<ResultatHost> getResultats() {
        return resultats;
    }
}
