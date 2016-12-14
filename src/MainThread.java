import java.io.File;

public class MainThread {
    public static void main(String[] args) {
        File fp;
        int NUM_THREADS = 1;
        
        if (args.length >= 1) {
            fp = new File(args[0]);
        } else {
            fp = new File("localmap.gif");
        }
        
        for (int i =  0; i < NUM_THREADS; i++) {
            new RunThread().startAlgorithm(fp);
        }
        
        RecordThread rt = RecordThread.getRecordThread();
        
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                if (rt.sizeOfArray() < NUM_THREADS) {
                    System.out.println("Abrupt halt, calculate statistics");
                    
                    for (int i = rt.sizeOfArray(); i < NUM_THREADS; i++) {
                        rt.addHopCount(50);
                    }
                }
            }
        });
    }
}
