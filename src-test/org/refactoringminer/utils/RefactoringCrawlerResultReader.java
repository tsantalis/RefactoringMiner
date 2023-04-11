package org.refactoringminer.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RefactoringCrawlerResultReader {

  public static RefactoringSet read(String project, String revision, String folderPath) {
//    List<RefactoringCrawlerRefactoring> list = readFolder("D:\\Danilo\\Workspaces\\phd-rmdataset\\results\\atmosphere-cc2b3f1");
    try {
      RefactoringSet result = new RefactoringSet(project, revision);
      for (RefactoringCrawlerRefactoring r : readFolder(folderPath)) {
        result.add(r.toRefactoringRelationship());
      }
      return result;
    } catch (Exception e) {
      throw new RuntimeException(e); 
    }
  }

  private static List<RefactoringCrawlerRefactoring> readFolder(String path) throws Exception {
    List<RefactoringCrawlerRefactoring> result = new ArrayList<>();
    File folder = new File(path);
    for (File f : folder.listFiles()) {
      if (f.isFile()) {
        readXml(f.getPath(), result);
      }
    }
    return result;
  }

  public static void readXml(String path, List<RefactoringCrawlerRefactoring> result) throws Exception {
    String content = readFile(path, StandardCharsets.UTF_8);
    Pattern p = Pattern.compile("<refactoring name=\"([^\"]+)\">\\s*<parameter name= \"new element\">([^/]+)</parameter>\\s*<parameter name= \"old element\">([^/]+)</parameter>");
    Matcher m = p.matcher(content);
    while (m.find()) {
      result.add(new RefactoringCrawlerRefactoring(m.group(1), m.group(2), m.group(3)));
    }
  }
  
  static String readFile(String path, Charset encoding) throws IOException {
    byte[] encoded = Files.readAllBytes(Paths.get(path));
    return new String(encoded, encoding);
  }
  
}
