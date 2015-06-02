package chip8.git;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

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
    private int pc;

    private int opcode;

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
        
        pc = 0x200; // Los programas en el Chip-8 inician en esta direccion
        I = 0;      // Reset del indice
        opcode = 0; // Reset del opcode actual
        sp = 0;     // Reset del stack pointer
        
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
        if (i_opcode == 0){
            pc += 2;
            return;
        }

        // Variable temporal para armar la representacion en string del
        // opcode traducido a assembly
        String assembly = "NONE";

        // Desifrar opcode
        //opcode = descifrarOpcode(i_opcode);

        /******************************
        * Estructura del Opcode:
        *  NNN: address
        *  KK:  8-bit constant (byte)
        *  N:   4-bit constant (nibble)
        *  X,Y: 4-bit register identifier
        *************************************/

        // Direccion, byte y nibble
        int nnn, kk, n;
        // identificadores de registros
        int x, y;

        switch (i_opcode & 0xF000) {
            case 0x0000:
                // Existen dos Opcodes que empiezan con 0x00
                switch (i_opcode & 0x000F) {
                    case 0x0000:
                        // 00E0 - CLS
                        nnn = kk = n = x = y = 0;
                        assembly = "CLS";
                        break;

                    case 0x000E:
                        // 00EE - RET
                        nnn = kk = n = x = y = 0;
                        assembly = "RET";
                        break;

                    default:
                        System.out.println(
                            "Opcode desconocido: "+
                            String.format("0x%04X", i_opcode)
                        );
                        break;
                }
                break;

            case 0x1000:
                // 1nnn - JP addr
                kk = n = x = y = 0;
                nnn = i_opcode & 0x0FFF;
                assembly = "JP "+nnn;
                break;

            case 0x2000:
                // 2nnn - CALL addr
                kk = n = x = y = 0;
                nnn = i_opcode & 0x0FFF;
                assembly = "CALL "+nnn;
                break;

            case 0x3000:
                // 3xkk - SE Vx, byte
                nnn = n = y = 0;

                x = (i_opcode & 0x0F00) >> 8;
                kk = i_opcode & 0x00FF;

                assembly = "SE V"+x+" "+kk;
                break;

            case 0x4000:
                // 4xkk - SNE Vx, byte
                nnn = n = y = 0;

                x = (i_opcode & 0x0F00) >> 8;
                kk = i_opcode & 0x00FF;

                assembly = "SNE V"+x+" "+kk;
                break;

            case 0x5000:
                // 5xy0 - SE Vx, Vy
                nnn = kk = n = 0;

                x = (i_opcode & 0x0F00) >> 8;
                y = (i_opcode & 0x00F0) >> 4;

                assembly = "SE V"+x+" V"+y;
                break;

            case 0x6000:
                // 6xkk - LD Vx, byte
                nnn = n = y = 0;

                x = (i_opcode & 0x0F00) >> 8;
                kk = i_opcode & 0x00FF;

                assembly = "LD V"+x+" "+kk;
                break;

            case 0x7000:
                // 7xkk - ADD Vx, byte
                nnn = n = y = 0;

                x = (i_opcode & 0x0F00) >> 8;
                kk = i_opcode & 0x00FF;

                assembly = "ADD V"+x+" "+kk;
                break;

            case 0x8000:
                // Existen 9 opcodes que empiezan con 0x8xy
                switch (i_opcode & 0x000F) {
                    case 0x0000:
                        // 8xy0 - LD Vx, Vy
                        nnn = kk = n = 0;

                        x = (i_opcode & 0x0F00) >> 8;
                        y = (i_opcode & 0x00F0) >> 4;

                        assembly = "LD V"+x+" V"+y;
                        break;

                    case 0x0001:
                        // 8xy1 - OR Vx, Vy
                        nnn = kk = n = 0;

                        x = (i_opcode & 0x0F00) >> 8;
                        y = (i_opcode & 0x00F0) >> 4;

                        assembly = "OR V"+x+" V"+y;
                        break;

                    case 0x0002:
                        // 8xy2 - AND Vx, Vy
                        nnn = kk = n = 0;

                        x = (i_opcode & 0x0F00) >> 8;
                        y = (i_opcode & 0x00F0) >> 4;

                        assembly = "AND V"+x+" V"+y;
                        break;

                    case 0x0003:
                        // 8xy3 - XOR Vx, Vy
                        nnn = kk = n = 0;

                        x = (i_opcode & 0x0F00) >> 8;
                        y = (i_opcode & 0x00F0) >> 4;

                        assembly = "XOR V"+x+" V"+y;
                        break;

                    case 0x0004:
                        // 8xy4 - ADD Vx, Vy
                        nnn = kk = n = 0;

                        x = (i_opcode & 0x0F00) >> 8;
                        y = (i_opcode & 0x00F0) >> 4;

                        assembly = "ADD V"+x+" V"+y;
                        break;

                    case 0x0005:
                        // 8xy5 - SUB Vx, Vy
                        nnn = kk = n = 0;

                        x = (i_opcode & 0x0F00) >> 8;
                        y = (i_opcode & 0x00F0) >> 4;

                        assembly = "SUB V"+x+" V"+y;
                        break;

                    case 0x0006:
                        // 8xy6 - SHR Vx {, Vy}
                        nnn = kk = n = 0;

                        x = (i_opcode & 0x0F00) >> 8;
                        y = (i_opcode & 0x00F0) >> 4;

                        assembly = "SHR V"+x+" V"+y;
                        break;

                    case 0x0007:
                        // 8xy7 - SUBN Vx, Vy
                        nnn = kk = n = 0;

                        x = (i_opcode & 0x0F00) >> 8;
                        y = (i_opcode & 0x00F0) >> 4;

                        assembly = "SUBN V"+x+" V"+y;
                        break;

                    case 0x000E:
                        // 8xyE - SHL Vx {, Vy}
                        nnn = kk = n = 0;

                        x = (i_opcode & 0x0F00) >> 8;
                        y = (i_opcode & 0x00F0) >> 4;

                        assembly = "SHL V"+x+" V"+y;
                        break;

                    default:
                        System.out.println(
                            "Opcode desconocido: "+
                            String.format("0x%04X", i_opcode)
                        );
                        break;
                }
                break;

            case 0x9000:
                // 9xy0 - SNE Vx, Vy
                nnn = kk = n = 0;

                x = (i_opcode & 0x0F00) >> 8;
                y = (i_opcode & 0x00F0) >> 4;

                assembly = "SNE V"+x+" V"+y;
                break;

            case 0xA000:
                // Annn - LD I, addr
                kk = n = x = y = 0;

                nnn = i_opcode & 0x0FFF;

                assembly = "LD I "+nnn;
                break;

            case 0xB000:
                // Bnnn - JP V0, addr
                kk = n = x = y = 0;

                nnn = i_opcode & 0x0FFF;

                assembly = "JP V0 "+nnn;
                break;

            case 0xC000:
                // Cxkk - RND Vx, byte
                nnn = n = y = 0;

                x = (i_opcode & 0x0F00) >> 8;
                kk = i_opcode & 0x00FF;

                assembly = "RND V"+x+" "+kk;
                break;

            case 0xD000:
                // Dxyn - DRW Vx, Vy, nibble
                nnn = kk = 0;

                x = (i_opcode & 0x0F00) >> 8;
                y = (i_opcode & 0x00F0) >> 4;
                n = i_opcode & 0x000F;

                assembly = "DRW V"+x+" V"+y+" "+n;
                break;

            case 0xE000:
                // Existen 2 opcodes que empiezan con 0xEx
                switch (i_opcode & 0x00FF) {
                    case 0x009E:
                        // Ex9E - SKP Vx
                        nnn = kk = n = y = 0;

                        x = (i_opcode & 0x0F00) >> 8;

                        assembly = "SKP V"+x;
                        break;

                    case 0x00A1:
                        // ExA1 - SKNP Vx
                        nnn = kk = n = y = 0;

                        x = (i_opcode & 0x0F00) >> 8;

                        assembly = "SKNP V"+x;
                        break;

                    default:
                        System.out.println(
                            "Opcode desconocido: "+
                            String.format("0x%04X", i_opcode)
                        );
                        break;
                }
                break;

            case 0xF000:
                // Existen 9 opcodes que empiezan con 0xFx
                switch (i_opcode & 0x00FF) {
                    case 0x0007:
                        // Fx07 - LD Vx, DT
                        nnn = kk = n = y = 0;

                        x = (i_opcode & 0x0F00) >> 8;

                        assembly = "LD V"+x+" DT";
                        break;

                    case 0x000A:
                        // Fx0A - LD Vx, K
                        nnn = kk = n = y = 0;

                        x = (i_opcode & 0x0F00) >> 8;

                        assembly = "LD V"+x+" K";
                        break;

                    case 0x0015:
                        // Fx15 - LD DT, Vx
                        nnn = kk = n = y = 0;

                        x = (i_opcode & 0x0F00) >> 8;

                        assembly = "LD DT V"+x;
                        break;

                    case 0x0018:
                        // Fx18 - LD ST, Vx
                        nnn = kk = n = y = 0;

                        x = (i_opcode & 0x0F00) >> 8;

                        assembly = "LD ST V"+x;
                        break;

                    case 0x001E:
                        // Fx1E - ADD I, Vx
                        nnn = kk = n = y = 0;

                        x = (i_opcode & 0x0F00) >> 8;

                        assembly = "ADD I, V"+x;
                        break;

                    case 0x0029:
                        // Fx29 - LD F, Vx
                        nnn = kk = n = y = 0;

                        x = (i_opcode & 0x0F00) >> 8;

                        assembly = "LD F V"+x;
                        break;

                    case 0x0033:
                        // Fx33 - LD B, Vx
                        nnn = kk = n = y = 0;

                        x = (i_opcode & 0x0F00) >> 8;

                        assembly = "LD B V"+x;
                        break;

                    case 0x0055:
                        // Fx55 - LD [I], Vx
                        nnn = kk = n = y = 0;

                        x = (i_opcode & 0x0F00) >> 8;

                        assembly = "LD I V"+x;
                        break;

                    case 0x0065:
                        // Fx65 - LD Vx, [I]
                        nnn = kk = n = y = 0;

                        x = (i_opcode & 0x0F00) >> 8;

                        assembly = "LD V"+x+" I";
                        break;

                    default:
                        System.out.println(
                            "Opcode desconocido: "+
                            String.format("0x%04X", i_opcode)
                        );
                        break;

                }
                break;

            default:
                System.out.println(
                    "Opcode desconocido: "+
                    String.format("0x%04X", i_opcode)
                );
                break;
        }

        // Ejecutar opcode
        //ejecutarOpcode(opcode);
        //System.out.println(
        //    String.fomat("Instruccion en 0x%04X: %s", pc, assembly)
        //);
        System.out.printf("Instruccion en 0x%04X: %s\n", pc, assembly);

        // Tras emular un ciclo, el pc debe moverse dos espacios
        // pues cada ciclo consume dos bytes.
        pc += 2;

    }

    public int get_pc(){
        return pc;
    }

    //private Opcode descifrarOpcode(int op){return null;}

    //private void ejecutarOpcode(Opcode opcode){}

    /* Funcion DEBUGG */
    public void imprimirMemoriaRaw(){
        for(int i: memory){
            System.out.print(String.format("%02X ", i));
        }
    }

    /* Funcion DEBUGG 2 */
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
}
