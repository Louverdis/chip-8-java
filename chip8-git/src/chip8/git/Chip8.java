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
        for (int i: memory)
            i = 0; // Pendiente cargar fontset

        for (int i: V)
            i = 0;

        for (int i: gfx)
            i = 0;

        for (int i: stack)
            i = 0;

        I = 0;
        opcode = 0;
        pc = 0x200; // Los programas en el Chip-8 inician en esta direccion
        delayTimer = 0;
        soundTimer = 0;
        sp = 0;
        drawFlag = false;
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

        // Desifrar opcode
        //opcode = descifrarOpcode(i_opcode);
        switch (i_opcode & 0xF000) {
            case 0x0000:
                // Existen dos Opcodes que empiezan con 0x00
                switch (i_opcode & 0x000F) {
                    case 0x0000:
                        // 00E0 - CLS
                        break;

                    case 0x000E:
                        // 00EE - RET
                        break;

                    case default:
                        System.out.println(
                            "Opcode desconocido: "+
                            String.format("0x%04X", i_opcode)
                        );
                        break;
                }
                break;

            case 0x1000:
                // 1nnn - JP addr
                break;

            case 0x2000:
                // 2nnn - CALL addr
                break;

            case 0x3000:
                // 3xkk - SE Vx, byte
                break;

            case 0x4000:
                // 4xkk - SNE Vx, byte
                break;

            case 0x5000:
                // 5xy0 - SE Vx, Vy
                break;

            case 0x6000:
                // 6xkk - LD Vx, byte
                break;

            case 0x7000:
                // 7xkk - ADD Vx, byte
                break;

            case 0x8000:
                // Existen 9 opcodes que empiezan con 0x8xy
                switch (i_opcode & 0x000F) {
                    case 0x0000:
                        // 8xy0 - LD Vx, Vy
                        break;

                    case 0x0001:
                        // 8xy1 - OR Vx, Vy
                        break;

                    case 0x0002:
                        // 8xy2 - AND Vx, Vy
                        break;

                    case 0x0003:
                        // 8xy3 - XOR Vx, Vy
                        break;

                    case 0x0004:
                        // 8xy4 - ADD Vx, Vy
                        break;

                    case 0x0005:
                        // 8xy5 - SUB Vx, Vy
                        break;

                    case 0x0006:
                        // 8xy6 - SHR Vx {, Vy}
                        break;

                    case 0x0007:
                        // 8xy7 - SUBN Vx, Vy
                        break;

                    case 0x000E:
                        // 8xyE - SHL Vx {, Vy}
                        break;

                    case default:
                        System.out.println(
                            "Opcode desconocido: "+
                            String.format("0x%04X", i_opcode)
                        );
                        break;
                }
                break;

            case 0x9000:
                // 9xy0 - SNE Vx, Vy
                break;

            case 0xA000:
                // Annn - LD I, addr
                break;

            case 0xB000:
                // Bnnn - JP V0, addr
                break;

            case 0xC000:
                // Cxkk - RND Vx, byte
                break;

            case 0xD000:
                // Dxyn - DRW Vx, Vy, nibble
                break;

            case 0xE000:
                // Existen 2 opcodes que empiezan con 0xEx
                switch (i_opcode & 0x00FF) {
                    case 0x009E:
                        // Ex9E - SKP Vx
                        break;

                    case 0x00A1:
                        // ExA1 - SKNP Vx
                        break;

                    case default:
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
                        //
                        break;

                    case 0x000A:
                        //
                        break;

                    case 0x0015:
                        //
                        break;

                    case 0x0018:
                        //
                        break;

                    case 0x001E:
                        //
                        break;

                    case 0x0029:
                        //
                        break;

                    case 0x0033:
                        //
                        break;

                    case 0x0055:
                        //
                        break;

                    case 0x0065:
                        //
                        break;

                    case default:
                        System.out.println(
                            "Opcode desconocido: "+
                            String.format("0x%04X", i_opcode)
                        );
                        break;

                }
                break;

            case default:
                System.out.println(
                    "Opcode desconocido: "+
                    String.format("0x%04X", i_opcode)
                );
                break;
        }

        // Ejecutar opcode
        //ejecutarOpcode(opcode);

        // Tras emular un ciclo, el pc debe moverse dos espacios
        // pues cada ciclo consume dos bytes.
        pc += 2;

    }

    private Opcode descifrarOpcode(int op){return null;}

    private void ejecutarOpcode(Opcode opcode){}

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
