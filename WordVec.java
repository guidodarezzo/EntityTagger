import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Caroline on 10/25/17.
 */
public class WordVec {
    public static void main (String[] args) throws IOException{
        readGloVe();

        double test = euclideanGlove("mitten", "estate");
        System.out.println(test);
        double cosTest = cosineGlove("mitten", "estate");
        System.out.println(cosTest);
    }
    static HashMap<String, ArrayList<Double>> WordVector = new HashMap<>();
    static void readGloVe() {
        try {
            FileReader reader = new FileReader("glove.6B.50d.txt");
            BufferedReader buffReader = new BufferedReader(reader);
            String line = "";

            while ((line = buffReader.readLine()) != null) {
                String[] splitLine = line.split("\\s+");
                ArrayList<Double> tempVec = new ArrayList<>();
                try {
                    for (int k = 1; k < splitLine.length; k++) {
                        tempVec.add(Double.parseDouble(splitLine[k]));
                    }
                }
                catch (NumberFormatException n) {
                    System.out.println("double parser is off...");
                }
                WordVector.put(splitLine[0], tempVec);
            }

        }

        catch (FileNotFoundException f) {
            System.out.println("GroVe word vector file not found");
        }
        catch (IOException ex) {
            System.out.println("Error reading GroVe file");
        }
    }

    static void readGoogVec() {
        try {
            FileReader reader = new FileReader("GoogVec.txt");
            BufferedReader buffReader = new BufferedReader(reader);
            String line = "";

            while ((line = buffReader.readLine()) != null) {
                String[] splitLine = line.split("\\s+");
                ArrayList<Double> tempVec = new ArrayList<>();
                try {
                    for (int k = 1; k < splitLine.length; k++) {
                        tempVec.add(Double.parseDouble(splitLine[k]));
                    }
                }
                catch (NumberFormatException n) {
                    System.out.println("double parser is off...");
                }
                WordVector.put(splitLine[0], tempVec);
            }

        }

        catch (FileNotFoundException f) {
            System.out.println("GroVe word vector file not found");
        }
        catch (IOException ex) {
            System.out.println("Error reading GroVe file");
        }
    }

    static double euclideanGlove(String word1, String word2) {

        String lower1 = word1.toLowerCase();
        String lower2 = word2.toLowerCase();
        ArrayList<Double> w1 = new ArrayList<>();
        ArrayList<Double> w2 = new ArrayList<>();


        if (WordVector.get(lower1) == null) {
            for (int i = 0; i < 50; i ++) {
                w1.add(0.0);
            }
        }
        if (WordVector.get(lower1) != null) {
            w1 = WordVector.get(lower1);
        }
        if (WordVector.get(lower2) == null) {
            for (int i = 0; i < 50; i ++) {
                w2.add(0.0);
            }
        }
        if (WordVector.get(lower2) != null) {
            w2 = WordVector.get(lower2);
        }

        System.out.println(w1);
        System.out.println(w2);
        System.out.println(w2.size());

        float sum = 0;
        for (int i = 0; i < w1.size(); i++) {
            sum += Math.pow((w1.get(i) - w2.get(i)), 2);
        }

        return Math.sqrt(sum);
    }

    static double cosineGlove(String word1, String word2) {
        String lower1 = word1.toLowerCase();
        String lower2 = word2.toLowerCase();
        ArrayList<Double> w1 = new ArrayList<>();
        ArrayList<Double> w2 = new ArrayList<>();

        if (WordVector.get(lower1) == null) {
            for (int i = 0; i < 50; i ++) {
                w1.add(0.0);
            }
        }
        if (WordVector.get(lower1) != null) {
            w1 = WordVector.get(lower1);
        }
        if (WordVector.get(lower2) == null) {
            for (int i = 0; i < 50; i ++) {
                w2.add(0.0);
            }
        }
        if (WordVector.get(lower2) != null) {
            w2 = WordVector.get(lower2);
        }

        float sumASq = 0;
        float sumBSq = 0;
        float sumAB = 0;
        for (int i = 0; i < w1.size(); i++) {
            sumAB += (w1.get(i) * w2.get(i));
            sumASq += (Math.pow(w1.get(i), 2));
            sumBSq += (Math.pow(w2.get(i), 2));
        }

        try {
            return sumAB / (Math.sqrt(sumASq) * Math.sqrt(sumBSq));
        }
        catch (ArithmeticException a) {
            System.out.println("division by 0 in cosine distance");
            return sumAB / (Math.sqrt(sumASq) * Math.sqrt(sumBSq) + 1);
        }

    }

}
