package chip8.git;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 *
 * @author Luis Mario
 */
public class Chip8 {
    /***********************
    * Notas de la documentacion del CHIP-8
    *   sitios:
    *       http://en.wikipedia.org/wiki/CHIP-8
    *       http://devernay.free.fr/hacks/chip8/C8TECH10.HTM
    *
    * Systems memory map:
    *
    * 0x000 - 0x1FF -> Chip 8 interpreter (contains font set in emu)
    * 0x050 - 0x0A0 -> Used for the built in 4x5 pixel font set (0-F)
    * 0x200 - 0xFFF -> Program ROM and work RAM
    **************************/

    // Font set del Chip-8. Cada numero/caracter es 4x5 unidades
    final private int chipFontset[] = {
        0xF0, 0x90, 0x90, 0x90, 0xF0, // 0
        0x20, 0x60, 0x20, 0x20, 0x70, // 1
        0xF0, 0x10, 0xF0, 0x80, 0xF0, // 2
        0xF0, 0x10, 0xF0, 0x10, 0xF0, // 3
        0x90, 0x90, 0xF0, 0x10, 0x10, // 4
        0xF0, 0x80, 0xF0, 0x10, 0xF0, // 5
        0xF0, 0x80, 0xF0, 0x90, 0xF0, // 6
        0xF0, 0x10, 0x20, 0x40, 0x40, // 7
        0xF0, 0x90, 0xF0, 0x90, 0xF0, // 8
        0xF0, 0x90, 0xF0, 0x10, 0xF0, // 9
        0xF0, 0x90, 0xF0, 0x90, 0x90, // A
        0xE0, 0x90, 0xE0, 0x90, 0xE0, // B
        0xF0, 0x80, 0x80, 0x80, 0xF0, // C
        0xE0, 0x90, 0x90, 0x90, 0xE0, // D
        0xF0, 0x80, 0xF0, 0x80, 0xF0, // E
        0xF0, 0x80, 0xF0, 0x80, 0x80  // F
    };

    // Memoria del el chip: 0x1000 memory locations (4k)
    final private int memory[] = new int[4096];

    // Registros del CPU(V0,V1,V2...VF)
    final private int V[] = new int[16];

    // Indice de registros(0x000 - 0xFFF)
    private int I;

    // Contador (program counter)
    public int pc;

    private Opcode opcode;

    // Graficas del Chip8:
    //   Blanco y negro.
    //   Pantalla de 2048 pixeles (64*32).
    final private int gfx[] = new int[64*32];

    // Registros del timer
    //  Los timers trabajan a 60 Hz
    private int delayTimer;
    private int soundTimer;

    // STACK del sistema: posee 16 niveles
    final private int stack[] = new int[16];

    //STACK pointer
    private int sp;

    // keypad basado en HEX (0x0 - 0xF)
    final private int key[] = new int[16];

    // Bandera para marcar una accion en pantalla pendiente
    public boolean drawFlag;

    // Arreglo de referencias de funciones
    final private CicloChip8 tablaChip8[] = {
        this::ejecutar00,   this::ejecutar1NNN, this::ejecutar2NNN,
        this::ejecutar3XNN, this::ejecutar4XNN, this::ejecutar5XY0,
        this::ejecutar6XNN, this::ejecutar7XNN, this::ejecutarOpAritmetica,
        this::ejecutar9XY0, this::ejecutarANNN, this::ejecutarBNNN,
        this::ejecutarCXNN, this::ejecutarDXYN, this::ejecutarSkip,
        this::ejecutarFX,   this::opcodeUndefined
    };

    final private CicloChip8 tablaAritmeticaChip8[] = {
        this::ejecutar8XY0, this::ejecutar8XY1, this::ejecutar8XY2,
        this::ejecutar8XY3, this::ejecutar8XY4, this::ejecutar8XY5,
        this::ejecutar8XY6, this::ejecutar8XY7, this::opcodeUndefined,
        this::opcodeUndefined, this::opcodeUndefined, this::opcodeUndefined,
        this::opcodeUndefined, this::opcodeUndefined, this::ejecutar8XYE
    };

    //private Map<Integer, CicloChip8> opcodeFxMap = new Map();

