package gr.uom.java.xmi.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CsvUtils {
    public static List<String> extractParametersFromCsv(String s) {
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1);
        }
        List<String> parameters = new ArrayList<>();
        int i = 0;
        int n = s.length();
        while (true) {
            while (i < n && Character.isWhitespace(s.charAt(i))) {
                i++;
            }
            String field;
            if (i < n && s.charAt(i) == '\'') {
                StringBuilder field1 = new StringBuilder();
                i++; //skip opening quote
                while (i < n) {
                    char c = s.charAt(i);
                    if (c == '\'') {
                        //two consecutive single quotes inside a quoted field is an escaped literal quote
                        if (i + 1 < n && s.charAt(i + 1) == '\'') {
                            field1.append('\'');
                            i += 2;
                            continue;
                        }
                        i++; //skip closing quote
                        break;
                    }
                    field1.append(c);
                    i++;
                }
                //skip any (whitespace) content between the closing quote and the next comma
                while (i < n && s.charAt(i) != ',') {
                    i++;
                }
                field = field1.toString();
            }
            else {
                int start = i;
                while (i < n && s.charAt(i) != ',') {
                    i++;
                }
                String trimmed = s.substring(start, i).trim();
                if (trimmed.startsWith("\"")) {
                    trimmed = trimmed.substring(1);
                }
                if (trimmed.endsWith("\"")) {
                    trimmed = trimmed.substring(0, trimmed.length() - 1);
                }
                field = trimmed;
            }
            parameters.add(field);
            if (i >= n) {
                break;
            }
            i++; //skip the comma separator
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
