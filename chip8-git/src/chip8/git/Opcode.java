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

    // Cabeza identificadora del opcode
    public int header;

    // Componente NNN
    public int address;

    // Componente KK
    public int _byte;

    // Componente N
    public int nibble;

    // Componente X
    public int vx;
    iOpcode & 0x00FF;
    // Componente Y
    public int vy;

    // Atributos para representacion en texto
    public String identificador;
    public String assembly;

    /*
    * Constructor default, desglosa los componentes del opcode apartir de
    * representacion numerica.
    */
    public Opcode(int iOpcode){
        header  = (iOpcode & 0xF000) >> 12;
        address = iOpcode & 0x0FFF;
        _byte   = iOpcode & 0x00FF;
        nibble  = iOpcode & 0x000F;

        vx = (iOpcode & 0x0F00) >> 8;
        vx = (iOpcode & 0x00F0) >> 4;

        identificador = "";
        assembly = "";

    }

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
