import java.awt.Color;
import java.util.Random;

public class Vehicle extends RunThread {
    private GPS mCoord;
    private GPS sCoord;
    private GPS dCoord;
    private WorkThread mThread = null;
    private final double ANGLE_DELTA = 15;
    private Module mMod;
    private int num_slots;
    private boolean interrupt = false;
    // Not caring about CTS and ACK time, since overkill
    //private final int SIFS = 10;
    private final int SLOT = 20;
    private final int DIFS = 50;
    private final int MEGA_BYTE = 1000 * 1000;
    private final double FREQ = 3.375 * MEGA_BYTE; // 12MB/s
    private Vehicle oVehicle;
    private Vehicle mVehicle;
    private Vehicle sVehicle;
    private SendThread mSend = null;
    private Color mColor = Color.BLACK;
    private RunThread mRunThread;
    private boolean transacting;
    private boolean delivered = false;
    
    public Vehicle(GPS coord, RunThread thread) {
        mCoord = coord;
        mMod = new Module();
        mVehicle = this;
        mRunThread = thread;
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
            // Return the time slot
            return mRand.nextInt((int) Math.pow(2.0, (double) mExp));
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
        
        /** If there is an existing request to send/receive
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
        
        synchronized (mVehicle) {
            mVehicle.notify();
        }
    }
    
    public void requestToReceive(Vehicle v) {
        if (mSend == null) {
            sVehicle = v;
            mSend = new SendThread();
            mSend.start();
        } else {
            v.occurCollision();
            sVehicle.occurCollision();
            
            if (mSend != null) {
                mSend.interrupt();
                mSend = null;
            }
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
        synchronized (mVehicle) {
            mVehicle.notify();
        }
    }
    
    public void finishSending() {
        synchronized (mVehicle) {
            mVehicle.notify();
        }
        
        transacting = false;
    }
    
    public void endSend() {
        synchronized (mVehicle) {
            mVehicle.notifyAll();
        }
        
        mRunThread.mPath.add(mCoord);
        
        for (int i = 0; i < mRunThread.aVehicle.size(); i++) {
            mRunThread.aVehicle.get(i).finishSending();
        }
    }
    
    public void isTransact() {
        transacting = true;
    }
    
    private class SendThread extends Thread {
        @Override
        public void run() {
            sVehicle.clearToReceive();
            
            for (int i = 0; i < mRunThread.aVehicle.size(); i++) {
                mRunThread.aVehicle.get(i).isTransact();
            }
            
            // Packet is 1000 bytes
            sVehicle.sleepFor(byteDuration(1000));
            
            sVehicle.endSend();
            
            mSend = null;
        }
    }
    
    public void initialTransact(GPS dest) {
        mColor = Color.RED;
        
        for (int i = 0; i < mRunThread.aVehicle.size(); i++) {
            if (!mRunThread.aVehicle.get(i).getGPS().compare(mCoord)) {
                mRunThread.aVehicle.get(i).ping(mCoord, dest, mVehicle);
            }
        }
    }
    
    public void startTransaction() {
        for (int i = 0; i < mRunThread.aVehicle.size(); i++) {
            if (!mRunThread.aVehicle.get(i).getGPS().compare(mCoord))
                mRunThread.aVehicle.get(i).ping(mCoord, dCoord, mVehicle);
        }
        
        if (mRunThread.mUI != null) {
            mRunThread.mUI.updateFrame();
        }
        System.out.println("Next Jump");
    }
    
    public void messageDelivered() {
        delivered = true;
    }
    
    private class WorkThread extends Thread {
        @Override
        public void run() {
            double deltax, deltay, destx, desty;
            // Sanity check, is the source location within 100 m
            if (getDistance(sCoord.getLat(), sCoord.getLong(),
                    mCoord.getLat(), mCoord.getLong()) <= 100.0) {
                deltax = sCoord.getLat() - mCoord.getLat();
                deltay = sCoord.getLong() - mCoord.getLong();
                destx = sCoord.getLat() - dCoord.getLat();
                desty = sCoord.getLong() - dCoord.getLong();
                double dAngle = Math.toDegrees(Math.atan(desty/destx));
                double mAngle = Math.toDegrees(Math.atan(deltay/deltax));
                
                // Is this module within the correct angle of the target?
                if (mCoord.compare(dCoord)) {
                    oVehicle.requestToReceive(mVehicle);
                    
                    try {
                        synchronized (mVehicle) {
                            mVehicle.wait();
                        }
                    } catch (InterruptedException e) {
                        // Should not be interrupted in this manner
                        e.printStackTrace();
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
                    
                    System.out.println("Reached Destination");
                } else if (mCoord.compare(sCoord)) {
                    // Ignore
                } else if (mAngle >= dAngle - ANGLE_DELTA
                        && mAngle <= dAngle + ANGLE_DELTA) {
                    mColor = Color.BLUE;
                    while (true) {
                        // Interrupts if collision occurred
                        if (interrupt) {
                            num_slots = mMod.getNewContention();
                            interrupt = false;
                            sleepFor(DIFS);
                        }
                        
                        if (delivered) {
                            return;
                        }
                        
                        // Time to send, so RTS (or receive RTR)
                        if (transacting) {
                            synchronized (mVehicle) {
                                try {
                                    mVehicle.wait();
                                } catch (InterruptedException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }
                            
                            if (!transacting) {
                                return;
                            }
                        } else if (num_slots == 0) {
                            oVehicle.requestToReceive(mVehicle);
                            
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
                            }
                            
                            if (interrupt) {
                                continue;
                            } else {
                                mColor = Color.GREEN;
                                startTransaction();
                            }
                        } else {
                            // Wait for contention window/slot
                            num_slots--;
                            sleepFor(SLOT);
                        }
                    }
                } else {
                    mColor = Color.CYAN;
                }
            }
        }
    }

}
