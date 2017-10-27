import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

/**
 * Created by Caroline on 10/24/17.
 */

// test Brown with pure length and also percent string match?
public class Brown {

    public static void main (String[] args) throws IOException {
        readMap();
        calcBrown("Google", "apple");
    }
    static HashMap<String, String> BrownBitMap = new HashMap<>();
    static HashMap<String, String> BrownCountMap = new HashMap<>();
    static void readMap() {

        String line = null;

        try {
            FileReader reader = new FileReader("brownClusters10-2014.txt");
            BufferedReader buffReader = new BufferedReader(reader);

            int numline = 0;
            while ((line = buffReader.readLine()) != null) {
                String[] splitLine = line.split("\\s+");
                BrownBitMap.put(splitLine[1], splitLine[0]);
                BrownCountMap.put(splitLine[1], splitLine[2]);
                numline++;
            }
            reader.close();
            buffReader.close();
            System.out.println("numline = " + numline);
            // should be >= 394563
        }

        catch (FileNotFoundException e) {
            System.out.println("Brown cluster file not found");

        }

        catch (IOException ex) {
            System.out.println("Error reading Brown cluster file.");
        }

        System.out.println(BrownBitMap.get("Jordan"));
        System.out.println(BrownCountMap.get("Jordan"));
    }

    static int calcBrown(String s1, String s2) {

        int distance = 0;
        int len = 0;
        String bit1 = BrownBitMap.get(s1);
        String bit2 = BrownBitMap.get(s2);

        System.out.println(bit1);
        System.out.println(bit2);

        if (bit1.length() > bit2.length()) {
            len = bit2.length();
        }
        else {
            len = bit1.length();
        }

        for (int i = 0; i < len; i++) {
            if (bit1.charAt(i) == bit2.charAt(i)) {
                distance += 1;
            }
            else {
                break;
            }
        }
        System.out.println(distance);

        return distance;
    }
}
