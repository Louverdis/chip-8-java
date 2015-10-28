package chip8;

/**
 * Creado por luismario
 * Fecha: 15/07/15.
 */

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Chip8 myChip = new Chip8();
        myChip.init();
        try {
            myChip.cargarJuego("invaders.c8");
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

        myChip.imprimirMemoria();

        Scanner scanner = new Scanner(System.in);

        System.out.println("\n\nEjecucion del programa c8\n");

        // Ciclo principal
        while(myChip.RUNNING){
            myChip.emularCiclo();

            // Polling del keypad
            //TODO

            // Render
            if(myChip.drawFlag){
                myChip.textRender();
                myChip.drawFlag = false;
            }
            scanner.nextInt();
        }
    }
}
