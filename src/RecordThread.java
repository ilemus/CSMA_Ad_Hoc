import java.text.DecimalFormat;
import java.util.ArrayList;

public class RecordThread {
    private ArrayList<Integer> mHops = new ArrayList<Integer>();
    private static final int ASSUME_LOST = 25 - 1;
    private final DecimalFormat df = new DecimalFormat("00.00");
    private static RecordThread mRt = null;
    
    public static RecordThread getRecordThread() {
        if (mRt == null) {
            mRt = new RecordThread();
        }
        return mRt;
    }
    
    public void addHopCount(int x) {
        mHops.add(x);
        double temp = 0;
        double avg = 0;
        double dev = 0;
        
        for (int i = 0; i < mHops.size(); i++) {
            if (mHops.get(i) > ASSUME_LOST) {
                temp++;
            } else {
                avg += mHops.get(i);
            }
        }

        avg = avg / (mHops.size() - temp);
        
        for (int i = 0; i < mHops.size(); i++) {
            dev += Math.pow((mHops.get(i) - avg), 2);
        }
        
        dev = dev/mHops.size();

        double percent = (double)(mHops.size() - temp) / mHops.size();
        
        System.out.println("AVG: " + df.format(avg) + ", Dev: " + df.format(dev) + ", SUCCESS: " + df.format(percent));
    }
    
    public int sizeOfArray() {
        return mHops.size();
    }
}
