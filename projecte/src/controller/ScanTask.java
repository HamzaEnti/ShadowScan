package controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import model.ResultatHost;
import utils.NetworkUtil;
import view.MainFrame;

public class ScanTask implements Runnable {
  private String ip;
  private MainFrame vista;
  private PortScanMode mode;

  public ScanTask(String ip, MainFrame v, PortScanMode mode) {
    this.ip = ip;
    this.vista = v;
    this.mode = mode;
  }

  private static final int[] COMMON_PORTS = {
      21, 22, 23, 25, 53, 80, 110, 139, 143, 443, 445, 3306, 3389, 8080};

  @Override
  public void run() {
    // primer mirem si el host esta viu
    if (!NetworkUtil.isReachable(ip, 200))
      return;

    ResultatHost host = new ResultatHost(ip);
    host.setEsViu(true);

    // llista sincronitzada per no petar amb els threads
    List<Integer> portsOberts = Collections.synchronizedList(new ArrayList<>());

    //Aquesta parts dels threads esta feta amb IA, tot i que la idea la vam treure del oriol
    // ara si que calculem be els threads
    int numCores = Runtime.getRuntime().availableProcessors();
    int threads = Math.max(numCores * 2, 8); // minim 8 threads per anar fluid
    
    System.out.println("Escanejant " + ip + " amb " + threads + " threads"); // per veure que va
    
    ExecutorService portPool = Executors.newFixedThreadPool(threads);

    if (mode == PortScanMode.PARCIAL) {
      // mode parcial, nomes ports comuns
      for (int port : COMMON_PORTS) {
        final int p = port; // important pel lambda
        portPool.execute(() -> {
          if (NetworkUtil.isPortOpen(ip, p, 50)) {
            portsOberts.add(p);
            System.out.println("Port obert trobat: " + p); // debug
          }
        });
      }
    } else {
      // mode full, tots els ports
      for (int port = 1; port <= 65535; port++) {
        final int p = port;
        portPool.execute(() -> {
          if (NetworkUtil.isPortOpen(ip, p, 20)) {
            portsOberts.add(p);
            System.out.println("Port obert trobat: " + p); // debug
          }
        });
      }
    }

    // tanquem el pool i esperem que acabi tot
    portPool.shutdown();
    try {
      // esperem maxim 10 minuts per si es un scan full llarg
      boolean finished = portPool.awaitTermination(10, TimeUnit.MINUTES);
      if (!finished) {
        System.out.println("WARNING: Timeout en el scan de " + ip);
        portPool.shutdownNow(); // forcem tancar si triga massa
      }
    } catch (InterruptedException e) {
      System.out.println("Scan interromput per " + ip);
      portPool.shutdownNow();
      Thread.currentThread().interrupt();
      return;
    }

    // ordenem els ports abans de mostrar
    Collections.sort(portsOberts);
    
    System.out.println("Scan completat per " + ip + ". Ports trobats: " + portsOberts.size());
    
    host.setPortsOberts(portsOberts);

    // actualitzem la interficie en el thread correcte
    SwingUtilities.invokeLater(() -> vista.afegirResultat(host));
  }
}