package filessortapp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Simon
 */
public class FilesSortApp {
    /// команды
    public static final String CMD_PREFIX = "-prefix";
    public static final String CMD_MODE = "-mode";
    public static final String CMD_TYPE = "-type";
    
    /// наибольшее количество потоков
    public static final int NUM_OF_THREADS = 4;
    
    static String prefix = "sorted_";
    static String mode = "a";
    static String type = "s";
    
    static int[] reverse(int[] arr) {
        int []res = arr;
        for (int i = 0; i < res.length / 2; i++)
        {
            int t = res[i]; res[i] = res[res.length - i - 1]; res[res.length - i - 1] = t;
        }
        return res;
    }
    
    static String[] reverse(String[] arr) {
        String []res = arr;
        for (int i = 0; i < res.length / 2; i++)
        {
            String t = res[i]; res[i] = res[res.length - i - 1]; res[res.length - i - 1] = t;
        }
        return res;
    }
    
    static int[] insertSort(int []arr, boolean isReversed) {
        int []res = arr;
        for (int i = 1; i < res.length; i++) {
            int x = res[i];
            int j = i - 1;
            while (j >= 0 && res[j] > x) {
                res[j + 1] = res[j];
                j--;
            }
            res[j + 1] = x;
        }
        if (!isReversed)
            return res;
        return reverse(res);
    }
    
    static String[] insertSort(String []arr, boolean isReversed) {
        String []res = arr;
        for (int i = 1; i < res.length; i++) {
            String x = res[i];
            int j = i - 1;
            while (j >= 0 && x.compareTo(res[j]) < 0) {
                res[j + 1] = res[j];
                j--;
            }
            res[j + 1] = x;
        }
        if (!isReversed)
            return res;
        return reverse(res);
    }
    
    static void setModifiers(String []mods) {
        for (String line : mods) {
            int i = 0; 
            String cmd_name = "";
            while (i < line.length() && line.charAt(i) != '=') {
                cmd_name += line.charAt(i);
                i++;
            }
            i++;
            if (i < line.length()) {
            String value = line.substring(i);
            if (cmd_name.equals(CMD_MODE) && (value.equals("a") || value.equals("d")))
                mode = value;
            else if (cmd_name.equals(CMD_PREFIX))
                prefix = value;
            else if (cmd_name.equals(CMD_TYPE) && (value.equals("i") || value.equals("s")))
                type = value;
            else {
                System.err.println("Invalid arguments: default settings applied");
                prefix = "sorted_";
                mode = "a";
                type = "s";
                return;
            }
            }
        }
    }
    
    static void sortFile(File file) {
        String filename = prefix + file.getName();
        if (type.equals("i")) {
            int[] arr = null;
            try (BufferedReader in = new BufferedReader(new FileReader(file.getAbsolutePath()))) {
                arr = in.lines().mapToInt(Integer::parseInt).toArray();
                in.close();
            } catch (IOException | NumberFormatException e) {
                System.err.println("Error while sorting file " + file.getAbsolutePath() + ": " + e.getMessage());
                return;
            }
            if (arr != null) {
                arr = insertSort(arr, !mode.equals("a"));
                try (BufferedWriter out = new BufferedWriter(new FileWriter(file.getParent() + "/" + filename))) {
                    if (arr.length > 1)
                    {for (int n : arr) {
                        out.write(Integer.toString(n));
                        out.write(System.getProperty("line.separator"));
                    }
                    out.close();
                    }
                    else
                    {out.write(Integer.toString(arr[0]));
                    out.close();}
                } catch (Exception e) {
                    System.err.println("Output error: " + e.getMessage());
                }
            } else {
                System.out.println("No numbers found in file");
            }
        } else if (type.equals("s")){
            String[] arr = null;
            try {
                List<String> lst = Files.readAllLines(file.toPath());
                arr = lst.toArray(new String[lst.size()]);
            } catch (IOException | NumberFormatException e) {
                System.err.println("Error while sorting file " + file.getAbsolutePath() + ": " + e.getMessage());
                return;
            }
            if (arr != null) {
                arr = insertSort(arr, !mode.equals("a"));
                try (BufferedWriter out = new BufferedWriter(new FileWriter(file.getParent() + "/" + filename))) {
                    if (arr.length > 1)
                    for (String n : arr) {
                        out.write(n);
                        out.write(System.getProperty("line.separator"));
                    }
                    else
                        out.write(arr[0]);
                } catch (Exception e) {
                    System.err.println("Output error: " + e.getMessage());
                }
            } else {
                System.out.println("No strings found in file");
            }
        }

    }
    
    static void processFolder(File folder) {
        File[] folderEntries = folder.listFiles();
        if (folderEntries.length == 0) {
            System.err.println("Empty folder");
            return;
        }
        int n = Runtime.getRuntime().availableProcessors();
        ThreadPoolExecutor service = new ThreadPoolExecutor(0, n, 1, TimeUnit.MINUTES, new ThreadPoolQueue(n));
        
        System.out.println("Number of threads = " + Integer.toString(n));
        
        for (File elem : folderEntries) {
            if (!elem.isFile()) {
                continue;
            }
            service.execute(new Runnable() {
                @Override
                public void run() {
                    System.out.println(elem.getName());
                    sortFile(elem);
                }
            });
        }
        service.shutdown();
        try {
            service.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException exc) {
            exc.printStackTrace();
        }
    }
    
    /**
     * @param args the command line arguments
     * args[0] - directory name
     * args[1], args[2], args[3] - modifiers of prefix, datatype and sort mode in any order
     * not every modified must be specified
     */
    public static void main(String[] args) {
        if (args.length < 1 || args.length > 4) {
            System.err.println("Invalid number of arguments - execution aborted");
            return;
        }
        setModifiers(args);
        File dir = new File(args[0]);
        if (dir.exists() && dir.isDirectory()) {
            processFolder(dir);
        }
        else {
            System.err.println("Unable to find this folder");
        }
    }
}
