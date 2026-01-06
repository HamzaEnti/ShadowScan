package controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities; // necessari per tocar la interficie
import model.ResultatHost;
import utils.NetworkUtil;
import view.MainFrame; // la vista de l'oscar

public class ScanTask implements Runnable {
  private String ip; // variable per guardar la finestra
  private MainFrame vista;
  private PortScanMode mode;

  // constructor actualitzat per nico
  public ScanTask(String ip, MainFrame v, PortScanMode mode) {
    this.ip = ip;
    this.vista = v;
    this.mode = mode;
  }
  //ports utilitzats per a l'escaneig parcial
  private static final int[] COMMON_PORTS = {
      21, 22, 23, 25, 53, 80, 110, 139, 143, 443, 445, 3306, 3389, 8080};

  @Override
  public void run() {
    if (!NetworkUtil.isReachable(ip, 200))
      return;

    ResultatHost host = new ResultatHost(ip);
    host.setEsViu(true);

    List<Integer> portsOberts = Collections.synchronizedList(new ArrayList<>());

    ExecutorService portPool = Executors.newFixedThreadPool(50); // ajustable

    if (mode == PortScanMode.PARCIAL) {
      for (int port : COMMON_PORTS) { //parcial
        portPool.execute(() -> {
          if (NetworkUtil.isPortOpen(ip, port, 50)) {
            portsOberts.add(port);
          }
        });
      }
    } else { // full
      for (int port = 1; port <= 65535; port++) {
        final int p = port;
        portPool.execute(() -> {
          if (NetworkUtil.isPortOpen(ip, p, 20)) {
            portsOberts.add(p);
          }
        });
      }
    }

    portPool.shutdown();
    try {
      portPool.awaitTermination(10, TimeUnit.MINUTES);
    } catch (InterruptedException ignored) {
    }

    host.setPortsOberts(portsOberts);

    SwingUtilities.invokeLater(() -> vista.afegirResultat(host));
  }
}