import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import javax.imageio.ImageIO;

public class RunThread {
    protected ArrayList<Vehicle> aVehicle = new ArrayList<Vehicle>();
    private int[][] mMap;
    private Random mRandom = new Random();
    private RunThreadUI mUI;
    
    public void startAlgorithm(File map) {
        InitialThread mainThread = new InitialThread(map);
        mainThread.start();
    }
    
    private class InitialThread extends Thread {
        private File map;
        private int SURROUND = 0xFFFFFF; // WHITE
        private final long SEED = 0xF0AB87; // Arbitrary random seed
        private final int NUMBER_VEHICLES = 200;
        private int WIDTH;
        private int HEIGHT;
        private int TOTAL_POS = 0;
        
        public InitialThread(File map) {
            this.map = map;
        }
        
        @Override
        public void run() {
            int count = 0;
            int pos;
            
            try {
                BufferedImage bImg = ImageIO.read(map);
                mMap = new int[bImg.getWidth()][bImg.getHeight()];
                WIDTH = bImg.getWidth();
                HEIGHT = bImg.getHeight();
                
                for (int i = 0; i < WIDTH; i++) {
                    for (int j = 0; j < HEIGHT; j++) {
                        if (bImg.getRGB(i, j) < SURROUND) {
                            mMap[i][j] = 1;
                            TOTAL_POS++;
                        } else {
                            mMap[i][j] = 0;
                        }
                    }
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            mRandom.setSeed(SEED);
            
            // It is possible for a location to have multiple cars
            for (int i = 0; i < NUMBER_VEHICLES; i++) {
                pos = mRandom.nextInt(TOTAL_POS);
                
                for (int j = 0; j < WIDTH; j++) {
                    for (int k = 0; k < HEIGHT; k++) {
                        if (count == pos) {
                            GPS gps = new GPS(j, k);
                            Vehicle temp = new Vehicle(gps);
                            aVehicle.add(temp);
                            count = 0;
                        } else {
                            count++;
                        }
                    }
                }
            }
            
            mUI = new RunThreadUI(map, aVehicle);
            
            // Possibility that destination and source are same location
            int source = mRandom.nextInt(NUMBER_VEHICLES);
            int dest = mRandom.nextInt(NUMBER_VEHICLES);
            
            aVehicle.get(dest).setColor(Color.RED);
            
            // Start the first sending to target (vehicle of random location)
            aVehicle.get(source).initialTransact(aVehicle.get(dest).getGPS());
        }
    }
    
    protected class GPS {
        private int lat_x, long_y;
        
        public GPS(int lat_x, int long_y) {
            this.lat_x = lat_x;
            this.long_y = long_y;
        }
        
        public int getLat() {
            return lat_x;
        }
        
        public int getLong() {
            return long_y;
        }
    }
}
