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
    *   sitios: http://en.wikipedia.org/wiki/CHIP-8
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
        
    }
    
    /* Funcion DEBUGG */
    public void imprimirMemoria(){
        for(int i: memory){
            System.out.print(String.format("%02X ", i));
        }
    }
}