    /****************************************************************
    * Funciones principales del Chip8
    ****************************************************************/
    public void init(){
        // Reset de memoria
        for(int i: memory) i = 0;

        // Reset de registros
        for(int i: V) i = 0;

        // Reset de graficas
        for(int i: gfx) i = 0;

        // Reset del Stacl
        for(int i: stack) i = 0;

        // Carga del fontSet a memoria
        for(int i = 0; i < 80; i++)
            memory[i] = chipFontset[i];

        pc = 0x200;    // Los programas en el Chip-8 inician en esta direccion
        I = 0;         // Reset del indice
        opcode = null; // Reset del opcode actual
        sp = 0;        // Reset del stack pointer

        delayTimer = 0; // Reset de timers
        soundTimer = 0;

        drawFlag = true; // Se marca para actualizar vista
    }

    public void cargarJuego(String juego) throws IOException{
        Path p = FileSystems.getDefault().getPath("", juego);
        byte buffer[] = Files.readAllBytes(p);

        for(int i=0; i < buffer.length; i++){
            // Los datos del programa en el Chip-8 empiezan
            // en la direccion 0x200 (512)
            memory[512+i] = (buffer[i] & 0xFF); // Se convierte a Unsigned
        }
    }

    public void emularCiclo(){
        // Obtener opcode: Compuesto de dos bytes, empezando desde 0x200
        int i_opcode = (memory[pc] << 8) | memory[pc+1];

        //if (i_opcode == 0){
        //    pc += 2;
        //    return;
        //}

        // Desifrar opcode
        opcode = new Opcode(i_opcode);

        // Ejecutar opcode
        ejecutarCiclo(tablaChip8[opcode.header]);

        // Debugg: Imprimir en pantalla resultados
        System.out.printf("Instruccion en 0x%04X: %s\n", pc, opcode.assembly);

        // Tras emular un ciclo, el pc debe moverse dos espacios
        // pues cada ciclo consume dos bytes.
        //pc += 2;
    }

    private void ejecutarCiclo(CicloChip8 ciclo){
        ciclo.ejecutar();
    }

    /****************************************************************
    * Grupo de funciones privadas encargadas de la ejecucion de
    *  los Opcodes durante cada ciclo de emulacion.
    ****************************************************************/
    private void opcodeUndefined(){
        System.out.print("    Opcode no definido: ");
        System.out.printf("0x%04X\n", opcode.hex_opcode);
        opcode.assembly = "UNDEFINED";
    }

    private void ejecutarOpAritmetica(){
        ejecutarCiclo(tablaAritmeticaChip8[opcode.nibble]);
    }

    private void ejecutar00(){
        if(opcode.nibble == 0x0000){
            ejecutar00E0();
        }
        else if(opcode.nibble == 0x000E){
            ejecutar00EE();
        }
        else{
            opcodeUndefined();
        }
    }

    private void ejecutarSkip(){
        if(opcode._byte == 0x009E){
            ejecutarEX9E();
        }
        else if(opcode._byte == 0x00A1){
            ejecutarEXA1();
        }
        else{
            opcodeUndefined();
        }
    }

    private void ejecutarFX(){
        int _byte = opcode._byte;

        switch (_byte) {
            case 0x0007:
                ejecutarFX07();
                break;

            case 0x000A:
                ejecutarFX0A();
                break;

            case 0x0015:
                ejecutarFX15();
                break;

            case 0x0018:
                ejecutarFX18();
                break;

            case 0x001E:
                ejecutarFX1E();
                break;

            case 0x0029:
                ejecutarFX29();
                break;

            case 0x0033:
                ejecutarFX33();
                break;

            case 0x0055:
                ejecutarFX55();
                break;

            case 0x0065:
                ejecutarFX65();
                break;

            default: opcodeUndefined();
        }
    }

    // Por terminar
    private void ejecutar00E0(){
        // 00E0 - CLS
        opcode.identificador = "00E0";
        opcode.assembly = "CLS";
    }

    // Por terminar
    private void ejecutar00EE(){
        // 00EE - RET
        opcode.identificador = "00EE";
        opcode.assembly = "RET";
    }

    // Por terminar
    private void ejecutar1NNN(){
        // 1nnn - JP addr
        opcode.identificador = "1nnn";
        opcode.assembly = "JP "+opcode.address;
    }

    private void ejecutar2NNN(){
        // 2nnn - CALL addr
        opcode.identificador = "2nnn";
        opcode.assembly = "CALL "+opcode.address;

        stack[sp] = pc;
        sp++;
        pc = opcode.address;
    }

