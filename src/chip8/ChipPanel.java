package chip8;

/**
 * Creado por luismario
 * Fecha: 15/07/15.
 */
import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JPanel;

public class ChipPanel extends JPanel {
    private final Chip8 chip;

    public ChipPanel(Chip8 chip) {
        this.chip = chip;
    }

    @Override
    public void paint(Graphics g) {
        for(int i = 0; i < chip.gfx.length; i++) {
            if(chip.gfx[i] == 0)
                g.setColor(Color.BLACK);
            else
                g.setColor(Color.WHITE);

            int x = (i % 64);
            int y = (int)Math.floor(i / 64);

            g.fillRect(x * 10, y * 10, 10, 10);
        }
    }
}
