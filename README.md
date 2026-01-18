# SHADOWSCAN

[![Java Version](https://img.shields.io/badge/java-11%2B-ED8B00?style=for-the-badge&logo=openjdk)](https://www.oracle.com/es/java/technologies/javase/jdk11-archive-downloads.html)
![License](https://img.shields.io/badge/license-MIT-green?style=for-the-badge)
![Status](https://img.shields.io/badge/status-active-success?style=for-the-badge)
![Architecture](https://img.shields.io/badge/architecture-MVC-blue?style=for-the-badge)

Plataforma de seguretat informatica amb interficie grafica que unifica multiples eines de pentesting en una sola aplicacio. Desenvolupada per a professionals de ciberseguretat i administradors de sistemes que necessiten realitzar auditories de xarxa de manera eficient i visual.

> **AVIS LEGAL**: Aquesta eina esta dissenyada exclusivament per a auditories autoritzades. L'us contra sistemes sense permis explicit es il·legal i pot comportar consequencies penals.

---

## Taula de Continguts

1. [Descripcio General](#descripcio-general)
2. [Funcionalitats](#funcionalitats)
3. [Requisits del Sistema](#requisits-del-sistema)
4. [Arquitectura](#arquitectura)
    - [Patro MVC](#patro-mvc)
    - [Estructura del Projecte](#estructura-del-projecte)
5. [Detalls Tecnics](#detalls-tecnics)
    - [Model de Concurrencia](#model-de-concurrencia)
6. [Instalacio](#instalacio)
7. [Configuracio](#configuracio)
8. [Us d'Intel·ligencia Artificial](#us-dintelligencia-artificial)
9. [Roadmap](#roadmap)
10. [Autors](#autors)

---

## Descripcio General

SHADOWSCAN es una solucio d'escriptori desenvolupada en Java que proporciona una interficie unificada per a l'execucio i gestio de diverses eines d'auditoria de seguretat. La plataforma centralitza operacions de reconeixement de xarxa, deteccio de serveis, proves de forca bruta i enumeracio web en un entorn visual integrat.

L'aplicacio aborda els seguents reptes operacionals:

* **Fragmentacio d'eines**: Elimina la necessitat d'alternar entre multiples terminals i aplicacions.
* **Corba d'aprenentatge**: Ofereix una interficie intuïtiva que abstrau la complexitat de les comandes CLI.
* **Gestio de resultats**: Centralitza la captura, visualitzacio i exportacio de dades d'auditoria.
* **Eficiencia operativa**: Permet executar escaneigs paralels amb monitoratge en temps real.

A diferencia d'eines de linia de comandes tradicionals, SHADOWSCAN ofereix visualitzacio de logs en temps real, exportacio automatitzada de resultats i navegacio fluida entre moduls funcionals.

---

## Funcionalitats

### Moduls Principals

| Modul | Eina Integrada | Descripcio | Estat |
|-------|----------------|------------|-------|
| Network Discovery | Java Sockets | Escaneig de rangs de xarxa amb deteccio de hosts actius i ports oberts | Operatiu |
| Service Detection | Nmap | Analisi de serveis amb identificacio de versions i fingerprinting | Operatiu |
| Brute Force | Hydra | Atacs de diccionari contra protocols SSH, FTP, MySQL, RDP i altres | Operatiu |
| Web Enumeration | Dirb | Descobriment de directoris i recursos ocults en servidors web | Operatiu |
| Web Fuzzing | Ffuf | Fuzzing d'alta velocitat per identificar endpoints no documentats | Operatiu |

### Caracteristiques Addicionals

* **Consola de Logs Integrada**: Monitoratge en temps real de la sortida de totes les eines amb format visual unificat.
* **Exportacio de Resultats**: Generacio d'informes en format JSON i CSV per a documentacio i analisi posterior.
* **Deteccio Automatica de Xarxa**: Identificacio automatica de la IP local i configuracio intel·ligent de parametres.
* **Verificacio de Dependencies**: Comprovacio automatica de la disponibilitat d'eines externes a l'inici.
* **Modes d'Escaneig**: Seleccio entre escaneig rapid (ports comuns) i escaneig exhaustiu (65535 ports).

---

## Requisits del Sistema

### Requisits de Maquinari

| Component | Minim | Recomanat |
|-----------|-------|-----------|
| Processador | Dual-core 2.0 GHz | Quad-core 2.5 GHz |
| Memoria RAM | 2 GB | 4 GB |
| Espai en disc | 100 MB | 500 MB |
| Connexio de xarxa | Ethernet/WiFi | Ethernet Gigabit |

### Requisits de Programari

| Component | Versio | Obligatori |
|-----------|--------|------------|
| Java JDK | 11 o superior | Si |
| Sistema Operatiu | Linux / Windows / macOS | Si |
| Nmap | 7.0 o superior | Si |
| Hydra | 9.0 o superior | Si |
| Dirb | 2.22 o superior | Si |
| Ffuf | 1.5 o superior | Si |

### Instalacio de Dependencies
Tot i que es recomana l'ús de Kali Linux perquè ja inclou totes les eines integrades, pots instal·lar les dependències en altres distribucions basades en Debian/Ubuntu seguint aquests passos:

```bash
sudo apt update
sudo apt install -y openjdk-11-jdk nmap hydra dirb
```

Per a Ffuf (requereix Go):

```bash
go install github.com/ffuf/ffuf/v2@latest
```

---

## Arquitectura

SHADOWSCAN implementa una arquitectura Model-Vista-Controlador (MVC) amb capes de servei dedicades per a la integracio d'eines externes.

### Patro MVC

```text
+-------------------------------------------------------------------+
|                            VISTA                                  |
|  +-------------+ +---------------+ +-----------+ +--------------+ |
|  |   MainFrame  | | DiscoveryPanel| | NmapPanel | |SecurityPanel| |
|  | (Coordinator)| | (Network Scan)| | (Services)| |  (Pentest)  | |
|  +-------------+ +---------------+ +-----------+ +--------------+ |
+--------------------------------+----------------------------------+
                                 | Events / Updates
+--------------------------------v-----------------------------------+
|                          CONTROLADOR                               |
|  +------------------+ +----------------+ +---------------------+   |
|  |  ScanController  | |   ScanTask     | |   PortScanMode      |   |
|  |  (Orchestrator)  | |  (Runnable)    | |     (Enum)          |   |
|  +------------------+ +----------------+ +---------------------+   |
+--------------------------------+-----------------------------------+
                                 | Data / Results
+--------------------------------v----------------------------------+
|                            MODEL                                  |
|  +------------------+ +----------------+ +---------------------+  |
|  | AbstractResultat | |  ResultatHost  | |   EstatResultat    |   |
|  |    (Abstract)    | |   (Concrete)   | |      (Enum)        |   |
|  +------------------+ +----------------+ +---------------------+  |
|  +------------------------------------------------------------+   |
|  |                    CAPA DE SERVEIS                         |   |
|  | AbstractScanService -> Nmap | Hydra | Dirb | Ffuf Wrappers |   |
|  +------------------------------------------------------------+   |
+-------------------------------------------------------------------+
```

### Estructura del Projecte

```text
shadowscan/
|
+-- src/
|   +-- Main.java                        # Punt d'entrada de l'aplicacio
|   |
|   +-- controller/                      # Capa de control
|   |   +-- PortScanMode.java            # Enum amb configuracio de modes
|   |   +-- ScanController.java          # Orquestrador d'escaneigs
|   |   +-- ScanTask.java                # Unitat de treball concurrent
|   |
|   +-- model/                           # Capa de dades
|   |   +-- Scannable.java               # Contracte per serveis
|   |   +-- AbstractResultat.java        # Base abstracta per resultats
|   |   +-- EstatResultat.java           # Enumeracio d'estats
|   |   +-- ResultatHost.java            # Entitat de resultat
|   |
|   +-- services/                        # Capa d'integracio
|   |   +-- AbstractScanService.java     # Base abstracta per serveis
|   |   +-- NmapWrapper.java             # Adaptador per Nmap
|   |   +-- BruteForceService.java       # Adaptador per Hydra
|   |   +-- WebDiscoveryService.java     # Adaptador per Dirb
|   |   +-- FuzzingService.java          # Adaptador per Ffuf
|   |
|   +-- utils/                           # Utilitats transversals
|   |   +-- NetworkUtil.java             # Funcions de xarxa
|   |   +-- FileUtils.java               # Operacions amb fitxers
|   |   +-- JsonExporter.java            # Serialitzacio JSON
|   |
|   +-- view/                            # Capa de presentacio
|   |   +-- BasePanel.java               # Base abstracta per panels
|   |   +-- MainFrame.java               # Finestra principal
|   |   +-- DiscoveryPanel.java          # Modul de descobriment
|   |   +-- NmapPanel.java               # Modul Nmap
|   |   +-- SecurityPanel.java           # Modul d'eines de seguretat
|   |   +-- ConsoleRedirector.java       # Redirector de sortida
|   |
|   +-- test/                            # Proves
|       +-- ConsoleDebug.java            # Utilitats de depuracio
|
+-- resources/                           # Recursos externs
|   +-- wordlists/                       # Diccionaris per atacs
|
+-- diagrams/                            # Documentacio tecnica
    +-- diagrama_classes.mermaid
    +-- diagrama_estats.mermaid
    +-- diagrama_flux.mermaid
    +-- diagrama_classes.png
    +-- diagrama_estats.png
    +-- diagrama_flux.png
```

---

## Detalls Tecnics

### Model de Concurrencia

SHADOWSCAN utilitza un model de concurrencia basat en ExecutorService per maximitzar l'eficiencia de l'escaneig.

#### Pool de Threads per Escaneig

```java
public class ScanController {
    private static final int NUM_THREADS = 20;
    private ExecutorService pool;
    
    public void escanearRang(String xarxa, PortScanMode mode) {
        pool = Executors.newFixedThreadPool(NUM_THREADS);
        
        for (int i = 1; i <= 254; i++) {
            if (!enExecucio) break;
            String ip = xarxa + i;
            pool.execute(new ScanTask(ip, vista, mode));
        }
        
        pool.shutdown();
    }
    
    public void aturar() {
        enExecucio = false;
        if (pool != null) {
            pool.shutdownNow();
        }
    }
}
```

#### Sincronitzacio amb la Interficie Grafica

```java
// Actualitzacio segura de la UI des de threads secundaris
SwingUtilities.invokeLater(() -> {
    vista.afegirResultat(host);
});

// Metode sincronitzat per evitar race conditions
public synchronized void afegirResultat(ResultatHost host) {
    resultats.add(host);
    modelTaula.addRow(new Object[] {
        host.getIp(),
        host.getEstat(),
        host.getPortsOberts()
    });
}
```

#### Escaneig de Ports en Paralel

```java
public class ScanTask implements Runnable {
    @Override
    public void run() {
        if (!NetworkUtil.isReachable(ip, 200)) return;
        
        ResultatHost host = new ResultatHost(ip);
        List<Integer> portsOberts = Collections.synchronizedList(new ArrayList<>());
        
        ExecutorService portPool = Executors.newFixedThreadPool(10);
        
        for (int port : mode.getPorts()) {
            portPool.execute(() -> {
                if (NetworkUtil.isPortOpen(ip, port, 50)) {
                    portsOberts.add(port);
                }
            });
        }
        
        portPool.shutdown();
        portPool.awaitTermination(10, TimeUnit.MINUTES);
        
        host.setPortsOberts(portsOberts);
        SwingUtilities.invokeLater(() -> vista.afegirResultat(host));
    }
}
```

---

## Instalacio

### Compilacio des del Codi Font

1. **Clonar el repositori**

```bash
git clone https://github.com/usuari/shadowscan.git
cd shadowscan
```

2. **Compilar el projecte**

```bash
cd projecte/src
javac -d ../bin Main.java controller/*.java model/*.java services/*.java utils/*.java view/*.java
```

3. **Executar l'aplicacio**

```bash
cd ../bin
java Main
```

### Verificacio de la Instalacio

En iniciar l'aplicacio, la consola mostrara l'estat de les dependencies:

```
>>> [INIT] IP Local detectada: 192.168.1.100
>>> [BOOT] Verificant eines externes...
>>> [OK] Nmap detectat.
>>> [OK] Hydra detectat.
>>> [OK] Dirb detectat.
>>> [OK] Ffuf detectat.
>>> [BOOT] Verificacio completada.
>>> [INIT] Aplicacio iniciada correctament
```

---

## Configuracio

### Parametres d'Escaneig

| Parametre | Valor per Defecte | Fitxer |
|-----------|-------------------|--------|
| Threads d'escaneig de xarxa | 20 | ScanController.java |
| Threads d'escaneig de ports | 10 | ScanTask.java |
| Timeout de ping | 200 ms | ScanTask.java |
| Timeout de port | 50 ms | ScanTask.java |
| Interval de heartbeat Nmap | -T4 | NmapWrapper.java |

### Ports Escanejats en Mode Parcial

| Port | Servei | Protocol |
|------|--------|----------|
| 21 | FTP | TCP |
| 22 | SSH | TCP |
| 23 | Telnet | TCP |
| 25 | SMTP | TCP |
| 53 | DNS | TCP/UDP |
| 80 | HTTP | TCP |
| 110 | POP3 | TCP |
| 139 | NetBIOS | TCP |
| 143 | IMAP | TCP |
| 443 | HTTPS | TCP |
| 445 | SMB | TCP |
| 3306 | MySQL | TCP |
| 3389 | RDP | TCP |
| 8080 | HTTP Proxy | TCP |

---
## Us d'Intel·ligencia Artificial

Durant el desenvolupament de SHADOWSCAN, s'ha utilitzat Intel·ligència Artificial com a eina de suport en diversos aspectes del projecte:

### Àrees on s'ha utilitzat IA

1. **Generació de documentació tècnica**
   - Estructuració del README
     
2. **Revisió i optimització de codi**
   - Identificació de possibles millores en la gestió de threads
   - Suggeriments per a la implementació de patrons de disseny
   - Detecció de possibles problemes de concurrència

3. **Generació de diagrames UML**
   - Assistència en la creació de diagrames
   - Generació de codi Mermaid per als diagrames
   - Validació de les relacions entre components

4. **Resolució de problemes tècnics**
   - Debugging de problemes amb ProcessBuilder
   - Solucions per a la sincronització entre threads i Swing
   - Optimització de la captura de sortida de processos externs

## Roadmap
Estem treballant per fer créixer el projecte i aquí tenim un recull dels objectius que ens hem marcat per a les properes versions, des de millores tècniques fins a noves interfícies d'usuari.

### Versio 1.1

- [ ] Suport per escaneig de xarxes IPv6
- [ ] Implementacio d'escaneig UDP
- [ ] Sistema de persistencia per guardar sessions

### Versio 1.2

- [ ] Dashboard amb estadistiques i grafics
- [ ] Generacio d'informes en format PDF
- [ ] Integracio amb bases de dades CVE

### Versio 2.0

- [ ] Mode fosc per la interficie
- [ ] Sistema de perfils d'escaneig
- [ ] API REST per integracio amb altres eines
- [ ] Suport per escaneig distribuit

---

## Autors

| Desenvolupador | Rol | Responsabilitats |
|----------------|-----|------------------|
| Oscar | Frontend Lead | Arquitectura de la interficie grafica, components Swing, experiencia d'usuari |
| Nico | Data Architect | Disseny del model de dades, utilitats, diagrames UML, documentacio tecnica |
| Hamza | Backend Lead | Logica de negoci, integracio d'eines externes, sistema de concurrencia |

---

<p align="center">
  <strong>SHADOWSCAN</strong> - Network Security Toolkit
</p>
