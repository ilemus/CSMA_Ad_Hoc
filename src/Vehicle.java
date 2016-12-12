import java.awt.Color;
import java.util.ArrayList;
import java.util.Random;

public class Vehicle extends RunThread {
    private GPS mCoord;
    private GPS sCoord;
    private GPS dCoord;
    private WorkThread mThread = null;
    private final double ANGLE_DELTA = 30;
    private Module mMod;
    private int num_slots;
    private boolean interrupt = false;
    private final int SIFS = 10;
    private final int SLOT = 20;
    private final int DIFS = 50;
    private final int MEGA_BYTE = 1000 * 1000;
    private final double FREQ = 3.375 * MEGA_BYTE; // 12MB/s
    private Vehicle oVehicle;
    private Vehicle mVehicle;
    private Vehicle sVehicle;
    private SendThread mSend = null;
    private Color mColor = Color.BLACK;
    
    public Vehicle(GPS coord) {
        mCoord = coord;
        mMod = new Module();
        mVehicle = this;
    }
    
    public GPS getGPS() {
        return mCoord;
    }
    
    public Color getColor() {
        return mColor;
    }
    
    /**
     * Input source location, and current/destination location
     * Then returns distance between two locations
     * @param sourcex
     * @param sourcey
     * @param destx
     * @param desty
     * @return
     */
    private float getDistance(int sourcex, int sourcey, int destx, int desty) {
        return (float) Math.sqrt(Math.pow(sourcex - destx, 2.0)
                + Math.pow(sourcey - desty, 2.0));
    }
    
    private class Module {
        private Random mRand = new Random();
        private int mExp = 0;
        private final int MAX_EXP = 10;
        
        public int getNewContention() {
            return mRand.nextInt((int) Math.pow(2.0, (double) mExp));
        }
        
        public void occurCollision() {
            // Exponential expansion
            if (mExp < MAX_EXP) {
                mExp++;
            }
        }
    }
    
    public void ping(GPS source, GPS dest, Vehicle v) {
        sCoord = source;
        dCoord = dest;
        oVehicle = v;
        
        /** If there is an existing request to send
         * Then ignore the request, start a new one
        */
        mThread = new WorkThread();
        mThread.start();
    }
    
    public void setColor(Color c) {
        mColor = c;
    }
    
    public void occurCollision() {
        interrupt = true;
        mMod.occurCollision();
        mVehicle.notify();
    }
    
    public void requestToReceive(Vehicle v) {
        
        if (mSend == null) {
            mSend = new SendThread();
            mSend.start();
            sVehicle = v;
        } else {
            v.occurCollision();
            sVehicle.occurCollision();
            mSend.interrupt();
            mSend = null;
        }
    }
    
    private void sleepFor(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            // Occur collision or other
            System.out.println("Thread has been interrupted");
        }
    }
    
    private long byteDuration(int bytes) {
        long value;
        double TO_MICRO_SEC = 1000000.0;
        value = (long) (bytes * TO_MICRO_SEC / FREQ);
        return value;
    }
    
    public void clearToReceive() {
        mVehicle.notify();
    }
    
    public void endSend() {
        mVehicle.notifyAll();
    }
    
    private class SendThread extends Thread {
        @Override
        public void run() {
            sVehicle.clearToReceive();
            
            // Packet is 1000 bytes
            sVehicle.sleepFor(byteDuration(1000));
            
            sVehicle.endSend();
        }
    }
    
    public void initialTransact(GPS dest) {
        mColor = Color.RED;
        
        for (Vehicle v: aVehicle) {
            v.ping(mCoord, dest, mVehicle);
        }
    }
    
    public void startTransaction() {
        for (Vehicle v: aVehicle) {
            v.ping(mCoord, dCoord, mVehicle);
        }
    }
    
    private class WorkThread extends Thread {
        @Override
        public void run() {
            float deltax, deltay, destx, desty;
            // Sanity check, is the source location within 100 m
            if (getDistance(sCoord.getLat(), sCoord.getLong(),
                    mCoord.getLat(), mCoord.getLong()) <= 100.0) {
                mColor = Color.DARK_GRAY;
                deltax = sCoord.getLat() - mCoord.getLat();
                deltay = sCoord.getLong() - mCoord.getLong();
                destx = sCoord.getLat() - dCoord.getLat();
                desty = sCoord.getLong() - dCoord.getLong();
                double dAngle = Math.toDegrees(Math.atan(desty/destx));
                double mAngle = Math.toDegrees(Math.atan(deltay/deltax));
                
                // Is this module within the correct angle of the target?
                if (mAngle >= dAngle - ANGLE_DELTA
                        && mAngle <= dAngle + ANGLE_DELTA) {
                    while (true) {
                        if (interrupt) {
                            num_slots = mMod.getNewContention();
                            interrupt = false;
                            sleepFor(DIFS);
                        }
                        
                        if (num_slots == 0) {
                            oVehicle.requestToReceive(mVehicle);
                            mColor = Color.BLUE;
                            
                            try {
                                mVehicle.wait();
                            } catch (InterruptedException e) {
                                // Should not be interrupted in this manner
                                e.printStackTrace();
                            }
                            
                            if (interrupt) {
                                continue;
                            } else {
                                mColor = Color.GREEN;
                                startTransaction();
                            }
                        }
                    }
                }
            }
        }
    }

}
