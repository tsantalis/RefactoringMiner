package com.alibaba.innodb.java.reader;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;

import com.alibaba.innodb.java.reader.cli.InnodbReaderBootstrap;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author xu.zx
 */
public class InnodbReaderBootstrapTest {

  private String sourceIbdFilePath =
      "../innodb-java-reader/src/test/resources/testsuite/mysql57/multiple/level/tb10.ibd";

  private String createTableSqlPath = "src/test/resources/tb10.sql";

  /**
   * when debugging test case, you can set to true
   */
  private static final boolean ENABLE_CONSOLE_OUTPUT = false;

  private static SysoutInterceptor SYS_OUT_INTERCEPTOR;

  /**
   * By running the following command:
   * <pre>
   *   mysql -N -uroot -Dtest -e "select * from test.tb10" > tb10.dat
   * </pre>
   */
  private static String TB10_DATA_FILE = "src/test/resources/tb10.dat";

  /**
   * By running the following command:
   * <pre>
   *   mysql -N -uroot -Dtest -e "select * from test.tb10" >
   *   tb10.dat2 && cat tb10.dat2 | tr "\t" "," > tb10-comma-delimiter.dat
   * </pre>
   */
  private static String TB10_DATA_FILE_WITH_COMMA_DELIMITER =
      "src/test/resources/tb10-comma-delimiter.dat";

  /**
   * By running the following command:
   * <pre>
   *   mysql -uroot -Dtest -e "select * from test.tb10" > tb10-with-header.dat
   * </pre>
   */
  private static String TB10_DATA_FILE_WITH_HEADER =
      "src/test/resources/tb10-with-header.dat";

  private static String EXPECTED_TB10_DATE_FILE_MD5;
  private static String EXPECTED_TB10_DATE_FILE_WITH_COMMA_DELIMITER_MD5;
  private static String EXPECTED_TB10_DATE_FILE_WITH_HEADER_MD5;

  @BeforeClass
  public static void prepareWhenClassInitiate() throws IOException {
    PrintStream origOut = System.out;
    SYS_OUT_INTERCEPTOR = new SysoutInterceptor(origOut, ENABLE_CONSOLE_OUTPUT);
    System.setOut(SYS_OUT_INTERCEPTOR);
    EXPECTED_TB10_DATE_FILE_MD5 = getFileMD5(TB10_DATA_FILE);
    EXPECTED_TB10_DATE_FILE_WITH_COMMA_DELIMITER_MD5 = getFileMD5(TB10_DATA_FILE_WITH_COMMA_DELIMITER);
    EXPECTED_TB10_DATE_FILE_WITH_HEADER_MD5 = getFileMD5(TB10_DATA_FILE_WITH_HEADER);
  }

  @Before
  public void prepare() {
    SYS_OUT_INTERCEPTOR.clear();
  }

