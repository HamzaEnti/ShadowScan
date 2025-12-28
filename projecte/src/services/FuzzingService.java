package services;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Random;

public class FuzzingService {

    // Metode per llancar l'atac de fuzzing durant un temps determinat
    public void lanzarFuzzing(String ip, int port, int segons) {
        // Calculem el moment en el futur en que ha d'acabar l'atac (temps actual + segons * 1000 ms)
        long tempsFinal = System.currentTimeMillis() + (segons * 1000);
        
        // Objecte per generar dades aleatories
        Random random = new Random();
        
        // Buffer de bytes per enviar dades brossa
        byte[] dadesAleatories = new byte[1024];

        // Intentem obrir un socket contra la IP i el port objectiu
        try (Socket socket = new Socket(ip, port)) {
            // Obtenim el canal de sortida per enviar dades al servidor
            OutputStream out = socket.getOutputStream();

            // Bucle que s'executara mentre no hagi passat el temps indicat
            while (System.currentTimeMillis() < tempsFinal) {
                // Omplim el buffer amb bytes aleatoris
                random.nextBytes(dadesAleatories);
                
                // Enviem els bytes pel socket
                out.write(dadesAleatories);
                
                // Forcem l'enviament de les dades immediatament
                out.flush();
            }

        } catch (IOException e) {
            // Capturem l'excepcio si el servidor tanca la connexio o hi ha un error de xarxa
            // Aixo es habitual en fuzzing si el servidor peta
            System.out.println("Excepció de connexió: " + e.getMessage());
        }
    }
}