import java.io.File;

public class MainThread {
    public static void main(String[] args) {
        RunThread thread = new RunThread();
        File fp;
        
        if (args.length >= 1) {
            fp = new File(args[0]);
            thread.startAlgorithm(fp);
        } else {
            fp = new File("localmap");
            thread.startAlgorithm(fp);
        }
    }
}
