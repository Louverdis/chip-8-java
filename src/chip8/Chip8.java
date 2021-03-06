package chip8;

/**
 * Creado por Luis Mario Reyes Moreno
 * Fecha: 15/07/15.
 */

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Chip8 {
    /***********************
     * Notas de la documentacion del CHIP-8
     *   Sitios de referencias:
     *       http://en.wikipedia.org/wiki/CHIP-8
     *       http://devernay.free.fr/hacks/chip8/C8TECH10.HTM
     *
     *   Super Chip-48 Instructions:
     *      00Cn - SCD nibble
     *      00FB - SCR
     *      00FC - SCL
     *      00FD - EXIT
     *      00FE - LOW
     *      00FF - HIGH
     *      Dxy0 - DRW Vx, Vy, 0
     *      Fx30 - LD HF, Vx
     *      Fx75 - LD R, Vx
     *      Fx85 - LD Vx, R
     *
     * System memory map:
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
    final public int gfx[] = new int[64*32];

    // Registros del timer
    //  Los timers trabajan a 60 Hz
    private int delayTimer;
    private int soundTimer;

    // STACK del sistema: posee 16 niveles
    final private int stack[] = new int[16];

    //STACK pointer
    private int sp;

    // Variable usada para generar numeros random en algunos opcodes
    final private Random rand = new Random();

    // keypad basado en HEX (0x0 - 0xF)
    final public int key[] = new int[16];

    // Bandera para marcar una accion en pantalla pendiente
    public boolean drawFlag;

    // Arreglos de referencias de metodos
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

    // Mapa de Opcodes FX
    private Map<Integer, CicloChip8> opcodeFxMap = new HashMap<>();

    // Bandera para despliegue de informacion en ejecucion
    private boolean debugMode;

    // Bandera del estado del chip
    public boolean RUNNING;

    // Periodo de ejecucion del CPU: 500hz - 1000hz
    public long period;

    /****************************************************************
     * Constructores
     ****************************************************************/
    public Chip8(){
        // Llenado de mapa de funciones FX
        llenarOpcodeFxMap();
        debugMode = true;
        RUNNING = true;
        period = 3;
    }

    public Chip8(boolean debug){
        // Llenado de mapa de funciones FX
        llenarOpcodeFxMap();
        debugMode = debug;
        RUNNING = true;
    }

    private void llenarOpcodeFxMap(){
        opcodeFxMap.put(0x07, this::ejecutarFX07);
        opcodeFxMap.put(0x0A, this::ejecutarFX0A);
        opcodeFxMap.put(0x15, this::ejecutarFX15);
        opcodeFxMap.put(0x18, this::ejecutarFX18);
        opcodeFxMap.put(0x1E, this::ejecutarFX1E);
        opcodeFxMap.put(0x29, this::ejecutarFX29);
        opcodeFxMap.put(0x33, this::ejecutarFX33);
        opcodeFxMap.put(0x55, this::ejecutarFX55);
        opcodeFxMap.put(0x65, this::ejecutarFX65);
    }

    /****************************************************************
     * Funciones principales del Chip8
     ****************************************************************/
    public void init(){
        // Reset de memoria
        for(int i = 0; i < memory.length; i++) memory[i] = 0;

        // Reset de registros
        for(int i = 0; i < V.length; i++) V[i] = 0;

        // Reset de keypad
        for(int i = 0; i < key.length; i++) key[i] = 0;

        // Reset de graficas
        for(int i = 0; i < gfx.length; i++) gfx[i] = 0;

        // Reset del Stack
        for(int i = 0; i < stack.length; i++) stack[i] = 0;

        // Carga del fontSet a memoria
        System.arraycopy(chipFontset, 0, memory, 0, 80);

        // Set del program counter: Los programas en el Chip-8 inician en esta direccion
        pc = 0x200;

        // Reset del indice, opcode y stack pointer
        opcode = null;
        sp = 0;
        I = 0;

        // Reset de timers
        delayTimer = 0;
        soundTimer = 0;

        // Se marca para actualizar vista
        drawFlag = true;
    }

    public void cargarJuego(String juego) throws IOException{
        Path p = FileSystems.getDefault().getPath("", juego);
        byte buffer[] = Files.readAllBytes(p);

        for(int i=0; i < buffer.length; i++){
            // Los datos del programa en el Chip-8 empiezan en la direccion 0x200 (512)
            memory[512+i] = (buffer[i] & 0xFF); // Se convierte a Unsigned
        }
    }

    public void setKeyPad(int[] keyBuffer) {
        System.arraycopy(keyBuffer, 0, key, 0, key.length);
    }

    public void emularCiclo(){
        // Obtener opcode: Compuesto de dos bytes, empezando desde 0x200
        int i_opcode = (memory[pc] << 8) | memory[pc+1];

        // Desifrar opcode
        opcode = new Opcode(i_opcode);

        // Ejecutar opcode
        ejecutarCiclo(tablaChip8[opcode.header]);

        // Operaciones con timers
        actualizarTimers();

        // Debugg: Imprimir en pantalla resultados
        if(debugMode)
            imprimirResultados();
    }

    private void actualizarTimers(){
        if(delayTimer > 0)
            delayTimer--;

        if(soundTimer > 0){
            if(soundTimer == 1)
                System.out.print("BEEP!\n");
            soundTimer--;
        }
    }

    public void ejecutarCiclo(CicloChip8 ciclo){
        ciclo.ejecutar();
    }

    /****************************************************************
     * Grupo de metodos encargadas de la ejecucion de
     *  los Opcodes durante cada ciclo de emulacion.
     ****************************************************************/
    public void opcodeUndefined(){
        System.out.print("    Opcode no definido: ");
        System.out.printf("0x%04X\n", opcode.hex_opcode);
        opcode.assembly = "UNDEFINED";
    }

    public void ejecutarOpAritmetica(){
        ejecutarCiclo(tablaAritmeticaChip8[opcode.nibble]);
    }

    public void ejecutar00(){
        if(opcode.nibble == 0x0000)
            ejecutar00E0();

        else if(opcode.nibble == 0x000E)
            ejecutar00EE();

        else
            opcodeUndefined();
    }

    public void ejecutarSkip(){
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

    public void ejecutarFX(){
        int _byte = opcode._byte;
        ejecutarCiclo(opcodeFxMap.getOrDefault(_byte, this::opcodeUndefined));
    }

    public void ejecutar00E0(){
        /* Documentacion: 00E0 - CLS
        * Clear the display
        */
        opcode.identificador = "00E0";
        opcode.assembly = "CLS";

        // Limpiado de pantalla
        for(int i = 0; i < 2048; i++){
            gfx[i] = 0x0;
        }

        drawFlag = true;
        pc += 2;
    }

    public void ejecutar00EE(){
        /* Documentacion: 00EE - RET
        * Return from a subroutine.
        *
        * The interpreter sets the program counter to the address at the top of
        * the stack, then subtracts 1 from the stack pointer.
        */
        opcode.identificador = "00EE";
        opcode.assembly = "RET";

        // Primero se resta para evitar sobre-escrituras
        sp--;
        pc = stack[sp];
        pc += 2;
    }

    public void ejecutar1NNN(){
        /* Documentacion: 1nnn - JP addr
        * Jump to location nnn.
        *
        * The interpreter sets the program counter to nnn.
        */
        opcode.identificador = "1nnn";
        opcode.assembly = String.format("JP %03X", opcode.address);

        pc = opcode.address;
    }

    public void ejecutar2NNN(){
        /* Documentacion: 2nnn - CALL addr
        * Call subroutine at nnn.
        *
        * The interpreter increments the stack pointer, then puts the current
        * PC on the top of the stack. The PC is then set to nnn.
        */
        opcode.identificador = "2nnn";
        opcode.assembly = String.format("CALL %03X", opcode.address);

        stack[sp] = pc;
        sp++;
        pc = opcode.address;
    }

    public void ejecutar3XNN(){
        /* Documentacion: 3xkk - SE Vx, byte
        * Skip next instruction if Vx = kk.
        *
        * The interpreter compares register Vx to kk, and if they are equal,
        * increments the program counter by 2.
        */
        opcode.identificador = "3xkk";
        opcode.assembly = String.format("SE V%01X %02X", opcode.vx, opcode._byte);

        if(V[opcode.vx] == opcode._byte)
            pc += 4;
        else
            pc += 2;
    }

    public void ejecutar4XNN(){
        /* Documentacion: 4xkk - SNE Vx, byte
        * Skip next instruction if Vx != kk.
        *
        * The interpreter compares register Vx to kk, and if they are not
        * equal, increments the program counter by 2.
        */
        opcode.identificador = "4xkk";
        opcode.assembly = String.format("SNE V%01X %02X", opcode.vx, opcode._byte);

        if(V[opcode.vx] != opcode._byte)
            pc += 4;
        else
            pc += 2;
    }

    public void ejecutar5XY0(){
        /* Documentacion: 5xy0 - SE Vx, Vy
        * Skip next instruction if Vx = Vy.
        *
        * The interpreter compares register Vx to register Vy, and if they
        * are equal, increments the program counter by 2.
        */
        opcode.identificador = "5xy0";
        opcode.assembly = String.format("SE V%01X V%01X", opcode.vx, opcode.vy);

        if(V[opcode.vx] == V[opcode.vy])
            pc += 4;
        else
            pc += 2;
    }

    public void ejecutar6XNN(){
        /* Documentacion: 6xkk - LD Vx, byte
        * Set Vx = kk.
        *
        * The interpreter puts the value kk into register Vx.
        */
        opcode.identificador = "6xkk";
        opcode.assembly = String.format("LD V%01X %02X", opcode.vx, opcode._byte);

        V[opcode.vx] = opcode._byte;
        pc += 2;
    }

    public void ejecutar7XNN(){
        /* Documentacion: 7xkk - ADD Vx, byte
        * Set Vx = Vx + kk.
        *
        * Adds the value kk to the value of register Vx, then stores the
        *¨result in Vx.
        */
        opcode.identificador = "7xkk";
        opcode.assembly = String.format("ADD V%01X %02X", opcode.vx, opcode._byte);

        int suma = V[opcode.vx] + opcode._byte;
        V[opcode.vx] = (suma & 0xFF);
        //V[opcode.vx] += opcode._byte;
        pc += 2;
    }

    public void ejecutar8XY0(){
        /* Documentacion: 8xy0 - LD Vx, Vy
        * Set Vx = Vy.
        *
        * Stores the value of register Vy in register Vx.
        */
        opcode.identificador = "8xy0";
        opcode.assembly = String.format("LD V%01X V%01X", opcode.vx, opcode.vy);

        V[opcode.vx] = V[opcode.vy];
        pc += 2;
    }

    public void ejecutar8XY1(){
        /* Documentacion: 8xy1 - OR Vx, Vy
        * Set Vx = Vx OR Vy.
        *
        * Performs a bitwise OR on the values of Vx and Vy, then stores the
        * result in Vx. A bitwise OR compares the corrseponding bits from two
        * values, and if either bit is 1, then the same bit in the result is
        * also 1. Otherwise, it is 0.
        */
        opcode.identificador = "8xy1";
        opcode.assembly = String.format("OR V%01X V%01X", opcode.vx, opcode.vy);

        V[opcode.vx] |= V[opcode.vy];
        pc += 2;
    }

    public void ejecutar8XY2(){
        /* Documentacion: 8xy2 - AND Vx, Vy
        * Set Vx = Vx AND Vy.
        *
        * Performs a bitwise AND on the values of Vx and Vy, then stores the
        * result in Vx. A bitwise AND compares the corrseponding bits from two
        * values, and if both bits are 1, then the same bit in the result is
        * also 1. Otherwise, it is 0.
        */
        opcode.identificador = "8xy2";
        opcode.assembly = String.format("ADD V%01X V%01X", opcode.vx, opcode.vy);

        V[opcode.vx] &= V[opcode.vy];
        pc += 2;
    }

    public void ejecutar8XY3(){
        /* Documentacion: 8xy3 - XOR Vx, Vy
        * Set Vx = Vx XOR Vy.
        *
        * Performs a bitwise exclusive OR on the values of Vx and Vy, then
        * stores the result in Vx. An exclusive OR compares the corrseponding
        * bits from two values, and if the bits are not both the same, then
        * the corresponding bit in the result is set to 1. Otherwise, it is 0.
        */
        opcode.identificador = "8xy3";
        opcode.assembly = String.format("XOR V%01X V%01X", opcode.vx, opcode.vy);

        V[opcode.vx] ^= V[opcode.vy];
        pc += 2;
    }

    public void ejecutar8XY4(){
        /* Documentacion: 8xy4 - ADD Vx, Vy
        * Set Vx = Vx + Vy, set VF = carry.
        *
        * The values of Vx and Vy are added together. If the result is greater
        * than 8 bits (i.e., > 255,) VF is set to 1, otherwise 0.
        * Only the lowest 8 bits of the result are kept, and stored in Vx.
        */
        opcode.identificador = "8xy4";
        opcode.assembly = String.format("ADD V%01X V%01X", opcode.vx, opcode.vy);

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

    public void ejecutar8XY5(){
        /* Documentacion: 8xy5 - SUB Vx, Vy
        * Set Vx = Vx - Vy, set VF = NOT borrow.
        *
        * If Vx > Vy, then VF is set to 1, otherwise 0. Then Vy is subtracted
        * from Vx, and the results stored in Vx.
        */
        opcode.identificador = "8xy5";
        opcode.assembly = String.format("SUB V%01X V%01X", opcode.vx, opcode.vy);

        if(V[opcode.vx] > V[opcode.vy])
            V[0xF] = 1;
        else
            V[0xF] = 0;

        int resta = V[opcode.vx] - V[opcode.vy];
        V[opcode.vx] = (resta & 0xFF);
        pc += 2;
    }

    public void ejecutar8XY6(){
        /* Documentacion: 8xy6 - SHR Vx {, Vy}
        * Set Vx = Vx SHR 1.
        *
        * If the least-significant bit of Vx is 1, then VF is set to 1,
        * otherwise 0. Then Vx is divided by 2.
        */
        opcode.identificador = "8xy6";
        opcode.assembly = String.format("SHR V%01X { V%01X }", opcode.vx, opcode.vy);

        // El bit menos significante es el de la derecha
        V[0xF] = V[opcode.vx] & 0x1; // Mascara del ultimo bit

        // Dividir entre dos es un shift a la derecha de 1 (SHR 1)
        V[opcode.vx] >>= 1;
        pc += 2;
    }

    public void ejecutar8XY7(){
        /* Documentacion: 8xy7 - SUBN Vx, Vy
        * Set Vx = Vy - Vx, set VF = NOT borrow.
        *
        * If Vy > Vx, then VF is set to 1, otherwise 0. Then Vx is subtracted
        * from Vy, and the results stored in Vx.
        */
        opcode.identificador = "8xy7";
        opcode.assembly = String.format("SUBN V%01X V%01X", opcode.vx, opcode.vy);

        if(V[opcode.vx] > V[opcode.vy])
            V[0xF] = 0;
        else
            V[0xF] = 1;

        int resta = V[opcode.vy] - V[opcode.vx];
        V[opcode.vx] = resta & 0xFF;
        pc += 2;
    }

    public void ejecutar8XYE(){
        /* Documentacion: 8xyE - SHL Vx {, Vy}
        * Set Vx = Vx SHL 1.
        *
        * If the most-significant bit of Vx is 1, then VF is set to 1,
        * otherwise to 0. Then Vx is multiplied by 2.
        */
        opcode.identificador = "8xyE";
        opcode.assembly = String.format("SHL V%01X { V%01X }", opcode.vx, opcode.vy);

        // El bit mas significativo es el de la izquierda
        V[0xF] = V[opcode.vx] >> 7; // El shift solo deja al bit necesario

        // Una multiplicacion por dos equivale a un shift izq de 1(SHL 1)
        V[opcode.vx] <<= 1;
        pc += 2;
    }

    public void ejecutar9XY0(){
        /* Documentacion: 9xy0 - SNE Vx, Vy
        * Skip next instruction if Vx != Vy.
        *
        * The values of Vx and Vy are compared, and if they are not equal,
        * the program counter is increased by 2.
        */
        opcode.identificador = "9xy0";
        opcode.assembly = String.format("SNE V%01X V%01X", opcode.vx, opcode.vy);

        if(V[opcode.vx] != V[opcode.vy])
            pc += 4;
        else
            pc += 2;
    }

    public void ejecutarANNN(){
        /* Documentacion: Annn - LD I, addr
        * Set I = nnn.
        *
        * The value of register I is set to nnn.
        */
        opcode.identificador = "Annn";
        opcode.assembly = String.format("LD I %02X", opcode.address);

        I = opcode.address;
        pc += 2;
    }

    public void ejecutarBNNN(){
        /* Documentacion: Bnnn - JP V0, addr
        * Jump to location nnn + V0.
        *
        * The program counter is set to nnn plus the value of V0.
        */
        opcode.identificador = "Bnnn";
        opcode.assembly = String.format("JP V0 %02X", opcode.address);

        pc = opcode.address + V[0];
    }

    public void ejecutarCXNN(){
        /* Documentacion: Cxkk - RND Vx, byte
        * Set Vx = random byte AND kk.
        *
        * The interpreter generates a random number from 0 to 255, which is
        * then ANDed with the value kk. The results are stored in Vx. See
        * instruction 8xy2 for more information on AND.
        */
        opcode.identificador = "Cxkk";
        opcode.assembly = String.format("RND V%01X %02X", opcode.vx, opcode._byte);

        // Numero random con valores de 0 - 255
        int n_rand = rand.nextInt(256);
        V[opcode.vx] = opcode._byte & n_rand;

        pc += 2;
    }

    public void ejecutarDXYN(){
        /* Documentacion: Dxyn - DRW Vx, Vy, nibble
        * Display n-byte sprite starting at memory location I at (Vx, Vy),
        * set VF = collision.

        * The interpreter reads n bytes from memory, starting at the address
        * stored in I. These bytes are then displayed as sprites on screen at
        * coordinates (Vx, Vy). Sprites are XORed onto the existing screen.
        * If this causes any pixels to be erased, VF is set to 1, otherwise
        * it is set to 0. If the sprite is positioned so part of it is outside
        * the coordinates of the display, it wraps around to the opposite side
        * of the screen. See instruction 8xy3 for more information on XOR,
        * and section 2.4, Display, for more information on the Chip-8
        * screen and sprites
        */
        opcode.identificador = "Dxyn";
        opcode.assembly = String.format(
                "DRW V%01X V%01X %01X", opcode.vx, opcode.vy, opcode.nibble
        );

        // Posicion x,y del sprite a dibujar
        int x = V[opcode.vx];
        int y = V[opcode.vy];

        int pixel;

        V[0xF] = 0;
        // Loop de las filas del sprite
        for(int ejeY=0; ejeY < opcode.nibble; ejeY++){
            pixel = memory[I+ejeY];

            // Loop de los 8 bits de la fila
            for(int ejeX=0; ejeX < 8; ejeX++){
                // Se comprueba si el bit actual se debera de pintar.
                // Se usa 0x80 (1000 0000 en binario) para ir recorriendo los bits de la fila del sprite
                int masked_pixel = (pixel & (0x80 >> ejeX));

                if(masked_pixel != 0){
                    // Los valores posicionales x,y haran
                    // warp de valores tras superar 0x3F(63) y 0x1F(31) respectivamente

                    // Solo se toman los 6 bits de la izq
                    int cordenadaX = (x + ejeX) & 0x3F;

                    // Solo se toman los 5 bits de la izq
                    int cordenadaY = (y + ejeY) & 0x1F;

                    // Si el pixel a pintar ya esta activo, se asigna 1 al
                    // registro VF, normalmente usado para detectar colision
                    if(gfx[(cordenadaX + ((cordenadaY)*64))] == 1){
                        V[0xF] = 1;
                    }

                    // El nuevo valor en pantalla se define con una operacion XOR
                    gfx[(cordenadaX + ((cordenadaY)*64))] ^= 1;
                }
            }
        }

        // La pantalla se marca para una actualizacion
        drawFlag = true;
        pc += 2;
    }

    public void ejecutarEX9E(){
        /* Documentacion: Ex9E - SKP Vx
        * Skip next instruction if key with the value of Vx is pressed.
        *
        * Checks the keyboard, and if the key corresponding to the value of
        * Vx is currently in the down position, PC is increased by 2.
        */
        opcode.identificador = "Ex9E";
        opcode.assembly = String.format("SKP V%01X", opcode.vx);

        // Si la tecla almacenada en Vx esta presionada, se salta la siguiente instruccion
        if(key[V[opcode.vx]] != 0)
            pc += 4;
        else
            pc += 2;
    }

    public void ejecutarEXA1(){
        /* Documentacion: ExA1 - SKNP Vx
        * Skip next instruction if key with the value of Vx is not pressed.
        *
        * Checks the keyboard, and if the key corresponding to the value of Vx
        * is currently in the up position, PC is increased by 2.
        */
        opcode.identificador = "ExA1";
        opcode.assembly = String.format("SKNP V%01X", opcode.vx);

        // Lo inverso a EX9E
        if(key[V[opcode.vx]] == 0)
            pc += 4;
        else
            pc += 2;
    }

    public void ejecutarFX07(){
        /* Documentacion: Fx07 - LD Vx, DT
        * Set Vx = delay timer value.
        *
        * The value of DT is placed into Vx.*/
        opcode.identificador = "Fx07";
        opcode.assembly = String.format("LD V%01X DT", opcode.vx);

        V[opcode.vx] = delayTimer;
        pc += 2;
    }

    public void ejecutarFX0A(){
        /* Documentacion: Fx0A - LD Vx, K
        * Wait for a key press, store the value of the key in Vx.
        *
        * All execution stops until a key is pressed, then the value of that
        * key is stored in Vx.
        */
        opcode.identificador = "Fx0A";
        opcode.assembly = String.format("LD V%01X K", opcode.vx);

        boolean keyPressed = false;

        for(int i = 0; i < 16; i++){
            if(key[i] != 0){
                V[opcode.vx] = i;
                keyPressed = true;
            }
        }

        // Si no se encontro una tecla presionada, se termina la ejecucion y se intenta otra vez.
        if(!keyPressed) return;

        pc += 2;
    }

    public void ejecutarFX15(){
        /* Documentacion: Fx15 - LD DT, Vx
        * Set delay timer = Vx.
        *
        * DT is set equal to the value of Vx.
        */
        opcode.identificador = "Fx15";
        opcode.assembly = String.format("LD DT V%01X", opcode.vx);

        delayTimer = V[opcode.vx];
        pc += 2;
    }

    public void ejecutarFX18(){
        /* Documentacion: Fx18 - LD ST, Vx
        * Set sound timer = Vx.
        *
        * ST is set equal to the value of Vx.
        */
        opcode.identificador = "Fx18";
        opcode.assembly = String.format("LD ST V%01X", opcode.vx);

        soundTimer = V[opcode.vx];
        pc += 2;
    }

    public void ejecutarFX1E(){
        /* Documentacion: Fx1E - ADD I, Vx
        * Set I = I + Vx.
        *
        * The values of I and Vx are added, and the results are stored in I.
        */
        opcode.identificador = "Fx1E";
        opcode.assembly = String.format("ADD I V%01X", opcode.vx);

        // Se coloca una bandera en VF si en I existe un range overflow
        if((I + V[opcode.vx]) > 0xFFF)
            V[0xF] = 1;
        else
            V[0xF] = 0;

        I += V[opcode.vx];
        pc += 2;
    }

    public void ejecutarFX29(){
        /* Documentacion: Fx29 - LD F, Vx
        * Set I = location of sprite for digit Vx.
        *
        * The value of I is set to the location for the hexadecimal sprite
        * corresponding to the value of Vx. See section 2.4, Display, for more
        * information on the Chip-8 hexadecimal font.
        */
        opcode.identificador = "Fx29";
        opcode.assembly = String.format("LD F V%01X", opcode.vx);

        I = V[opcode.vx] * 0x5;
        pc += 2;
    }

    public void ejecutarFX33(){
        /* Documentacion: Fx33 - LD B, Vx
        * Store BCD representation of Vx in memory locations I, I+1, and I+2.
        *
        * The interpreter takes the decimal value of Vx, and places the
        * hundreds digit in memory at location in I, the tens digit at
        * location I+1, and the ones digit at location I+2.
        */
        opcode.identificador = "Fx33";
        opcode.assembly = String.format("LD B V%01X", opcode.vx);

        // Representacion decimal (centenas) en I
        memory[I] = (V[opcode.vx]/100);

        // Representacion decimal (decenas) en I+1
        memory[I+1] = (V[opcode.vx]/10)%10;

        // Representacion decimal (unidades) en I+2
        memory[I+2] = (V[opcode.vx]%100)%10;

        pc += 2;
    }

    @SuppressWarnings("ManualArrayCopy")
    public void ejecutarFX55(){
        /* Documentacion: Fx55 - LD [I], Vx
        * Store registers V0 through Vx in memory starting at location I.
        *
        * The interpreter copies the values of registers V0 through Vx into
        * memory, starting at the address in I.
        */
        opcode.identificador = "Fx55";
        opcode.assembly = String.format("LD [I] V%01X", opcode.vx);

        for(int i=0; i<= opcode.vx; i++){
            memory[I + i] = V[i];
        }

        // En el interprete original del Chip-8, tras terminar esta operacion,
        // se asigna I = I + X + 1
        I += (opcode.vx + 1);

        pc += 2;
    }

    @SuppressWarnings("ManualArrayCopy")
    public void ejecutarFX65(){
        /* Documentacion: Fx65 - LD Vx, [I]
        * Read registers V0 through Vx from memory starting at location I.
        *
        * The interpreter reads values from memory starting at location I
        * into registers V0 through Vx.
        */
        opcode.identificador = "Fx65";
        opcode.assembly = String.format("LD V%01X [I]", opcode.vx);

        for(int i=0; i<= opcode.vx; i++){
            V[i] = memory[I + i];
        }

        // En el interprete original del Chip-8, tras terminar esta operacion,
        // se asigna I = I + X + 1
        I += (opcode.vx + 1);

        pc += 2;
    }

    /*******************************************************************
     * Metodos de DEBUGG
     *******************************************************************/
    @SuppressWarnings("unused")
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

    public void imprimirResultados(){
        System.out.printf("Instruccion en 0x%04X: %s\n", pc, opcode.assembly);
        System.out.printf(
                "\tEn base al opcode: %04X -- id: %s\n",
                opcode.hex_opcode,
                opcode.identificador
        );
    }

    @SuppressWarnings("unused")
    public void imprimirDetalleChip(){
        System.out.println("\t-----------------");

        System.out.println("Detalle de registros");
        for(int i=0; i<16; i++){
            System.out.printf("V%01X: %02X - ", i, V[i]);
        }

        System.out.println("\nValor de I: "+I);
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
