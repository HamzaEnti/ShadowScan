# SHADOWSCAN
 
[![Java Version](https://img.shields.io/badge/java-11%2B-ED8B00?style=for-the-badge&logo=openjdk)](https://www.oracle.com/es/java/technologies/javase/jdk11-archive-downloads.html)
[![License](https://img.shields.io/badge/license-MIT-green?style=for-the-badge)](https://github.com/HamzaEnti/ShadowScan/blob/main/LICENSE)
[![Status](https://img.shields.io/badge/status-active-success?style=for-the-badge)](https://github.com/HamzaEnti/ShadowScan)
[![Architecture](https://img.shields.io/badge/architecture-MVC-blue?style=for-the-badge)](https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93controller)
[![Version](https://img.shields.io/badge/version-1.1.0-purple?style=for-the-badge)](https://github.com/HamzaEnti/ShadowScan/releases)
 
Plataforma de seguretat informatica amb interficie grafica que unifica multiples eines de pentesting en una sola aplicacio. Desenvolupada per a professionals de ciberseguretat i administradors de sistemes que necessiten realitzar auditories de xarxa de manera eficient i visual.
 
> **AVIS LEGAL**: Aquesta eina esta dissenyada exclusivament per a auditories autoritzades. L'us contra sistemes sense permis explicit es il·legal i pot comportar consequencies penals.
 
---
 
## Taula de Continguts
 
1. [Novetats v1.1](#novetats-v11)
2. [Descripcio General](#descripcio-general)
3. [Funcionalitats](#funcionalitats)
4. [Requisits del Sistema](#requisits-del-sistema)
5. [Arquitectura](#arquitectura)
   - [Patro MVC](#patro-mvc)
   - [Estructura del Projecte](#estructura-del-projecte)
6. [Detalls Tecnics](#detalls-tecnics)
   - [Model de Concurrencia](#model-de-concurrencia)
7. [Instalacio](#instalacio)
8. [Configuracio](#configuracio)
9. [Us d'Intel·ligencia Artificial](#us-dintelligencia-artificial)
10. [Roadmap](#roadmap)
11. [Autors](#autors)
---
 
## Novetats v1.1
 
> Versió publicada el **2 de maig de 2026**. Aquesta actualització corregeix tots els bugs detectats a la v1.0, millora la robustesa del sistema de concurrència i incorpora integració nativa amb **RedTrace**.
 
### Bugs corregits
 
| Fitxer | Bug | Solució aplicada |
|--------|-----|-----------------|
| `ScanController.java` | `enExecucio` no era `volatile` → possible race condition entre threads | Declarat com `volatile boolean` per garantir visibilitat entre threads |
| `ScanTask.java` | Mode FULL encolava 65.535 `Runnable` per host → risc de `OutOfMemoryError` | Semàfor `Semaphore(500)` que limita les tasques en cua amb backpressure |
| `BruteForceService.java` | `checkInstalled()` no esperava el procés → procés zombie en background | Afegit `waitFor()` + `destroy()` si supera timeout, igual que `NmapWrapper` |
| `DiscoveryPanel.java` | El botó "Guardar JSON" estava actiu durant l'escaneig → exportació incompleta | Botó deshabilitat durant el scan, s'habilita via callback quan acaba |
 
### Warnings resolts
 
- **Processos sense timeout**: Nmap, Hydra, Dirb i Ffuf podien quedar penjats indefinidament. Ara tots tenen un timeout de 5–10 minuts amb terminació forçada.
- **`java.util.Date` deprecated**: Substituït per `LocalDateTime.now()` a tots els exportadors CSV (`NmapWrapper`, `BruteForceService`, `WebDiscoveryService`, `FuzzingService`).
- **`saveToJSONPretty()` silenciava errors**: La `IOException` es capturava sense cap missatge. Ara es logueja correctament.
- **Validació d'IP massa permissiva**: La comprovació `ip.contains(".")` acceptava qualsevol cadena. Substituïda per regex completa (`^((25[0-5]|...)\.){3}...$`).
- **`WebDiscoveryService.checkInstalled()` zombie**: Mateix problema que Hydra. Corregit amb `waitFor()` i `destroy()`.
### Noves funcionalitats
 
**Integració amb RedTrace**
 
`JsonExporter` incorpora el nou mètode `saveToTopology()` que genera el fitxer `topology.json` compatible amb [RedTrace](https://github.com/HamzaEnti/RedTrace), el simulador de moviment lateral desenvolupat com a projecte ADAA. El format exportat inclou:
 
- **Nodes** amb tipus inferit automàticament (`router`, `webserver`, `database`, `fileserver`, `workstation`) a partir dels ports oberts.
- **Arestes** calculades entre tots els hosts actius de la mateixa subxarxa.
- **Pesos d'explotació** per aresta basats en el risc dels ports oberts (Telnet/SMB/RDP = risc alt, SSH/MySQL = risc mig).
- **Mapa de serveis** per port (`22 → ssh`, `3306 → mysql`, etc.).
- `entry_point` i `target` configurables (per defecte, primer i últim host actiu).
El botó **"Exportar RedTrace"** apareix al panel de Discovery un cop finalitzat l'escaneig.
 
**Columna de risc a la taula de Discovery**
 
La taula de resultats incorpora una columna **Risc** (BAIX / MITJÀ / CRÍTIC) amb codi de colors per files:
 
- CRÍTIC — hosts amb Telnet, SMB, RDP o FTP oberts
- MITJÀ — hosts amb SSH, MySQL, PostgreSQL o MSSQL oberts
- BAIX — resta de casos
**Callback de fi d'escaneig**
 
`ScanController` incorpora la interfície `ScanCallback` que notifica quan tots els threads han acabat. Permet actualitzar la UI (progress bar, botons, comptador) sense polling ni timers.
 
### Millores de disseny
 
**Barra lateral fosca (sidebar)**
 
El menú de navegació passa d'un panel gris genèric a una barra lateral fosca professional:
 
- Fons `#1C2028` amb botons `#28293A`
- Botó actiu destacat en blau `#3B82F6`
- Efecte hover suau `#37415`
- Títol "SHADOWSCAN" en dos colors (blanc / blau)
- Número de versió al peu del sidebar
**Consola de logs redissenyada**
 
- Fons negre `#12141A` amb text verd `#50DC64` estil terminal
- Botó "Netejar" per buidar els logs sense reiniciar
- Marge interior per millor llegibilitat
**Progress bar i feedback en temps real**
 
- Barra de progrés indeterminada durant l'escaneig
- Comptador `Hosts trobats: N` que s'actualitza en temps real a mesura que arriben resultats
- Missatge de finalització amb recompte total
**Finestra principal**
 
- Mida mínima de 900×600 per evitar trencament del layout
- Títol actualitzat a `ShadowScan v1.1 — Network Security Toolkit`
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
| RedTrace Export | JsonExporter | Exportació de topologia de xarxa per a simulació de moviment lateral | **Nou v1.1** |
 
### Caracteristiques Addicionals
 
* **Consola de Logs Integrada**: Monitoratge en temps real de la sortida de totes les eines amb format visual unificat.
* **Exportacio de Resultats**: Generacio d'informes en format JSON, CSV i `topology.json` (RedTrace).
* **Deteccio Automatica de Xarxa**: Identificacio automatica de la IP local i configuracio intel·ligent de parametres.
* **Verificacio de Dependencies**: Comprovacio automatica de la disponibilitat d'eines externes a l'inici.
* **Modes d'Escaneig**: Seleccio entre escaneig rapid (ports comuns) i escaneig exhaustiu (65535 ports).
* **Classificació de Risc**: Cada host descobert rep un nivell BAIX / MITJÀ / CRÍTIC basat en els ports oberts. *(Nou v1.1)*
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
|   |   +-- JsonExporter.java            # Serialitzacio JSON + export RedTrace
|   |
|   +-- view/                            # Capa de presentacio
|   |   +-- BasePanel.java               # Base abstracta per panels
|   |   +-- MainFrame.java               # Finestra principal (sidebar fosc)
|   |   +-- DiscoveryPanel.java          # Modul de descobriment (risc + progress)
|   |   +-- NmapPanel.java               # Modul Nmap
|   |   +-- SecurityPanel.java           # Modul d'eines de seguretat
|   |   +-- ConsoleRedirector.java       # Redirector de sortida
|   |
|   +-- test/                            # Proves
|       +-- ConsoleDebug.java            # Utilitats de depuracio
|
+-- resources/                           # Recursos externs
    +-- wordlists/                       # Diccionaris per atacs
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
    // v1.1: volatile per garantir visibilitat entre threads
    private volatile boolean enExecucio;
 
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
 
#### Escaneig de Ports en Paralel (v1.1 — amb backpressure)
 
```java
public class ScanTask implements Runnable {
    private static final int MAX_QUEUED = 500; // v1.1: evita OOM en mode FULL
 
    @Override
    public void run() {
        if (!NetworkUtil.isReachable(ip, 200)) return;
 
        ResultatHost host = new ResultatHost(ip);
        List<Integer> portsOberts = Collections.synchronizedList(new ArrayList<>());
        ExecutorService portPool = Executors.newFixedThreadPool(50);
 
        // Mode FULL: semàfor per limitar tasques en cua
        Semaphore sem = new Semaphore(MAX_QUEUED);
        for (int port = 1; port <= 65535; port++) {
            final int p = port;
            sem.acquire();
            portPool.execute(() -> {
                try {
                    if (NetworkUtil.isPortOpen(ip, p, 25)) portsOberts.add(p);
                } finally {
                    sem.release();
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
 
#### Export de Topologia per a RedTrace (v1.1)
 
```java
// Genera topology.json compatible amb RedTrace
JsonExporter.saveToTopology(resultats, "topology.json");
 
// Format de sortida:
// {
//   "metadata": { "generated_by": "ShadowScan", "hosts_active": 12 },
//   "nodes": [ { "id": "192.168.1.1", "type": "router", "risk": 0.85 } ],
//   "edges": [ { "from": "192.168.1.1", "to": "192.168.1.10", "weight": 0.3 } ],
//   "entry_point": "192.168.1.1",
//   "target": "192.168.1.10"
// }
```
 
---
 
## Instalacio
 
### Compilacio des del Codi Font
 
1. **Clonar el repositori**
```bash
git clone https://github.com/HamzaEnti/shadowscan.git
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
 
4. **Preview de l'aplicacio**
https://github.com/user-attachments/assets/60fb99e6-6aa6-4d0d-8e14-ed07f73956d8
 
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
>>> [INIT] ShadowScan iniciada correctament.
```
 
---
 
## Configuracio
 
### Parametres d'Escaneig
 
| Parametre | Valor per Defecte | Fitxer |
|-----------|-------------------|--------|
| Threads d'escaneig de xarxa | 20 | ScanController.java |
| Threads d'escaneig de ports | 50 | ScanTask.java |
| Màxim tasques en cua (FULL) | 500 | ScanTask.java |
| Timeout de ping | 200 ms | ScanTask.java |
| Timeout de port | 50 ms | ScanTask.java |
| Timeout processos externs | 5–10 min | *Service.java |
| Interval de heartbeat Nmap | -T4 | NmapWrapper.java |
 
### Ports Escanejats en Mode Parcial
 
| Port | Servei | Protocol | Nivell de Risc |
|------|--------|----------|----------------|
| 21 | FTP | TCP | Alt |
| 22 | SSH | TCP | Mig |
| 23 | Telnet | TCP | Alt |
| 25 | SMTP | TCP | Baix |
| 53 | DNS | TCP/UDP | Baix |
| 80 | HTTP | TCP | Baix |
| 110 | POP3 | TCP | Baix |
| 139 | NetBIOS | TCP | Alt |
| 143 | IMAP | TCP | Baix |
| 443 | HTTPS | TCP | Baix |
| 445 | SMB | TCP | Alt |
| 3306 | MySQL | TCP | Mig |
| 3389 | RDP | TCP | Alt |
| 8080 | HTTP Proxy | TCP | Baix |
 
---
 
## Us d'Intel·ligencia Artificial
 
Durant el desenvolupament de SHADOWSCAN, s'ha utilitzat Intel·ligència Artificial com a eina de suport en diversos aspectes del projecte:
 
### Àrees on s'ha utilitzat IA
 
1. **Generació de documentació tècnica**
   - Estructuració del README i la secció de novetats v1.1
2. **Revisió i optimització de codi**
   - Identificació de possibles millores en la gestió de threads
   - Suggeriments per a la implementació de patrons de disseny
   - Detecció de possibles problemes de concurrència
3. **Resolució de problemes tècnics**
   - Debugging de problemes amb ProcessBuilder
   - Solucions per a la sincronització entre threads i Swing
   - Optimització de la captura de sortida de processos externs
4. **Millores v1.1** *(Assisted by Claude — Anthropic)*
   - Disseny del pattern de backpressure amb `Semaphore` per al mode FULL
   - Disseny del format `topology.json` per a la integració amb RedTrace
   - Redisseny del sidebar fosc i la consola de logs
---
 
## Roadmap
 
### Versio 1.1 — *Publicada maig 2026*
 
- [x] Correcció de race condition en `enExecucio` (`volatile`)
- [x] Backpressure amb `Semaphore` per a mode FULL (fix OOM)
- [x] Fix processos zombie en `checkInstalled()` de tots els serveis
- [x] Timeout de 5–10 min per a Nmap, Hydra, Dirb i Ffuf
- [x] Substitució de `java.util.Date` per `LocalDateTime` (deprecated)
- [x] Botó export deshabilitat durant l'escaneig actiu
- [x] Validació d'IP amb regex completa
- [x] Integració amb RedTrace (`saveToTopology()`)
- [x] Columna de risc a la taula de Discovery (BAIX / MITJÀ / CRÍTIC)
- [x] Sidebar fosc professional amb estat actiu i hover
- [x] Consola redissenyada amb tema fosc i botó de netejar
- [x] Progress bar i comptador de hosts en temps real
### Versio 1.2
 
- [ ] Dashboard amb estadistiques i grafics
- [ ] Generacio d'informes en format PDF
- [ ] Integracio amb bases de dades CVE
### Versio 2.0
 
- [ ] Sistema de perfils d'escaneig
- [ ] API REST per integracio amb altres eines
- [ ] Suport per escaneig distribuit
- [ ] Suport per escaneig de xarxes IPv6
- [ ] Implementacio d'escaneig UDP
---
 
## Autors
 
| Desenvolupador | Rol | Responsabilitats |
|----------------|-----|------------------|
| Oscar | Frontend Lead | Arquitectura de la interficie grafica, components Swing, experiencia d'usuari |
| Nico | Data Architect | Disseny del model de dades, utilitats, diagrames UML, documentacio tecnica |
| Hamza | Backend Lead | Logica de negoci, integracio d'eines externes, sistema de concurrencia |
 
---
 
<p align="center">
  <strong>SHADOWSCAN</strong> — Network Security Toolkit &nbsp;·&nbsp; v1.1.0 &nbsp;·&nbsp; MIT License
</p>
 