    // Por terminar
    private void ejecutar3XNN(){
        // 3xkk - SE Vx, byte
        opcode.identificador = "3xkk";
        opcode.assembly = "SE V"+opcode.vx+" "+opcode._byte;
    }

    // Por terminar
    private void ejecutar4XNN(){
        // 4xkk - SNE Vx, byte
        opcode.identificador = "4xkk";
        opcode.assembly = "SNE V"+opcode.vx+" "+opcode._byte;
    }

    // Por terminar
    private void ejecutar5XY0(){
        // 5xy0 - SE Vx, Vy
        opcode.identificador = "5xy0";
        opcode.assembly = "SE V"+opcode.vx+" V"+opcode.vy;
    }

    // Por terminar
    private void ejecutar6XNN(){
        // 6xkk - LD Vx, byte
        opcode.identificador = "6xkk";
        opcode.assembly = "LD V"+opcode.vx+" "+opcode._byte;
    }

    // Por terminar
    private void ejecutar7XNN(){
        // 7xkk - ADD Vx, byte
        opcode.identificador = "7xkk";
        opcode.assembly = "ADD V"+opcode.vx+" "+opcode._byte;
    }

    // Por terminar
    private void ejecutar8XY0(){
        // 8xy0 - LD Vx, Vy
        opcode.identificador = "8xy0";
        opcode.assembly = "LD V"+opcode.vx+" V"+opcode.vy;
    }

    // Por terminar
    private void ejecutar8XY1(){
        // 8xy1 - OR Vx, Vy
        opcode.identificador = "8xy1";
        opcode.assembly = "OR V"+opcode.vx+" V"+opcode.vy;
    }

    // Por terminar
    private void ejecutar8XY2(){
        // 8xy2 - AND Vx, Vy
        opcode.identificador = "8xy2";
        opcode.assembly = "AND V"+opcode.vx+" V"+opcode.vy;
    }

    // Por terminar
    private void ejecutar8XY3(){
        // 8xy3 - XOR Vx, Vy
        opcode.identificador = "8xy3";
        opcode.assembly = "XOR V"+opcode.vx+" V"+opcode.vy;
    }

    private void ejecutar8XY4(){
        // 8xy4 - ADD Vx, Vy
        opcode.identificador = "8xy4";
        opcode.assembly = "ADD V"+opcode.vx+" V"+opcode.vy;

        // Si la suma de Vx y Vy es mayor a 255, el registro VF se le
        // marca un carry
        if(V[opcode.vy] > (0xFF - V[opcode.vx])){
            // Carry
            V[0xF] = 1;
        }
        else{
            // No carry
            V[0xF] = 0;
        }
        int suma = V[opcode.vx] + V[opcode.vy];

        // Los registros solo tienen un tamaño de un byte,
        // de modo que solo se toman los primeros 8 bits del resultado
        V[opcode.vx] = (suma & 0xFF);
        pc += 2;
    }

    // Por terminar
    private void ejecutar8XY5(){
        // 8xy5 - SUB Vx, Vy
        opcode.identificador = "8xy5";
        opcode.assembly = "SUB V"+opcode.vx+" V"+opcode.vy;
    }

    // Por terminar
    private void ejecutar8XY6(){
        // 8xy6 - SHR Vx {, Vy}
        opcode.identificador = "8xy6";
        opcode.assembly = "SHR V"+opcode.vx+" V"+opcode.vy;
    }

    // Por terminar
    private void ejecutar8XY7(){
        // 8xy7 - SUBN Vx, Vy
        opcode.identificador = "8xy7";
        opcode.assembly = "SUBN V"+opcode.vx+" V"+opcode.vy;
    }

    // Por terminar
    private void ejecutar8XYE(){
        // 8xyE - SHL Vx {, Vy}
        opcode.identificador = "8xyE";
        opcode.assembly = "SHL V"+opcode.vx+" V"+opcode.vy;
    }

    // Por terminar
    private void ejecutar9XY0(){
        // 9xy0 - SNE Vx, Vy
        opcode.identificador = "9xy0";
        opcode.assembly = "SNE V"+opcode.vx+" V"+opcode.vy;
    }

    // Por terminar
    private void ejecutarANNN(){
        // Annn - LD I, addr
        opcode.identificador = "Annn";
        opcode.assembly = "LD I "+opcode.address;
    }

