
public class RunThread {
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
    
    private class Vehicle extends RunThread {
        private GPS mCoord;
        private GPS sCoord;
        private GPS dCoord;
        
        public Vehicle(GPS coord) {
            mCoord = coord;
        }
        
        public void ping(GPS source, GPS dest) {
            sCoord = source;
            dCoord = dest;
        }
    }
}
