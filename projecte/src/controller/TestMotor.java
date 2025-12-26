package controller;

public class TestMotor {

    public static void main(String[] args) {
        ScanController control = new ScanController();

        // ip de proves, cal mantenir el punt final
        String xarxaLocal = "192.168.1."; 

        System.out.println("--- Test de Logica Backend ---");
        
        // iniciem la carrega de treball
        control.escanearRang(xarxaLocal);

        // simulacio d'espera mentre els fils treballen
        try {
            Thread.sleep(10000); 
            
            // provem l'aturada
            control.aturar();
            
        } catch (Exception e) {
            System.out.println("Error en l'espera del test");
        }
    }
}