  @Test
  public void testShowAllPages() {
    String[] args = {"-ibd-file-path", sourceIbdFilePath,
        "-create-table-sql-file-path", createTableSqlPath,
        "-c", "show-all-pages"};
    InnodbReaderBootstrap.main(args);
    List<String> output = SYS_OUT_INTERCEPTOR.getOutput();
    String expected = "0,FILE_SPACE_HEADER,space=173,numPagesUsed=44,size=576,xdes.size=2\n"
        + "1,IBUF_BITMAP\n"
        + "2,INODE,inode.size=6\n"
        + "3,INDEX,root.page=true,index.id=226,level=1,numOfRecs=39,num.dir.slot=10,garbage.space=0\n"
        + "4,INDEX,root.page=true,index.id=227,level=1,numOfRecs=2,num.dir.slot=2,garbage.space=0\n"
        + "5,INDEX,root.page=true,index.id=228,level=1,numOfRecs=4,num.dir.slot=2,garbage.space=0\n"
        + "6,INDEX,index.id=226,level=0,numOfRecs=13,num.dir.slot=5,garbage.space=7501\n"
        + "7,INDEX,index.id=226,level=0,numOfRecs=26,num.dir.slot=7,garbage.space=0\n"
        + "8,INDEX,index.id=226,level=0,numOfRecs=26,num.dir.slot=7,garbage.space=0\n"
        + "9,INDEX,index.id=226,level=0,numOfRecs=26,num.dir.slot=7,garbage.space=0\n"
        + "10,INDEX,index.id=226,level=0,numOfRecs=26,num.dir.slot=7,garbage.space=0\n"
        + "11,INDEX,index.id=226,level=0,numOfRecs=26,num.dir.slot=7,garbage.space=0\n"
        + "12,INDEX,index.id=226,level=0,numOfRecs=26,num.dir.slot=7,garbage.space=0\n"
        + "13,INDEX,index.id=226,level=0,numOfRecs=26,num.dir.slot=7,garbage.space=0\n"
        + "14,INDEX,index.id=226,level=0,numOfRecs=26,num.dir.slot=7,garbage.space=0\n"
        + "15,INDEX,index.id=226,level=0,numOfRecs=26,num.dir.slot=7,garbage.space=0\n"
        + "16,INDEX,index.id=226,level=0,numOfRecs=26,num.dir.slot=7,garbage.space=0\n"
        + "17,INDEX,index.id=226,level=0,numOfRecs=26,num.dir.slot=7,garbage.space=0\n"
        + "18,INDEX,index.id=226,level=0,numOfRecs=26,num.dir.slot=7,garbage.space=0\n"
        + "19,INDEX,index.id=226,level=0,numOfRecs=26,num.dir.slot=7,garbage.space=0\n"
        + "20,INDEX,index.id=226,level=0,numOfRecs=26,num.dir.slot=7,garbage.space=0\n"
        + "21,INDEX,index.id=226,level=0,numOfRecs=26,num.dir.slot=7,garbage.space=0\n"
        + "22,INDEX,index.id=228,level=0,numOfRecs=239,num.dir.slot=55,garbage.space=6006\n"
        + "23,INDEX,index.id=228,level=0,numOfRecs=260,num.dir.slot=61,garbage.space=5124\n"
        + "24,INDEX,index.id=226,level=0,numOfRecs=26,num.dir.slot=7,garbage.space=0\n"
        + "25,INDEX,index.id=226,level=0,numOfRecs=26,num.dir.slot=7,garbage.space=0\n"
        + "26,INDEX,index.id=226,level=0,numOfRecs=26,num.dir.slot=7,garbage.space=0\n"
        + "27,INDEX,index.id=226,level=0,numOfRecs=26,num.dir.slot=7,garbage.space=0\n"
        + "28,INDEX,index.id=226,level=0,numOfRecs=26,num.dir.slot=7,garbage.space=0\n"
        + "29,INDEX,index.id=226,level=0,numOfRecs=26,num.dir.slot=7,garbage.space=0\n"
        + "30,INDEX,index.id=226,level=0,numOfRecs=26,num.dir.slot=7,garbage.space=0\n"
        + "31,INDEX,index.id=226,level=0,numOfRecs=26,num.dir.slot=7,garbage.space=0\n"
        + "32,INDEX,index.id=226,level=0,numOfRecs=26,num.dir.slot=7,garbage.space=0\n"
        + "33,INDEX,index.id=226,level=0,numOfRecs=26,num.dir.slot=7,garbage.space=0\n"
        + "34,INDEX,index.id=226,level=0,numOfRecs=26,num.dir.slot=7,garbage.space=0\n"
        + "35,INDEX,index.id=226,level=0,numOfRecs=26,num.dir.slot=7,garbage.space=0\n"
        + "36,INDEX,index.id=226,level=0,numOfRecs=26,num.dir.slot=7,garbage.space=0\n"
        + "37,INDEX,index.id=226,level=0,numOfRecs=26,num.dir.slot=7,garbage.space=0\n"
        + "38,INDEX,index.id=228,level=0,numOfRecs=261,num.dir.slot=63,garbage.space=0\n"
        + "39,INDEX,index.id=226,level=0,numOfRecs=26,num.dir.slot=7,garbage.space=0\n"
        + "40,INDEX,index.id=228,level=0,numOfRecs=240,num.dir.slot=59,garbage.space=0\n"
        + "41,INDEX,index.id=226,level=0,numOfRecs=26,num.dir.slot=7,garbage.space=0\n"
        + "42,INDEX,index.id=227,level=0,numOfRecs=464,num.dir.slot=118,garbage.space=7888\n"
        + "43,INDEX,index.id=227,level=0,numOfRecs=536,num.dir.slot=135,garbage.space=0\n"
        + "44,ALLOCATED\n"
        + "45,ALLOCATED\n"
        + "46,ALLOCATED";
    String[] array = expected.split("\n");
    assertThat(output.size(), is(577));
    for (int i = 0; i < array.length; i++) {
      assertThat(output.get(i + 1), is(array[i]));
    }
  }

