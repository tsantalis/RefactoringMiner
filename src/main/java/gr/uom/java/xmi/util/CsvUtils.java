package gr.uom.java.xmi.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CsvUtils {
    public static List<String> extractParametersFromCsv(String s) {
        List<String> parameters = new ArrayList<>();
        String[] tokens = s.split(",");
        for (String token : tokens) {
            String trimmed = token.trim();
            if (trimmed.startsWith("\"")) {
                trimmed = trimmed.substring(1, trimmed.length());
            }
            if (trimmed.endsWith("\"")) {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
            }
            parameters.add(trimmed);
        }
        return parameters;
    }

    public static List<List<String>> extractParametersFromCsvFile(List<String> tests) {
        List<List<String>> testParameters = new ArrayList<>();
        for (String test : tests) {
            List<String> parameters = extractParametersFromCsv(test);
            testParameters.add(parameters);
        }
        return testParameters;
    }

    public static List<String> readLinesOfCsvFile(String csvFile) throws IOException {
        List<String> parameters = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(csvFile));
        String line = br.readLine();
        while (line != null) {
            parameters.add(line);
            line = br.readLine();
        }
        br.close();
        return parameters;
    }
}
