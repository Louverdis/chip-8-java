package chip8;

/**
 * Creado por luismario
 * Fecha: 15/07/15.
 */

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;

public class MainFrame {
    public Chip8 chip8;
    public ChipFrame frame;

    public MainFrame(String archivo) throws IOException {
        chip8 = new Chip8(false);
        chip8.init();
        chip8.cargarJuego(archivo);
        frame = new ChipFrame(chip8);
    }

    public static void main(String[] args) {
        MainFrame mainFrame;

        try {
            //mainFrame = new MainFrame(args[0]);
            mainFrame = new MainFrame("invaders.c8");

            //Ciclo de reloj objetivo: ~500hz / ~1000hz (super chip8)
            ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

            service.scheduleAtFixedRate(() -> {
                mainFrame.chip8.setKeyPad(mainFrame.frame.getKeyBuffer());
                mainFrame.chip8.emularCiclo();
                if(mainFrame.chip8.drawFlag) {
                    mainFrame.frame.repaint();
                    mainFrame.chip8.drawFlag = false;
                }
            }, 0, mainFrame.chip8.period, TimeUnit.MILLISECONDS);

        } catch (IOException ex) {
            Logger.getLogger(MainFrame.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
    }
}