  @Test
  public void testShowPages() {
    String[] args = {"-ibd-file-path", sourceIbdFilePath, "-create-table-sql-file-path", createTableSqlPath,
        "-c", "show-pages", "-args", "3,4,5"};
    InnodbReaderBootstrap.main(args);
    List<String> output = SYS_OUT_INTERCEPTOR.getOutput();
    assertThat(output.size(), is(3));
    assertThat(output.get(0).startsWith("Index(indexHeader=IndexHeader(numOfDirSlots=10, "
            + "heapTopPosition=627, numOfHeapRecords=41, format=COMPACT"),
        is(true));
  }

  @Test
  public void testShowPagesJsonPrettyStyle() {
    String[] args = {"-ibd-file-path", sourceIbdFilePath, "-create-table-sql-file-path", createTableSqlPath,
        "-json-pretty-style", "-c", "show-pages", "-args", "3,4,5"};
    InnodbReaderBootstrap.main(args);
    List<String> output = SYS_OUT_INTERCEPTOR.getOutput();
    assertThat(output.size(), is(3));
  }

  @Test
  public void testQueryAll() {
    String[] args = {"-ibd-file-path", sourceIbdFilePath, "-create-table-sql-file-path", createTableSqlPath,
        "-c", "query-all"};
    InnodbReaderBootstrap.main(args);
    List<String> output = SYS_OUT_INTERCEPTOR.getOutput();
    assertThat(output.size(), is(1000));
    check(output, 1, 1000);
  }

  @Test
  public void testQueryAllWithOrder() {
    String[] args = {"-ibd-file-path", sourceIbdFilePath, "-create-table-sql-file-path", createTableSqlPath,
        "-desc", "-c", "query-all"};
    InnodbReaderBootstrap.main(args);
    List<String> output = SYS_OUT_INTERCEPTOR.getOutput();
    assertThat(output.size(), is(1000));
    check(output, 1, 1000, true);
  }

  @Test
  public void testQueryAllWithProjection() {
    String[] args = {"-ibd-file-path", sourceIbdFilePath, "-create-table-sql-file-path", createTableSqlPath,
        "-c", "query-all", "-projection", "id,a"};
    InnodbReaderBootstrap.main(args);
    List<String> output = SYS_OUT_INTERCEPTOR.getOutput();
    assertThat(output.size(), is(1000));
    // pk is included by default
    check(output, 1, 1000, ImmutableList.of("a"));
  }

  @Test
  public void testQueryAllWithProjectionDesc() {
    String[] args = {"-ibd-file-path", sourceIbdFilePath, "-create-table-sql-file-path", createTableSqlPath,
        "-c", "query-all", "-desc", "-projection", "id,a"};
    InnodbReaderBootstrap.main(args);
    List<String> output = SYS_OUT_INTERCEPTOR.getOutput();
    assertThat(output.size(), is(1000));
    // pk is included by default
    check(output, 1, 1000, ImmutableList.of("a"), true);
  }

