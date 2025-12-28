package services;

import java.util.List;

public class BruteForceService {

    // Llistes per guardar els diccionaris d'usuaris i contrassenyes
    private List<String> usuaris;
    private List<String> passwords;

    // Constructor que rep les llistes i les assigna a les propietats de la classe
    public BruteForceService(List<String> usuaris, List<String> passwords) {
        this.usuaris = usuaris;
        this.passwords = passwords;
    }

    // Metode principal per realitzar l'atac de forca bruta
    public void atacar(String ip, int port) {
        // Primer bucle: recorre tota la llista d'usuaris
        for (String usuari : usuaris) {
            
            // Segon bucle: per a cada usuari, prova totes les contrassenyes de la llista
            for (String password : passwords) {
                
                // Imprimim per consola la combinacio que estem provant ara mateix
                System.out.println("Provant " + usuari + ":" + password);

                try {
                    // Fem una pausa de 50 milisegons per simular el temps de xarxa o proces
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    // Gestio de l'excepcio si el fil s'interromp
                    e.printStackTrace();
                }

                // Verifiquem si hem trobat les credencials correctes (hardcoded segons enunciat)
                if ("admin".equals(usuari) && "1234".equals(password)) {
                    // Si coincideix, imprimim el missatge d'exit
                    System.out.println("PASSWORD TROBAT!");
                }
            }
        }
    }
}