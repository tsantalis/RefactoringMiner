package narrator.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExcelOperator {
    public static List<String> readCommits(String path) {
        List<String> urls = new ArrayList();

        try (FileInputStream fis = new FileInputStream(path); XSSFWorkbook workbook = new XSSFWorkbook(fis)) {
            XSSFSheet sheet = workbook.getSheetAt(0);

            int rowindex = 0;
            for (Row row : sheet) {
                if (rowindex++ == 0) {
                    continue;
                }
                urls.add(row.getCell(3).getStringCellValue());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return urls;
    }

    public static void appendTiming(String owner, String repo, String sha, int additions, int deletions,
                                    long clusterTiming, long timing) {
        String path = "timing.xlsx";

        try (FileInputStream fis = new FileInputStream(path); Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();

            Row newRow = sheet.createRow(lastRowNum + 1);
            newRow.createCell(0).setCellValue(owner);
            newRow.createCell(1).setCellValue(repo);
            newRow.createCell(2).setCellValue(sha);
            newRow.createCell(3).setCellValue(additions);
            newRow.createCell(4).setCellValue(deletions);
            newRow.createCell(5).setCellValue(clusterTiming);
            newRow.createCell(6).setCellValue(timing);

            try (FileOutputStream fos = new FileOutputStream(path)) {
                workbook.write(fos);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
