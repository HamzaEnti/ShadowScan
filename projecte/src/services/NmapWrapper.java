package services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class NmapWrapper {

    // Metode per comprovar si l'eina Nmap esta disponible al sistema
    public boolean checkNmapInstalled() {
        // Preparem la comanda "nmap --version" per veure si respon
        ProcessBuilder pb = new ProcessBuilder("nmap", "--version");
        try {
            // Iniciem el proces
            Process process = pb.start();
            
            // Esperem a que el proces acabi i guardem el codi de sortida
            int exitCode = process.waitFor();
            
            // Si el codi de sortida es 0, significa que s'ha executat correctament (true)
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            // Si hi ha qualsevol error o excepcio, assumim que no esta installat
            e.printStackTrace();
            return false;
        }
    }

    // Metode per fer un escaneig rapid i de versions a una IP concreta
    public String escanearConNmap(String ip) {
        StringBuilder resultat = new StringBuilder();
        
        // Configurem el ProcessBuilder amb la comanda: nmap -sV -F [IP]
        // -sV: deteccio de versions
        // -F: mode fast (escaneja menys ports)
        ProcessBuilder pb = new ProcessBuilder("nmap", "-sV", "-F", ip);
        
        try {
            // Iniciem el proces d'escaneig
            Process process = pb.start();
            
            // Creem un reader per llegir el flux d'entrada (la sortida del terminal)
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String linia;
                
                // Llegim linia per linia fins que no hi hagi mes text
                while ((linia = reader.readLine()) != null) {
                    // Afegim la linia al nostre StringBuilder i un salt de linia
                    resultat.append(linia).append("\n");
                }
            }
            
            // Esperem que el proces nmap acabi del tot
            process.waitFor();

        } catch (IOException | InterruptedException e) {
            // Si falla, imprimim l'error i retornem un missatge d'avis
            e.printStackTrace();
            return "Error al executar Nmap";
        }

        // Retornem tot el text capturat com un unic String
        return resultat.toString();
    }
}