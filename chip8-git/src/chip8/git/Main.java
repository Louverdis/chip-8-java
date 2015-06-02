/*
 * TODO:
 * El programa ya inicializa la memoria y los registros del CHIP-8.
 * El programa ya lee un programa .c8 y es capaz de cargarlo a la memoria
 */
package chip8.git;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Luis Mario
 */
public class Main {

    public static void main(String[] args) {
        Chip8 myChip = new Chip8();
        myChip.init();
        try {
            myChip.cargarJuego("pong2.c8");
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

        myChip.imprimirMemoria();

        System.out.println("\n\nDisassembly del programa c8");
        while(myChip.get_pc() < 4096){
            myChip.emularCiclo();
        }
    }

}
