import java.awt.Color;
import java.util.Random;
import java.util.Scanner;

public class Vehicle extends RunThread {
    private GPS mCoord;
    private GPS sCoord;
    private GPS dCoord;
    private WorkThread mThread = null;
    private double ANGLE_DELTA = 15;
    private Module mMod;
    private int num_slots;
    private boolean interrupt = false;
    // Not caring about CTS and ACK time, since overkill
    //private final int SIFS = 10;
    private final int SLOT = 5;
    private final int DIFS = 50; // Ensure there is not thread collision
    private final int MEGA_BYTE = 1000 * 1000;
    private final double FREQ = 3.375 * MEGA_BYTE; // 12MB/s
    private Vehicle oVehicle;
    private Vehicle mVehicle;
    private Vehicle sVehicle;
    private SendThread mSend = null;
    private Color mColor = Color.BLACK;
    private RunThread mRunThread;
    private boolean transacting = false;
    private boolean delivered = false;
    private int VIN;
    private boolean I_AM_ORIGIN = false;
    private TaskTimeOut mTimeout;
    private static final int ASSUME_LOST = 25;
    private static final boolean DEBUG = false;
    
    public Vehicle(GPS coord, RunThread thread) {
        mCoord = coord;
        mMod = new Module();
        mVehicle = this;
        mRunThread = thread;
        VIN = this.hashCode();
    }
    
    public GPS getGPS() {
        return mCoord;
    }
    
    public Color getColor() {
        return mColor;
    }
    
    public Module getModule() {
        return mMod;
    }
    
    public void setColor(Color c) {
        mColor = c;
    }
    
    public void doubleAngle() {
        ANGLE_DELTA = ANGLE_DELTA * 2;
    }
    
    public void resetAngle() {
        ANGLE_DELTA = 15;
    }
    
    public void occurCollision() {
        interrupt = true;
        mMod.occurCollision();
        
        synchronized (mVehicle) {
            mVehicle.notifyAll();
        }
    }
    
    public int getVin() {
        return VIN;
    }
    
    public void setInterrupt(boolean x) {
        // Very dangerous, only use in synchronized method
        interrupt = x;
    }
    
