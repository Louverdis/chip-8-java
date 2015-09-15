package chip8.git;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.Scanner;

/**
 *
 * @author Luis Mario
 */
public class Main {

    /*
    public static void imprimeInteger(int x, IntConsumer f){
        f.accept(x);
    }

    public static void referenciaFuncion1(int x){
        System.out.println(x);
    }

    public static void referenciaFuncion2(int x){
        System.out.println(x*10);
    }

    public static void referenciaFuncion3(int x){
        System.out.println(x*100);
    }

    public static void referenciaFuncion4(int x){
        System.out.println(x*1000);
    }
    */

    public static void main(String[] args) {
        boolean CPU_ACTIVO = true;
        /*
        int i = 1;

        // Variables como apuntadores a metodos
        IntConsumer apuntador = Main::referenciaFuncion1;

        // Arreglo de apuntadores(referencias) a funciones
        IntConsumer arregloFunciones[] = {
            Main::referenciaFuncion1,
            Main::referenciaFuncion2,
            Main::referenciaFuncion3,
            Main::referenciaFuncion4
        };

        imprimeInteger(i, arregloFunciones[0]);
        imprimeInteger(i, arregloFunciones[1]);
        imprimeInteger(i, arregloFunciones[2]);
        imprimeInteger(i, arregloFunciones[3]);
        */

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
        while(CPU_ACTIVO){
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