  @Test
  public void testQueryByPageNumberNonLeafPage() {
    String[] args = {"-ibd-file-path", sourceIbdFilePath, "-create-table-sql-file-path", createTableSqlPath,
        "-c", "query-by-page-number", "-args", "3", "-delimiter", ","};
    InnodbReaderBootstrap.main(args);
    List<String> output = SYS_OUT_INTERCEPTOR.getOutput();
    assertThat(output.size(), is(39));
    assertThat(output.get(1), is(String.valueOf("14,null,null,null")));
  }

  @Test
  public void testQueryByPageNumberLeafPage() {
    String[] args = {"-ibd-file-path", sourceIbdFilePath, "-create-table-sql-file-path", createTableSqlPath,
        "-c", "query-by-page-number", "-args", "64"};
    InnodbReaderBootstrap.main(args);
    List<String> output = SYS_OUT_INTERCEPTOR.getOutput();
    assertThat(output.size(), is(26));
    check(output, 820, 26);
  }

  @Test
  public void testQueryByPrimaryKey() {
    String[] args = {"-ibd-file-path", sourceIbdFilePath, "-create-table-sql-file-path", createTableSqlPath,
        "-c", "query-by-pk", "-args", "888"};
    InnodbReaderBootstrap.main(args);
    List<String> output = SYS_OUT_INTERCEPTOR.getOutput();
    assertThat(output.size(), is(1));
    check(output, 888, 1);
  }

  @Test
  public void testQueryByPrimaryKeyWithProjection() {
    String[] args = {"-ibd-file-path", sourceIbdFilePath, "-create-table-sql-file-path", createTableSqlPath,
        "-projection", "b", "-c", "query-by-pk", "-args", "888"};
    InnodbReaderBootstrap.main(args);
    List<String> output = SYS_OUT_INTERCEPTOR.getOutput();
    assertThat(output.size(), is(1));
    check(output, 888, 1, ImmutableList.of("b"));
  }

  @Test
  public void testQueryByPrimaryKeyNotExist() {
    String[] args = {"-ibd-file-path", sourceIbdFilePath, "-create-table-sql-file-path", createTableSqlPath,
        "-c", "query-by-pk", "-args", "999999"};
    InnodbReaderBootstrap.main(args);
    List<String> output = SYS_OUT_INTERCEPTOR.getOutput();
    assertThat(output.size(), is(0));
  }

  /**
   * print java.lang.IllegalArgumentException:
   * argument number should not exactly two, delimited by ; 700,800
   */
  @Test
  public void testRangeQueryByPrimaryKeyNegative() {
    String[] args = {"-ibd-file-path", sourceIbdFilePath, "-create-table-sql-file-path", createTableSqlPath,
        "-c", "range-query-by-pk", "-args", "700,800"};
    InnodbReaderBootstrap.main(args);
  }

  @Test
  public void testRangeQueryByPrimaryKey() {
    String[] args = {"-ibd-file-path", sourceIbdFilePath, "-create-table-sql-file-path", createTableSqlPath,
        "-c", "range-query-by-pk", "-args", ">=;700;<;800"};
    InnodbReaderBootstrap.main(args);
    List<String> output = SYS_OUT_INTERCEPTOR.getOutput();
    assertThat(output.size(), is(100));
    check(output, 700, 100);
  }

  @Test
  public void testRangeQueryByPrimaryKeyDesc() {
    String[] args = {"-ibd-file-path", sourceIbdFilePath, "-create-table-sql-file-path", createTableSqlPath,
        "-desc", "-c", "range-query-by-pk", "-args", ">=;700;<;800"};
    InnodbReaderBootstrap.main(args);
    List<String> output = SYS_OUT_INTERCEPTOR.getOutput();
    assertThat(output.size(), is(100));
    check(output, 700, 100, true);
  }

