import java.util.Random;

public class RunThread {
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
    
    private class Vehicle extends RunThread {
        private GPS mCoord;
        private GPS sCoord;
        private GPS dCoord;
        private WorkThread mThread = null;
        private final double MIN_ANGLE = 15;
        private final double MAX_ANGLE = 90 - MIN_ANGLE;
        private Module mMod;
        
        public Vehicle(GPS coord) {
            mCoord = coord;
            mMod = new Module();
        }
        
        public void ping(GPS source, GPS dest) {
            sCoord = source;
            dCoord = dest;
            
            /** If there is an existing request to send
             * Then ignore the request, start a new one
            */
            mThread = new WorkThread();
            mThread.start();
        }
        
        private class WorkThread extends Thread {
            @Override
            public void run() {
                float deltax, deltay, destx, desty;
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
                }
            }
        }
    }
}
