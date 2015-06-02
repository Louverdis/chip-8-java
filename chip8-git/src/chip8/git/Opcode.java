package chip8.git;

/**
 *
 * @author Luis Mario
 */
public class Opcode {
    
    /******************************
    * Estructura del Opcode:
    *  NNN: address
    *  KK:  8-bit constant (byte)
    *  N:   4-bit constant (nibble)
    *  X,Y: 4-bit register identifier
    *************************************/
    
    // Componente NNN
    public int address;
    
    // Componente KK
    public int _byte;
    
    // Componente N
    public int nibble;
    
    // Componente X
    public int vx;
    
    // Componente Y
    public int vy;
    
    // Atributos para representacion en texto
    public String identificador;
    public String assembly;
    
    public Opcode(int nnn, int kk, int n, int x, int y, String id, String asm){
        address = nnn;
        _byte   = kk;
        nibble  = n;
        
        vx = x;
        vy = y;
        
        identificador = id;
        assembly = asm;
    }
    
    @Override
    public String toString(){
        return assembly;
    }
}