  @Test
  public void testRangeQueryByPrimaryKey2() {
    String[] args = {"-ibd-file-path", sourceIbdFilePath, "-create-table-sql-file-path", createTableSqlPath,
        "-c", "range-query-by-pk", "-args", "\">=;700;<;800\""};
    InnodbReaderBootstrap.main(args);
    List<String> output = SYS_OUT_INTERCEPTOR.getOutput();
    assertThat(output.size(), is(100));
    check(output, 700, 100);
  }

  @Test
  public void testRangeQueryByPrimaryKey3() {
    String[] args = {"-ibd-file-path", sourceIbdFilePath, "-create-table-sql-file-path", createTableSqlPath,
        "-c", "range-query-by-pk", "-args", ">;700;nop;null"};
    InnodbReaderBootstrap.main(args);
    List<String> output = SYS_OUT_INTERCEPTOR.getOutput();
    assertThat(output.size(), is(300));
    check(output, 701, 300);
  }

  @Test
  public void testRangeQueryByPrimaryKeyWithProjection() {
    String[] args = {"-ibd-file-path", sourceIbdFilePath, "-create-table-sql-file-path", createTableSqlPath,
        "-projection", "id,a", "-c", "range-query-by-pk", "-args", ">=;700;<=;800"};
    InnodbReaderBootstrap.main(args);
    List<String> output = SYS_OUT_INTERCEPTOR.getOutput();
    assertThat(output.size(), is(101));
    check(output, 700, 100, ImmutableList.of("id", "a"));
  }

  @Test
  public void testRangeQueryByPrimaryKeyWithProjectionDesc() {
    String[] args = {"-ibd-file-path", sourceIbdFilePath, "-create-table-sql-file-path", createTableSqlPath,
        "-projection", "id,a", "-c", "range-query-by-pk", "-desc", "-args", ">=;700;<;800"};
    InnodbReaderBootstrap.main(args);
    List<String> output = SYS_OUT_INTERCEPTOR.getOutput();
    assertThat(output.size(), is(100));
    check(output, 700, 100, ImmutableList.of("id", "a"), true);
  }

  // ~~~ sk

  @Test
  public void testQueryBySecondaryKey() {
    String[] args = {"-ibd-file-path", sourceIbdFilePath, "-create-table-sql-file-path", createTableSqlPath,
        "-c", "query-by-sk", "-args", ">=;1000;<;1900", "-skname", "a"};
    InnodbReaderBootstrap.main(args);
    List<String> output = SYS_OUT_INTERCEPTOR.getOutput();
    assertThat(output.size(), is(450));
    check(output, 500, 450);
  }

  @Test
  public void testQueryBySecondaryKeyVarcharKey() {
    String[] args = {"-ibd-file-path", sourceIbdFilePath, "-create-table-sql-file-path", createTableSqlPath,
        "-c", "query-by-sk", "-args",
        ">=;" + StringUtils.repeat("g", 32) + ";<=;" + StringUtils.repeat("g", 32),
        "-skname", "b"};
    InnodbReaderBootstrap.main(args);
    List<String> output = SYS_OUT_INTERCEPTOR.getOutput();
    assertThat(output.size(), is(33));
  }

  @Test
  public void testQueryBySecondaryKeyDesc() {
    String[] args = {"-ibd-file-path", sourceIbdFilePath, "-create-table-sql-file-path", createTableSqlPath,
        "-c", "query-by-sk", "-args", ">=;500;<;1800", "-skname", "a", "-desc"};
    InnodbReaderBootstrap.main(args);
    List<String> output = SYS_OUT_INTERCEPTOR.getOutput();
    assertThat(output.size(), is(650));
    check(output, 250, 650, true);
  }