    private long byteDuration(int bytes) {
        long value;
        double TO_MICRO_SEC = 1000000.0;
        value = (long) (bytes * TO_MICRO_SEC / FREQ);
        
        return value;
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
    
    /**
     * Input source location, and current/destination location
     * Then returns distance between two locations
     * @param a
     * @param b
     * @return
     */
    private float getDistance(GPS a, GPS b) {
        return (float) Math.sqrt(Math.pow(a.getLat() - b.getLat(), 2.0)
                + Math.pow(a.getLong() - b.getLong(), 2.0));
    }
    
    private class Module {
        private Random mRand = new Random();
        private int mExp = 0;
        private final int MAX_EXP = 10;
        
        public int getNewContention() {
            // Return the time slot
            return mRand.nextInt((int) Math.pow(2.0, (double) mExp));
        }
        
        public void resetCollision() {
            mExp = 0;
        }
        
        public void occurCollision() {
            // Exponential expansion back-off
            if (mExp < MAX_EXP) {
                mExp++;
            }
        }
    }
    
    public void ping(GPS source, GPS dest, Vehicle v) {
        sCoord = source;
        dCoord = dest;
        oVehicle = v;
        
        // Origin should not restart
        if (I_AM_ORIGIN) {
            return;
        }
        
        /** If there is an existing request to send/receive
         * Then ignore the request, start a new one
        */
        mThread = new WorkThread();
        mThread.start();
    }
    
    public synchronized boolean requestToReceive(Vehicle v) {
        boolean success = false;
        
        if (interrupt || delivered) {
            // Do not accept any RTRs
        } else if (mSend == null) {
            sVehicle = v;
            mSend = new SendThread();
            mSend.start();
            success = true;
        } else {
            for (int i = 0; i < mRunThread.aVehicle.size(); i++) {
                if (getDistance(mCoord, mRunThread.aVehicle.get(i).getGPS()) <= 100) {
                    mRunThread.aVehicle.get(i).occurCollision();
                }
            }
            
            if (mSend != null) {
                mSend.interrupt();
                mSend = null;
            }
        }
        
        if (mTimeout != null) {
            mTimeout.interrupt();
        }
        
        return success;
    }
    
    public void clearToReceive() {
        synchronized (mVehicle) {
            mVehicle.notify();
        }
    }
    
    // Client notification
    public void finishSending() {
        synchronized (mVehicle) {
            mVehicle.notifyAll();
        }
        
        if (mColor != Color.GREEN && mColor != Color.RED) {
            mColor = Color.BLACK;
        }
        
        transacting = false;
    }
    
    // Server notification
    public void endSend() {        
        for (int i = 0; i < mRunThread.aVehicle.size(); i++) {
            if (getDistance(mCoord, mRunThread.aVehicle.get(i).getGPS()) <= 100) {
                mRunThread.aVehicle.get(i).finishSending();
                mRunThread.aVehicle.get(i).resetAngle();
            }
        }
    }
    
    // Client monitoring
    public void isTransact() {
        transacting = true;
    }
    
    private class SendThread extends Thread {
        @Override
        public void run() {
            sVehicle.clearToReceive();
            
            for (int i = 0; i < mRunThread.aVehicle.size(); i++) {
                if (getDistance(mCoord, mRunThread.aVehicle.get(i).getGPS()) <= 100) {
                    mRunThread.aVehicle.get(i).isTransact();
                }
            }
            
            // Packet is 1000 bytes
            try {
                sleep(byteDuration(1000));
            } catch (InterruptedException e) {
                // Collision occurs kill self
                log(getVin() + ": KILL SEND THREAD");
                return;
            }
            
            sVehicle.endSend();
            
            mSend = null;
        }
    }
    
    // First transaction (push of dominoes)
    public void initialTransact(GPS dest) {
        mColor = Color.RED;
        I_AM_ORIGIN = true;
        sCoord = mCoord;
        dCoord = dest;
        
        mTimeout = new TaskTimeOut();
        mTimeout.start();
        
        for (int i = 0; i < mRunThread.aVehicle.size(); i++) {
            if (getDistance(mCoord, mRunThread.aVehicle.get(i).getGPS()) <= 100
                    && !mRunThread.aVehicle.get(i).getGPS().compare(mCoord)) {
                mRunThread.aVehicle.get(i).ping(mCoord, dest, mVehicle);
            }
        }
    }
    
    // Start the next connection (lengthen bridge)
    public void startTransaction() {
        if (mRunThread.mPath.size() > ASSUME_LOST) {
            mRunThread.notifyFinished();
            
            for (int i = 0; i < mRunThread.aVehicle.size(); i++) {
                mRunThread.aVehicle.get(i).messageDelivered();
            }
            return;
        }
        
        mTimeout = new TaskTimeOut();
        mTimeout.start();
        
        for (int i = 0; i < mRunThread.aVehicle.size(); i++) {
            if (getDistance(mCoord, mRunThread.aVehicle.get(i).getGPS()) <= 100
                    && !mRunThread.aVehicle.get(i).getGPS().compare(mCoord)
                    && (I_AM_ORIGIN || !mRunThread.aVehicle.get(i).getGPS().compare(oVehicle.getGPS()))) {
                mRunThread.aVehicle.get(i).getModule().resetCollision();
                mRunThread.aVehicle.get(i).ping(sCoord, dCoord, mVehicle);
            }
        }
        
        mRunThread.clearContend();
        
        if (mRunThread.mUI != null) {
            mRunThread.mUI.updateFrame();
        }
    }
    
    private class TaskTimeOut extends Thread {
        @Override
        public void run() {
            try {
                // Absolute max time to wait
                sleep(DIFS + SLOT * 1024);
            } catch (InterruptedException e) {
                return;
            }
            
            for (int i = 0; i < mRunThread.aVehicle.size(); i++) {
                if (getDistance(mCoord, mRunThread.aVehicle.get(i).getGPS()) <= 100
                        && !mRunThread.aVehicle.get(i).getGPS().compare(mCoord)) {
                    mRunThread.aVehicle.get(i).doubleAngle();
                }
            }
            
            startTransaction();
        }
    }
    
    // Finish all threads
    public void messageDelivered() {
        delivered = true;
        
        // Clean up threads
        if (mTimeout != null) {
            mTimeout.interrupt();
        }
        
        if (mSend != null) {
            mSend.interrupt();
        }
        
        if (mThread != null) {
            mThread.interrupt();
        }
    }
    
    private class WorkThread extends Thread {
        @Override
        public void run() {
        double deltax, deltay, destx, desty;
            deltax = sCoord.getLat() - mCoord.getLat();
            deltay = sCoord.getLong() - mCoord.getLong();
            destx = sCoord.getLat() - dCoord.getLat();
            desty = sCoord.getLong() - dCoord.getLong();
            double dAngle = Math.atan2(desty, destx);
            double mAngle = Math.atan2(deltay, deltax);
            
            // Is this module the destination?
            if (mCoord.compare(dCoord)) {
                oVehicle.requestToReceive(mVehicle);
                
                try {
                    synchronized (mVehicle) {
                        mVehicle.wait();
                    }
                } catch (InterruptedException e) {
                    // Should not be interrupted in this manner
                    //e.printStackTrace();
                }
                
                if (interrupt) {
                    return;
                }
                
                // Now wait until transaction is complete (1000 byte data transfer)
                try {
                    synchronized (mVehicle) {
                        mVehicle.wait();
                    }
                } catch (InterruptedException e) {
                    // Should not be interrupted in this manner
                    e.printStackTrace();
                }
                
                for (int i = 0; i < mRunThread.aVehicle.size(); i++) {
                    mRunThread.aVehicle.get(i).messageDelivered();
                }
                
                mRunThread.mPath.add(mCoord);
                
                if (mRunThread.mUI != null) {
                    mRunThread.mUI.updateFrame();
                }
                
                mRunThread.notifyFinished();
                
                log(VIN + ": Reached Destination");
                return;
            } else if (mCoord.compare(sCoord)) {
                // Ignore when the current coordinate is the source coordinate
                return;
            } else if (Math.toDegrees(Math.abs(mAngle - dAngle)) < ANGLE_DELTA) {
                mColor = Color.BLUE;
                
                try {
                    sleep(DIFS);
                } catch (InterruptedException e) {
                    // Abnormal exception, kill self anyway
                    //e.printStackTrace();
                    return;
                }
                
                while (true) {
                    // Interrupts if collision occurred
                    if (interrupt) {
                        log(VIN + ": Collision occured");
                        num_slots = mMod.getNewContention();
                        interrupt = false;
                        try {
                            sleep(DIFS);
                        } catch (InterruptedException e) {
                            // Abnormal exception, kill self anyway
                            //e.printStackTrace();
                            return;
                        }
                        
                        oVehicle.setInterrupt(false);
                        finishSending();
                    }
                    
                    if (delivered) {
                        return;
                    }
                    
                    // Time to send, so RTS (or receive RTR)
                    if (transacting) {
                        try {
                            mRunThread.addContend(VIN);
                        } catch (Exception e) {
                            
                        }
                        
                        log(VIN + ": transacting occured");
                        synchronized (mVehicle) {
                            try {
                                mVehicle.wait(10000);
                            } catch (InterruptedException e) {
                                // Just kill self, this occurs when found destination
                                return;
                            }
                        }
                        
                        if (!transacting) {
                            return;
                        }
                    } else if (num_slots == 0) {
                        log(VIN + ": Start, angle: " + Math.toDegrees(Math.abs(mAngle - dAngle)));
                        if (!oVehicle.requestToReceive(mVehicle)) {
                            continue;
                        }
                        
                        log(VIN + ": RTR success");
                        try {
                            synchronized (mVehicle) {
                                mVehicle.wait();
                            }
                        } catch (InterruptedException e) {
                            // Should not be interrupted in this manner
                            e.printStackTrace();
                        }
                        
                        if (interrupt) {
                            continue;
                        }
                        
                        // Now wait until transaction is complete (1000 byte data transfer)
                        try {
                            synchronized (mVehicle) {
                                mVehicle.wait();
                            }
                        } catch (InterruptedException e) {
                            // Should not be interrupted in this manner
                            e.printStackTrace();
                            return;
                        }
                        
                        if (interrupt) {
                            continue;
                        } else {
                            log(VIN + ": End transaction");
                            mColor = Color.GREEN;
                            
                            /*
                            mRunThread.mUI.updateFrame();
                            
                            Scanner mScanner = new Scanner(System.in);
                            mScanner.nextLine();
                            */
                            
                            mRunThread.mPath.add(mCoord);
                            startTransaction();
                            return;
                        }
                    } else {
                        // Wait for contention window/slot
                        num_slots--;
                        try {
                            sleep(SLOT);
                        } catch (InterruptedException e) {
                            // Thread interrupted, kill self
                            return;
                        }
                    }
                }
            } else {
                mColor = Color.CYAN;
            }
        }
    }

    private void log(String line) {
        if (DEBUG) {
            System.out.println(line);
        }
    }
}
