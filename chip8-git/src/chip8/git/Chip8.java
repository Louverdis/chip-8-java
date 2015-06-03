package chip8.git;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Random;

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
    
    // Variable usada para generar numeros random en algunos opcodes
    final private Random rand = new Random();

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
        System.arraycopy(chipFontset, 0, memory, 0, 80);

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

        actualizarTimers();
        
        // Debugg: Imprimir en pantalla resultados
        System.out.printf("Instruccion en 0x%04X: %s\n", pc, opcode.assembly);

        // Tras emular un ciclo, el pc debe moverse dos espacios
        // pues cada ciclo consume dos bytes.
        //pc += 2;
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

    private void ejecutar00E0(){
        /* Documentacion: 00E0 - CLS
        * Clear the display
        */
        opcode.identificador = "00E0";
        opcode.assembly = "CLS";
        
        // Limpiado de pantalla
        for(int i: gfx) i = 0;
        drawFlag = true;
        pc += 2;
    }

    private void ejecutar00EE(){
        /* Documentacion: 00EE - RET
        * Return from a subroutine.
        *
        * The interpreter sets the program counter to the address at the top of 
        * the stack, then subtracts 1 from the stack pointer.
        */
        opcode.identificador = "00EE";
        opcode.assembly = "RET";
        
        sp--; // Primero se resta para evitar sobre-escrituras
        pc = stack[sp];
        pc += 2;        
    }

    private void ejecutar1NNN(){
        /* Documentacion: 1nnn - JP addr
        * Jump to location nnn.
        *
        * The interpreter sets the program counter to nnn.
        */
        opcode.identificador = "1nnn";
        opcode.assembly = "JP "+opcode.address;
        
        pc = opcode.address;
    }

    private void ejecutar2NNN(){
        /* Documentacion: 2nnn - CALL addr
        * Call subroutine at nnn.
        *
        * The interpreter increments the stack pointer, then puts the current 
        * PC on the top of the stack. The PC is then set to nnn.
        */        
        opcode.identificador = "2nnn";
        opcode.assembly = "CALL "+opcode.address;

        stack[sp] = pc;
        sp++;
        pc = opcode.address;
    }

    private void ejecutar3XNN(){
        /* Documentacion: 3xkk - SE Vx, byte
        * Skip next instruction if Vx = kk.
        *
        * The interpreter compares register Vx to kk, and if they are equal, 
        * increments the program counter by 2.
        */
        opcode.identificador = "3xkk";
        opcode.assembly = "SE V"+opcode.vx+" "+opcode._byte;
        
        if(V[opcode.vx] == opcode._byte)
            pc += 4;
        else
            pc += 2;
    }

    private void ejecutar4XNN(){
        /* Documentacion: 4xkk - SNE Vx, byte
        * Skip next instruction if Vx != kk.
        *
        * The interpreter compares register Vx to kk, and if they are not 
        * equal, increments the program counter by 2.
        */
        opcode.identificador = "4xkk";
        opcode.assembly = "SNE V"+opcode.vx+" "+opcode._byte;
        
        if(V[opcode.vx] == opcode._byte)
            pc += 2;
        else
            pc += 4;
    }

    private void ejecutar5XY0(){
        /* Documentacion: 5xy0 - SE Vx, Vy
        * Skip next instruction if Vx = Vy.
        *
        * The interpreter compares register Vx to register Vy, and if they 
        * are equal, increments the program counter by 2.
        */
        opcode.identificador = "5xy0";
        opcode.assembly = "SE V"+opcode.vx+" V"+opcode.vy;
        
        if(V[opcode.vx] == V[opcode.vy])
            pc += 4;
        else
            pc += 2;
    }

    private void ejecutar6XNN(){
        /* Documentacion: 6xkk - LD Vx, byte
        * Set Vx = kk.
        *
        * The interpreter puts the value kk into register Vx.
        */
        opcode.identificador = "6xkk";
        opcode.assembly = "LD V"+opcode.vx+" "+opcode._byte;
        
        V[opcode.vx] = opcode._byte;
        pc += 2;
    }

    private void ejecutar7XNN(){
        /* Documentacion: 7xkk - ADD Vx, byte
        * Set Vx = Vx + kk.
        *
        * Adds the value kk to the value of register Vx, then stores the 
        *¨result in Vx. 
        */
        opcode.identificador = "7xkk";
        opcode.assembly = "ADD V"+opcode.vx+" "+opcode._byte;
        
        V[opcode.vx] += opcode._byte;
        pc += 2;
    }

    private void ejecutar8XY0(){
        /* Documentacion: 8xy0 - LD Vx, Vy
        * Set Vx = Vy.
        *
        * Stores the value of register Vy in register Vx.
        */
        opcode.identificador = "8xy0";
        opcode.assembly = "LD V"+opcode.vx+" V"+opcode.vy;
        
        V[opcode.vx] = V[opcode.vy];
        pc += 2;
    }

    private void ejecutar8XY1(){
        /* Documentacion: 8xy1 - OR Vx, Vy
        * Set Vx = Vx OR Vy.
        *
        * Performs a bitwise OR on the values of Vx and Vy, then stores the 
        * result in Vx. A bitwise OR compares the corrseponding bits from two 
        * values, and if either bit is 1, then the same bit in the result is 
        * also 1. Otherwise, it is 0. 
        */
        opcode.identificador = "8xy1";
        opcode.assembly = "OR V"+opcode.vx+" V"+opcode.vy;
        
        V[opcode.vx] |= V[opcode.vy];
        pc += 2;
    }

    private void ejecutar8XY2(){
        /* Documentacion: 8xy2 - AND Vx, Vy
        * Set Vx = Vx AND Vy.
        *
        * Performs a bitwise AND on the values of Vx and Vy, then stores the 
        * result in Vx. A bitwise AND compares the corrseponding bits from two 
        * values, and if both bits are 1, then the same bit in the result is 
        * also 1. Otherwise, it is 0.
        */
        opcode.identificador = "8xy2";
        opcode.assembly = "AND V"+opcode.vx+" V"+opcode.vy;
        
        V[opcode.vx] &= V[opcode.vy];
        pc += 2;
    }

    private void ejecutar8XY3(){
        /* Documentacion: 8xy3 - XOR Vx, Vy
        * Set Vx = Vx XOR Vy.
        *
        * Performs a bitwise exclusive OR on the values of Vx and Vy, then 
        * stores the result in Vx. An exclusive OR compares the corrseponding 
        * bits from two values, and if the bits are not both the same, then 
        * the corresponding bit in the result is set to 1. Otherwise, it is 0. 
        */
        opcode.identificador = "8xy3";
        opcode.assembly = "XOR V"+opcode.vx+" V"+opcode.vy;
        
        V[opcode.vx] ^= V[opcode.vy];
        pc += 2;
    }

    private void ejecutar8XY4(){
        /* Documentacion: 8xy4 - ADD Vx, Vy
        * Set Vx = Vx + Vy, set VF = carry.
        *
        * The values of Vx and Vy are added together. If the result is greater 
        * than 8 bits (i.e., > 255,) VF is set to 1, otherwise 0. 
        * Only the lowest 8 bits of the result are kept, and stored in Vx.
        */
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

    private void ejecutar8XY5(){
        /* Documentacion: 8xy5 - SUB Vx, Vy
        * Set Vx = Vx - Vy, set VF = NOT borrow.
        *
        * If Vx > Vy, then VF is set to 1, otherwise 0. Then Vy is subtracted 
        * from Vx, and the results stored in Vx.
        */
        opcode.identificador = "8xy5";
        opcode.assembly = "SUB V"+opcode.vx+" V"+opcode.vy;
        
        if(V[opcode.vy] > V[opcode.vx])
            V[0xF] = 0;
        else
            V[0xF] = 1;
        
        int resta = V[opcode.vx] - V[opcode.vy];
        V[opcode.vx] = resta & 0xFF;
        pc += 2;
    }

    private void ejecutar8XY6(){
        /* Documentacion: 8xy6 - SHR Vx {, Vy}
        * Set Vx = Vx SHR 1.
        * 
        * If the least-significant bit of Vx is 1, then VF is set to 1, 
        * otherwise 0. Then Vx is divided by 2.
        */
        opcode.identificador = "8xy6";
        opcode.assembly = "SHR V"+opcode.vx+" V"+opcode.vy;
        
        // El bit menos significante es el de la derecha
        V[0xF] = V[opcode.vx] & 0x1; // Mascara del ultimo bit
        
        // Dividir entre dos es un shift a la derecha de 1 (SHR 1)
        V[opcode.vx] >>= 1;
        pc += 2;        
    }

    private void ejecutar8XY7(){
        /* Documentacion: 8xy7 - SUBN Vx, Vy
        * Set Vx = Vy - Vx, set VF = NOT borrow.
        *
        * If Vy > Vx, then VF is set to 1, otherwise 0. Then Vx is subtracted 
        * from Vy, and the results stored in Vx.
        */
        opcode.identificador = "8xy7";
        opcode.assembly = "SUBN V"+opcode.vx+" V"+opcode.vy;
        
        if(V[opcode.vx] > V[opcode.vy])
            V[0xF] = 0;
        else
            V[0xF] = 1;
        
        int resta = V[opcode.vy] - V[opcode.vx];
        V[opcode.vx] = resta & 0xFF;
        pc += 2;
    }

    private void ejecutar8XYE(){
        /* Documentacion: 8xyE - SHL Vx {, Vy}
        * Set Vx = Vx SHL 1.
        *
        * If the most-significant bit of Vx is 1, then VF is set to 1, 
        * otherwise to 0. Then Vx is multiplied by 2.
        */
        opcode.identificador = "8xyE";
        opcode.assembly = "SHL V"+opcode.vx+" V"+opcode.vy;
        
        // El bit mas significativo es el de la izquierda
        V[0xF] = V[opcode.vx] >> 7; // El shift solo deja al bit necesario
        
        // Una multiplicacion por dos equivale a un shift izq de 1(SHL 1)
        V[opcode.vx] <<= 1;
        pc += 2;
    }

    private void ejecutar9XY0(){
        /* Documentacion: 9xy0 - SNE Vx, Vy
        * Skip next instruction if Vx != Vy.
        *
        * The values of Vx and Vy are compared, and if they are not equal, 
        * the program counter is increased by 2.
        */
        opcode.identificador = "9xy0";
        opcode.assembly = "SNE V"+opcode.vx+" V"+opcode.vy;
        
        if(V[opcode.vx] != V[opcode.vy])
            pc += 4;
        else
            pc += 2;
    }

    private void ejecutarANNN(){
        /* Documentacion: Annn - LD I, addr
        * Set I = nnn.
        *
        * The value of register I is set to nnn.
        */
        opcode.identificador = "Annn";
        opcode.assembly = "LD I "+opcode.address;
        
        I = opcode.address;
        pc += 2;
    }

    private void ejecutarBNNN(){
        /* Documentacion: Bnnn - JP V0, addr
        * Jump to location nnn + V0.
        *
        * The program counter is set to nnn plus the value of V0.
        */
        opcode.identificador = "Bnnn";
        opcode.assembly = "JP V0 "+opcode.address;
        
        pc = opcode.address + V[0];
    }

    private void ejecutarCXNN(){
        /* Documentacion: Cxkk - RND Vx, byte
        * Set Vx = random byte AND kk.
        *
        * The interpreter generates a random number from 0 to 255, which is 
        * then ANDed with the value kk. The results are stored in Vx. See 
        * instruction 8xy2 for more information on AND.
        */
        opcode.identificador = "Cxkk";
        opcode.assembly = "RND V"+opcode.vx+" "+opcode._byte;
        
        // Numero random con valores de 0 - 255
        int n_rand = rand.nextInt(256);
        V[opcode.vx] = opcode._byte & n_rand ;
        
        pc += 2;
    }

    private void ejecutarDXYN(){
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
        opcode.assembly = "DRW V"+opcode.vx+" V"+opcode.vy+" "+opcode.nibble;
        
        int pixel;
        
        V[0xF] = 0;
        // Loop de las filas del sprite
        for(int y=0; y < opcode.nibble; y++){
            pixel = memory[I+y];
            
            // Loop de los 8 bits de la fila
            for(int x=0; x < 8; x++){
                // Se comprueba si el bit actual se debera de pintar
                // Se usa 0x80 (1000 0000 en binario) para ir recorriendo los
                // bit de la fila del sprite
                if((pixel & (0x80 >> x)) != 0){
                    // Si el pixel a pintar ya esta activo, se asigna 1 al
                    // registro VF, normalmente usado para detectar colision
                    if(gfx[(opcode.vx + x +((opcode.vy + y) * 64))] == 1){
                        V[0xF] = 1;
                    }
                    
                    // El nuevo valor en pantalla se define con una 
                    // operacion XOR
                    gfx[opcode.vx + x + ((opcode.vy + y) * 64)] ^= 1;
                }
            }
            
        }
        
        // La pantalla se marca para una actualizacion
        drawFlag = true;
        pc += 2;
    }

    private void ejecutarEX9E(){
        /* Documentacion: Ex9E - SKP Vx
        * Skip next instruction if key with the value of Vx is pressed.
        *
        * Checks the keyboard, and if the key corresponding to the value of 
        * Vx is currently in the down position, PC is increased by 2.
        */
        opcode.identificador = "Ex9E";
        opcode.assembly = "SKP V"+opcode.vx;
        
        // Si la tecla almacenada en Vx esta presionada, se salta
        // la siguiente instruccion
        if(key[V[opcode.vx]] != 0)
            pc += 4;
        else
            pc += 2;
    }

    private void ejecutarEXA1(){
        /* Documentacion: ExA1 - SKNP Vx
        * Skip next instruction if key with the value of Vx is not pressed.
        *
        * Checks the keyboard, and if the key corresponding to the value of Vx 
        * is currently in the up position, PC is increased by 2.
        */
        opcode.identificador = "ExA1";
        opcode.assembly = "SKNP V"+opcode.vx;
        
        // Lo inverso a EX9E
        if(key[V[opcode.vx]] != 0)
            pc += 2;
        else
            pc += 4;
    }

    private void ejecutarFX07(){
        /* Documentacion: Fx07 - LD Vx, DT
        * Set Vx = delay timer value.
        *
        * The value of DT is placed into Vx.*/
        opcode.identificador = "Fx07";
        opcode.assembly = "LD V"+opcode.vx+" DT";
        
        V[opcode.vx] = delayTimer;
        pc += 2;
    }

    private void ejecutarFX0A(){
        /* Documentacion: Fx0A - LD Vx, K
        * Wait for a key press, store the value of the key in Vx.
        *
        * All execution stops until a key is pressed, then the value of that 
        * key is stored in Vx.
        */
        opcode.identificador = "Fx0A";
        opcode.assembly = "LD V"+opcode.vx+" K";
        
        boolean keyPressed = false;
        
        for(int i: key){
            if(i != 0){
                V[opcode.vx] = i;
                keyPressed = true;
                break;
            }
        }
        
        // Si no se encontro una tecla presionada, se termina la ejecucion
        // y se intenta otravez.
        if(!keyPressed) return;
        
        pc += 2;        
    }

    private void ejecutarFX15(){
        /* Documentacion: Fx15 - LD DT, Vx
        * Set delay timer = Vx.
        *
        * DT is set equal to the value of Vx.
        */
        opcode.identificador = "Fx15";
        opcode.assembly = "LD DT V"+opcode.vx;
        
        delayTimer = V[opcode.vx];
        pc += 2;
    }

    private void ejecutarFX18(){
        /* Documentacion: Fx18 - LD ST, Vx
        * Set sound timer = Vx.
        *
        * ST is set equal to the value of Vx.
        */
        opcode.identificador = "Fx18";
        opcode.assembly = "LD ST V"+opcode.vx;
        
        soundTimer = V[opcode.vx];
        pc += 2;       
    }

    private void ejecutarFX1E(){
        /* Documentacion: Fx1E - ADD I, Vx
        * Set I = I + Vx.
        *
        * The values of I and Vx are added, and the results are stored in I.
        */
        opcode.identificador = "Fx1E";
        opcode.assembly = "ADD I, V"+opcode.vx;
        
        // Se coloca una bandera en VF si en I existe un range overflow
        if((I + V[opcode.vx]) > 0xFFF)
            V[0xF] = 1;
        else
            V[0xF] = 0;        
        
        I += V[opcode.vx];
        pc += 2;
    }

    private void ejecutarFX29(){
        /* Documentacion: Fx29 - LD F, Vx
        * Set I = location of sprite for digit Vx.
        *
        * The value of I is set to the location for the hexadecimal sprite 
        * corresponding to the value of Vx. See section 2.4, Display, for more 
        * information on the Chip-8 hexadecimal font.
        */
        opcode.identificador = "Fx29";
        opcode.assembly = "LD F V"+opcode.vx;
        
        I = V[opcode.vx] * 0x5;
        pc += 2;
    }

    private void ejecutarFX33(){
        /* Documentacion: Fx33 - LD B, Vx
        * Store BCD representation of Vx in memory locations I, I+1, and I+2.
        *
        * The interpreter takes the decimal value of Vx, and places the 
        * hundreds digit in memory at location in I, the tens digit at 
        * location I+1, and the ones digit at location I+2.
        */
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

    private void ejecutarFX55(){
        /* Documentacion: Fx55 - LD [I], Vx
        * Store registers V0 through Vx in memory starting at location I.
        *
        * The interpreter copies the values of registers V0 through Vx into 
        * memory, starting at the address in I.
        */
        opcode.identificador = "Fx55";
        opcode.assembly = "LD I V"+opcode.vx;
        
        for(int i=0; i<V[opcode.vx]; i++){
            memory[I + i] = V[i]; 
        }
        
        // El interprete original del Chip-8, tras terminar esta operacion,
        // se asigna I = I + X + 1 (Queda sujeto a pruebas)
        I += V[opcode.vx]+1;
        
        pc += 2;
    }

    private void ejecutarFX65(){
        /* Documentacion: Fx65 - LD Vx, [I]
        * Read registers V0 through Vx from memory starting at location I.
        *
        * The interpreter reads values from memory starting at location I 
        * into registers V0 through Vx.
        */
        opcode.identificador = "Fx65";
        opcode.assembly = "LD V"+opcode.vx+" I";
        
        for(int i=0; i<V[opcode.vx]; i++){
            V[i] = memory[I + i]; 
        }
        
        // El interprete original del Chip-8, tras terminar esta operacion,
        // se asigna I = I + X + 1 (Queda sujeto a pruebas)
        I += V[opcode.vx]+1;
        
        pc += 2;
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