  @Test
  public void testQueryBySecondaryKey2() {
    String[] args = {"-ibd-file-path", sourceIbdFilePath, "-create-table-sql-file-path", createTableSqlPath,
        "-c", "query-by-sk", "-args", ">=;987;nop;null", "-skname", "a"};
    InnodbReaderBootstrap.main(args);
    List<String> output = SYS_OUT_INTERCEPTOR.getOutput();
    assertThat(output.size(), is(507));
    check(output, 494, 507);
  }

  @Test
  public void testQueryBySecondaryKeyWithProjectionCoveringIndex() {
    String[] args = {"-ibd-file-path", sourceIbdFilePath, "-create-table-sql-file-path", createTableSqlPath,
        "-projection", "id,a", "-c", "query-by-sk", "-args", ">=;1700;<;1800", "-skname", "a"};
    InnodbReaderBootstrap.main(args);
    List<String> output = SYS_OUT_INTERCEPTOR.getOutput();
    assertThat(output.size(), is(50));
    check(output, 850, 50, ImmutableList.of("id", "a"));
  }

  @Test
  public void testQueryBySecondaryKeyWithProjection() {
    String[] args = {"-ibd-file-path", sourceIbdFilePath, "-create-table-sql-file-path", createTableSqlPath,
        "-projection", "id,b", "-c", "query-by-sk", "-args", ">=;1700;<;1800", "-skname", "a"};
    InnodbReaderBootstrap.main(args);
    List<String> output = SYS_OUT_INTERCEPTOR.getOutput();
    assertThat(output.size(), is(50));
    check(output, 850, 50, ImmutableList.of("id", "b"));
  }

  @Test
  public void testQueryBySecondaryKeyWithProjectionDesc() {
    String[] args = {"-ibd-file-path", sourceIbdFilePath, "-create-table-sql-file-path", createTableSqlPath,
        "-desc", "-projection", "id,a", "-c", "query-by-sk", "-args", ">=;1700;<;1800", "-skname", "a"};
    InnodbReaderBootstrap.main(args);
    List<String> output = SYS_OUT_INTERCEPTOR.getOutput();
    assertThat(output.size(), is(50));
    check(output, 850, 50, ImmutableList.of("id", "a"), true);
  }

  @Test
  public void testQueryBySecondaryKeySpecifySkRootPageNumber() {
    String[] args = {"-ibd-file-path", sourceIbdFilePath, "-create-table-sql-file-path", createTableSqlPath,
        "-c", "query-by-sk", "-args", ">=;1000;<;1900", "-skname", "a", "-skrootpage", "4"};
    InnodbReaderBootstrap.main(args);
    List<String> output = SYS_OUT_INTERCEPTOR.getOutput();
    assertThat(output.size(), is(450));
    check(output, 500, 450);
  }

  @Test
  public void testQueryBySecondaryKeySpecifySkRootPageNumberNegate() {
    String[] args = {"-ibd-file-path", sourceIbdFilePath, "-create-table-sql-file-path", createTableSqlPath,
        "-c", "query-by-sk", "-args", ">=;1000;<;1900", "-skname", "a", "-skrootpage", "12"};
    InnodbReaderBootstrap.main(args);
    List<String> output = SYS_OUT_INTERCEPTOR.getOutput();
    // sk and pk type is the same INT, so query runs OK but got nothing.
    assertThat(output.size(), is(0));
  }

  @Test
  public void testQueryBySecondaryKeySpecifySkOrdinal() {
    String[] args = {"-ibd-file-path", sourceIbdFilePath, "-create-table-sql-file-path", createTableSqlPath,
        "-c", "query-by-sk", "-args", ">=;1000;<;1900", "-skname", "a", "-skordinal", "0"};
    InnodbReaderBootstrap.main(args);
    List<String> output = SYS_OUT_INTERCEPTOR.getOutput();
    assertThat(output.size(), is(450));
    check(output, 500, 450);
  }

