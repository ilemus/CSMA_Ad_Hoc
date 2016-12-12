import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

public class RunThreadUI extends JFrame {
    /**
     * UI used to draw the sending of a packet (path of)
     */
    private static final long serialVersionUID = 4775029820775955579L;
    private File mFile;
    private BufferedImage image = null;
    private ArrayList<Vehicle> mVehicles;
    private boolean updateFrame = false;
    
    private final JPanel panel = new JPanel() {
        /**
         * Main panel used in drawing
         */
        private static final long serialVersionUID = -8769322900441975361L;

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(image, 0, 0, null);
            
            // Draw each of the points with color (set by selected Vehicle)
            for (Vehicle c: mVehicles) {
                g.setColor(c.getColor());
                g.fillOval(c.getGPS().getLat(), c.getGPS().getLong(), 5, 5);
            }
        }
    };
    
    public RunThreadUI(File fp, ArrayList<Vehicle> vehicles) {
        mFile = fp;
        mVehicles = vehicles;
        
        try {
            image = ImageIO.read(mFile);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(image.getWidth(), image.getHeight());
        setVisible(true);
        
        RunThread thread = new RunThread();
        thread.start();
        add(panel);
        updateFrame();
    }
    
    public void updateFrame() {
        updateFrame = true;
    }
    
    private class RunThread extends Thread {
        @Override
        public void run() {
            while (true) {
                if (updateFrame) {
                    // Only update the JPanel when requested
                    updateFrame = false;
                    panel.revalidate();
                    panel.repaint();
                } else {
                    // Otherwise wait for update
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
