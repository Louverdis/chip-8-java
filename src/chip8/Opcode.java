package chip8;

/**
 * Creado por Luis Mario Reyes Moreno
 * Fecha: 15/07/15.
 */

public class Opcode {
    /******************************
     * Estructura del Opcode:
     *  NNN: address
     *  KK:  8-bit constant (byte)
     *  N:   4-bit constant (nibble)
     *  X,Y: 4-bit register identifier
     *************************************/

    // Forma original del opcode
    public int hex_opcode;

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

    // Componente Y
    public int vy;

    // Atributos para representacion en texto
    public String identificador;
    public String assembly;

    /*
    * Constructor default, desglosa los componentes del opcode apartir de una
    * representacion numerica.
    */
    public Opcode(int opcode){
        hex_opcode = opcode;
        header  = (opcode & 0xF000) >> 12;
        address = opcode & 0x0FFF;
        _byte   = opcode & 0x00FF;
        nibble  = opcode & 0x000F;

        vx = (opcode & 0x0F00) >> 8;
        vy = (opcode & 0x00F0) >> 4;

        identificador = "";
        assembly = "";

    }

    @Override
    public String toString(){
        return assembly;
    }
}