  @Test
  public void testQueryBySecondaryKeySpecifySkOrdinalNegate() {
    String[] args = {"-ibd-file-path", sourceIbdFilePath, "-create-table-sql-file-path", createTableSqlPath,
        "-c", "query-by-sk", "-args", ">=;1000;<;1900", "-skname", "a", "-skordinal", "1"};
    InnodbReaderBootstrap.main(args);
    List<String> output = SYS_OUT_INTERCEPTOR.getOutput();
    // Cannot compare aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa(java.lang.String) and 1000(java.lang.Long) for column b
    // java.lang.ClassCastException: java.lang.Long cannot be cast to java.lang.String
    assertThat(output.size(), is(0));
  }

  @Test
  public void testGenLsnHeatmap() throws IOException {
    String[] args = {"-ibd-file-path", sourceIbdFilePath, "-create-table-sql-file-path", createTableSqlPath,
        "-c", "gen-lsn-heatmap", "-args", "/tmp/lsn-heatmap-output.html 800 1000"};
    InnodbReaderBootstrap.main(args);
    List<String> fileContent = Files.readLines(new File("/tmp/lsn-heatmap-output.html"), Charset.defaultCharset());
    assertThat(fileContent.get(0), is("<head>"));
    assertThat(fileContent.get(1054).trim(), is("97"));
  }

  @Test
  public void testGenFillingRateHeatmap() throws IOException {
    String[] args = {"-ibd-file-path", sourceIbdFilePath, "-create-table-sql-file-path", createTableSqlPath,
        "-c", "gen-filling-rate-heatmap", "-args", "/tmp/lsn-filling-rate-output.html 800 1000"};
    InnodbReaderBootstrap.main(args);
    List<String> fileContent = Files.readLines(new File("/tmp/lsn-filling-rate-output.html"),
        Charset.defaultCharset());
    assertThat(fileContent.get(0), is("<head>"));
    assertThat(fileContent.get(1067).trim(), is("0.011"));
  }

  @Test
  public void testQueryAllBufferIO() throws IOException {
    testIOTemplate("/tmp/query-all-buffer-io.out", "buffer",
        EXPECTED_TB10_DATE_FILE_MD5, false);
  }

  @Test
  public void testQueryAllMmapIO() throws IOException {
    testIOTemplate("/tmp/query-all-mmap-io.out", "mmap",
        EXPECTED_TB10_DATE_FILE_MD5, false);
  }

  /**
   * enable dio in travis ci environment, please refer to <code>.travis.yml</code>
   */
  @Test
  public void testQueryAllDirectIO() throws IOException {
    if (System.getenv("support_dio") != null) {
      testIOTemplate("/tmp/query-all-direct-io.out", "direct",
          EXPECTED_TB10_DATE_FILE_MD5, false);
    } else {
      System.out.println("system does not support dio, so skip test");
    }
  }

  @Test
  public void testQueryAllShowHeader() throws IOException {
    testIOTemplate("/tmp/query-all-mmap-io.out", "mmap",
        EXPECTED_TB10_DATE_FILE_WITH_HEADER_MD5, true);
  }

  @Test
  public void testQueryAllCommaDelimiter() throws IOException {
    testIOTemplate("/tmp/query-all-mmap-io.out", "mmap",
        EXPECTED_TB10_DATE_FILE_WITH_COMMA_DELIMITER_MD5, false, ",");
  }

  @Test
  public void testGetAllIndexPageFillingRate() {
    String[] args = {"-ibd-file-path", sourceIbdFilePath, "-create-table-sql-file-path", createTableSqlPath,
        "-c", "get-all-index-page-filling-rate"};
    InnodbReaderBootstrap.main(args);
    List<String> output = SYS_OUT_INTERCEPTOR.getOutput();
    assertThat(output.size(), is(2));
    assertThat(output.get(1).startsWith("Index page filling rate is 0.81943"), is(true));
  }

