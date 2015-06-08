package chip8.git;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Luis Mario
 */
public class MainFrame extends Thread {
    private Chip8 chip8;
    private ChipFrame frame;
	
    public MainFrame() throws IOException {
        chip8 = new Chip8();
        chip8.init();
        chip8.cargarJuego("MISSILE");
        frame = new ChipFrame(chip8);
    }
    
    @Override
    public void run() {
        //60 hz, 60 updates per second
        while(true) {
            chip8.setKeyPad(frame.getKeyBuffer());
            chip8.emularCiclo();
            if(chip8.drawFlag) {
                frame.repaint();
                chip8.drawFlag = false;
            }
            try {
                Thread.sleep(4);
            } catch (InterruptedException e) {
		//Unthrown exception
            }
        }
    }
    public static void main(String[] args) {
	MainFrame main;
        try {
            main = new MainFrame();
            main.start();
        } catch (IOException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
        }	
    }
}