    // Por terminar
    private void ejecutarBNNN(){
        // Bnnn - JP V0, addr
        opcode.identificador = "Bnnn";
        opcode.assembly = "JP V0 "+opcode.address;
    }

    // Por terminar
    private void ejecutarCXNN(){
        // Cxkk - RND Vx, byte
        opcode.identificador = "Cxkk";
        opcode.assembly = "RND V"+opcode.vx+" "+opcode._byte;
    }

    // Por terminar
    private void ejecutarDXYN(){
        // Dxyn - DRW Vx, Vy, nibble
        opcode.identificador = "Dxyn";
        opcode.assembly = "DRW V"+opcode.vx+" V"+opcode.vy+" "+opcode.nibble;
    }

    // Por terminar
    private void ejecutarEX9E(){
        // Ex9E - SKP Vx
        opcode.identificador = "Ex9E";
        opcode.assembly = "SKP V"+opcode.vx;
    }

    // Por terminar
    private void ejecutarEXA1(){
        // ExA1 - SKNP Vx
        opcode.identificador = "ExA1";
        opcode.assembly = "SKNP V"+opcode.vx;
    }

    // Por terminar
    private void ejecutarFX07(){
        // Fx07 - LD Vx, DT
        opcode.identificador = "Fx07";
        opcode.assembly = "LD V"+opcode.vx+" DT";
    }

    // Por terminar
    private void ejecutarFX0A(){
        // Fx0A - LD Vx, K
        opcode.identificador = "Fx0A";
        opcode.assembly = "LD V"+opcode.vx+" K";
    }

    // Por terminar
    private void ejecutarFX15(){
        // Fx15 - LD DT, Vx
        opcode.identificador = "Fx15";
        opcode.assembly = "LD DT V"+opcode.vx;
    }

    // Por terminar
    private void ejecutarFX18(){
        // Fx18 - LD ST, Vx
        opcode.identificador = "Fx18";
        opcode.assembly = "LD ST V"+opcode.vx;
    }

    // Por terminar
    private void ejecutarFX1E(){
        // Fx1E - ADD I, Vx
        opcode.identificador = "Fx1E";
        opcode.assembly = "ADD I, V"+opcode.vx;
    }

    // Por terminar
    private void ejecutarFX29(){
        // Fx29 - LD F, Vx
        opcode.identificador = "Fx29";
        opcode.assembly = "LD F V"+opcode.vx;
    }

    private void ejecutarFX33(){
        // Fx33 - LD B, Vx
        opcode.identificador = "Fx33";
        opcode.assembly = "LD B V"+opcode.vx;

        // Representacion decimal (centenas) en I
        memory[I] = (opcode.vx/100);

        // Representacion decimal (decenas) en I+1
        memory[I+1] = (opcode.vx/10)%10;

        // Representacion decimal (unidades) en I+2
        memory[I+2] = (opcode.vx%100)%10;

        pc += 2;
    }

    // Por terminar
    private void ejecutarFX55(){
        // Fx55 - LD [I], Vx
        opcode.identificador = "Fx55";
        opcode.assembly = "LD I V"+opcode.vx;
    }

    // Por terminar
    private void ejecutarFX65(){
        // Fx65 - LD Vx, [I]
        opcode.identificador = "Fx65";
        opcode.assembly = "LD V"+opcode.vx+" I";
    }


    /*******************************************************************
    * Metodos de DEBUGG
    *******************************************************************/
    public void imprimirMemoriaRaw(){
        for(int i: memory){
            System.out.print(String.format("%02X ", i));
        }
    }

    public void imprimirMemoria(){
        int contador = 32;
        System.out.println("Memory Dump");
        for (int i = 0; i < memory.length; i++){
            if (contador == 32)
                System.out.print(String.format("0x%04X: ", i));

            System.out.print(String.format("%02X ", memory[i]));
            contador--;

            if (contador == 0){
                System.out.println();
                contador = 32;
            }
        }
        System.out.println();
    }

    public void textRender(){
        // Dibujar con texto el gfx[]
        for(int y = 0; y < 32; y++){
            for(int x = 0; x < 64; x++){
                if(gfx[(y*64) + x] == 0)
                    System.out.print("O");

                else
                    System.out.print(" ");
            }
            System.out.println();
        }
        System.out.println();
    }
}