  private void testIOTemplate(String outputFilePath, String ioMode,
                              String expectedFileMd5, boolean showHeader) throws IOException {
    testIOTemplate(outputFilePath, ioMode, expectedFileMd5, showHeader, "\t");
  }

  private void testIOTemplate(String outputFilePath, String ioMode,
                              String expectedFileMd5, boolean showHeader, String delimiter) throws IOException {
    String[] args;
    if (showHeader) {
      args = new String[]{"-ibd-file-path", sourceIbdFilePath, "-create-table-sql-file-path", createTableSqlPath,
          "-showheader", "-c", "query-all", "-o", outputFilePath, "-iomode", ioMode};
    } else {
      args = new String[]{"-ibd-file-path", sourceIbdFilePath, "-create-table-sql-file-path", createTableSqlPath,
          "-c", "query-all", "-o", outputFilePath, "-iomode", ioMode};
    }
    if (!"\t".equals(delimiter)) {
      String[] newArgs = Arrays.copyOf(args, args.length + 2);
      newArgs[args.length] = "-delimiter";
      newArgs[args.length + 1] = ",";
      args = newArgs;
    }
    InnodbReaderBootstrap.main(args);
    List<String> fileContent = Files.readLines(new File(outputFilePath), Charset.defaultCharset());
    if (showHeader) {
      assertThat(fileContent.size(), is(1001));
      assertThat(fileContent.get(0), is(Joiner.on(delimiter).join(new String[]{"id", "a", "b", "c"})));
      fileContent.remove(0);
    } else {
      assertThat(fileContent.size(), is(1000));
    }
    check(fileContent, 1, 1000, delimiter);
    String md5 = getFileMD5(outputFilePath);
    System.out.println(md5);
    assertThat(md5, is(expectedFileMd5));
  }

  private void check(List<String> output, int start, int count) {
    check(output, start, count, "\t", ImmutableList.of("id", "a", "b", "c"), false);
  }

  private void check(List<String> output, int start, int count, boolean desc) {
    check(output, start, count, "\t", ImmutableList.of("id", "a", "b", "c"), desc);
  }

  private void check(List<String> output, int start, int count, String delimiter) {
    check(output, start, count, delimiter, ImmutableList.of("id", "a", "b", "c"), false);
  }

  private void check(List<String> output, int start, int count, String delimiter, boolean desc) {
    check(output, start, count, delimiter, ImmutableList.of("id", "a", "b", "c"), desc);
  }

  private void check(List<String> output, int start, int count, List<String> projection) {
    check(output, start, count, "\t", projection, false);
  }

  private void check(List<String> output, int start, int count, List<String> projection, boolean desc) {
    check(output, start, count, "\t", projection, desc);
  }

  private void check(List<String> output, int start, int count, String delimiter, List<String> projection,
                     boolean desc) {
    int iStart = 0;
    int iEnd = 0;
    if (desc) {
      iStart = start + count - 1;
      iEnd = start;
    } else {
      iStart = start;
      iEnd = start + count;
    }
    int index = 0;
    for (int i = iStart; desc ? i >= iEnd : i < iEnd; ) {
      String[] array = output.get(index++).split(delimiter);
      assertThat(array[0], is(String.valueOf(i)));
      assertThat(array[1], is(projection.contains("a") ? String.valueOf(i * 2) : "null"));
      assertThat(array[2], is((projection.contains("b")
          ? StringUtils.repeat(String.valueOf((char) (97 + i % 26)), 32) : "null")));
      assertThat(array[3], is((projection.contains("c")
          ? StringUtils.repeat(String.valueOf((char) (97 + i % 26)), 512) : "null")));
      if (desc) {
        i--;
      } else {
        i++;
      }
    }
  }

  public static String getFileMD5(String filePath) throws IOException {
    try (InputStream is = java.nio.file.Files.newInputStream(Paths.get(filePath))) {
      return org.apache.commons.codec.digest.DigestUtils.md5Hex(is);
    }
  }

}
