package sortpom.processinstruction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import refutils.ReflectionHelper;

import java.util.ArrayList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author bjorn
 * @since 2013-12-28
 */
class IgnoredSectionsStoreTest {

    private IgnoredSectionsStore ignoredSectionsStore;
    private ArrayList<String> ignoredSections;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        ignoredSectionsStore = new IgnoredSectionsStore();
        ignoredSections = new ReflectionHelper(ignoredSectionsStore).getField(ArrayList.class);
    }

    @Test
    void replaceNoSectionShouldReturnSameXml() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n" +
                "  <artifactId>sortpom</artifactId>\n" +
                "  <description name=\"pelle\" id=\"id\" other=\"övrigt\">Här använder vi åäö</description>\n" +
                "  <groupId>sortpom</groupId>\n" +
                "  <modelVersion>4.0.0</modelVersion>\n" +
                "  <name>SortPom</name>\n" +
                "  <!-- Egenskaper för projektet -->\n" +
                "  <properties>\n" +
                "    <compileSource>1.6</compileSource>\n" +
                "    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>\n" +
                "  </properties>\n" +
                "  <reporting />\n" +
                "  <version>1.0.0-SNAPSHOT</version>\n" +
                "</project>";
        String replaced = ignoredSectionsStore.replaceIgnoredSections(xml);

        assertThat(replaced, is(xml));
        assertThat(ignoredSections.size(), is(0));
    }

    @Test
    void replaceOneSectionShouldCreateOneToken() {
        String xml = "abc<?sortpom ignore?>def<?sortpom resume?>cba";
        String replaced = ignoredSectionsStore.replaceIgnoredSections(xml);

        assertThat(replaced, is("abc<?sortpom token='0'?>cba"));
        assertThat(ignoredSections.size(), is(1));
        assertThat(ignoredSections.get(0), is("<?sortpom ignore?>def<?sortpom resume?>"));
    }

    @Test
    void replaceMultipleSectionShouldCreateManyTokens() {
        String xml = "abc<?sortpom ignore?>def1<?sortpom resume?>cbaabc<?SORTPOM Ignore?>def2<?sortPom reSUME?>cba";
        String replaced = ignoredSectionsStore.replaceIgnoredSections(xml);

        assertThat(replaced, is("abc<?sortpom token='0'?>cbaabc<?sortpom token='1'?>cba"));
        assertThat(ignoredSections.size(), is(2));
        assertThat(ignoredSections.get(0), is("<?sortpom ignore?>def1<?sortpom resume?>"));
        assertThat(ignoredSections.get(1), is("<?SORTPOM Ignore?>def2<?sortPom reSUME?>"));
    }

    @Test
    void replaceMultipleLineXmlShouldCreateManyTokens() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n" +
                "\n" +
                "  <modelVersion>4.0.0</modelVersion>\n" +
                "\n" +
                "  <!-- Basics -->\n" +
                "    <groupId>something</groupId>\n" +
                "  <artifactId>maven-sortpom-sorter</artifactId>\n" +
                "    <version>1.0</version>\n" +
                "  <packaging>jar</packaging>\n" +
                "  <name>SortPom Sorter</name>\n" +
                "  <description>The sorting functionality</description>\n" +
                "\n" +
                "  <dependencies>\n" +
                "    <dependency>\n" +
                "      <groupId>org.jdom</groupId>\n" +
                "      <artifactId>jdom</artifactId>\n" +
                "      <version>1.1.3</version>\n" +
                "    </dependency>\n" +
                "    <dependency>\n" +
                "      <groupId>junit</groupId>\n" +
                "      <artifactId>junit</artifactId>\n" +
                "        <?sortpom ignore?>\n" +
                "      <version>4.11</version><!--$NO-MVN-MAN-VER$ -->\n" +
                "        <?sortpom resume?>\n" +
                "      <scope>test</scope>\n" +
                "    </dependency>\n" +
                "    <dependency>\n" +
                "      <groupId>commons-io</groupId>\n" +
                "      <artifactId>commons-io</artifactId>\n" +
                "        <?sortpom ignore?>\n" +
                "      <version>2.1</version><!--$NO-MVN-MAN-VER$ -->\n" +
                "        <?sortpom resume?>\n" +
                "    </dependency>\n" +
                "\n" +
                "    <!-- Test dependencies -->\n" +
                "    <dependency>\n" +
                "      <groupId>com.google.code.reflection-utils</groupId>\n" +
                "      <artifactId>reflection-utils</artifactId>\n" +
                "      <version>0.0.1</version>\n" +
                "      <scope>test</scope>\n" +
                "    </dependency>\n" +
                "  </dependencies>\n" +
                "\n" +
                "</project>\n";
        ignoredSectionsStore.replaceIgnoredSections(xml);

        assertThat(ignoredSections.size(), is(2));
        assertThat(ignoredSections.get(0), is("<?sortpom ignore?>\n" +
                "      <version>4.11</version><!--$NO-MVN-MAN-VER$ -->\n" +
                "        <?sortpom resume?>"));
        assertThat(ignoredSections.get(1), is("<?sortpom ignore?>\n" +
                "      <version>2.1</version><!--$NO-MVN-MAN-VER$ -->\n" +
                "        <?sortpom resume?>"));
    }

    @Test
    void revertTokensInOrderShouldWork() {
        String xml = "abc<?sortpom token='0'?>cbaabc<?sortpom token='1'?>cba";
        ignoredSections.add("<?sortpom ignore?>def1<?sortpom resume?>");
        ignoredSections.add("<?SORTPOM Ignore?>def2<?sortPom reSUME?>");

        String replaced = ignoredSectionsStore.revertIgnoredSections(xml);
        assertThat(replaced, is("abc<?sortpom ignore?>def1<?sortpom resume?>cbaabc<?SORTPOM Ignore?>def2<?sortPom reSUME?>cba"));
    }

    @Test
    void revertTokensInRearrangedOrderShouldPlaceTextInRightOrder() {
        String xml = "abc<?sortpom token='1'?>cbaabc<?sortpom token='0'?>cba";
        ignoredSections.add("<?sortpom ignore?>def0<?sortpom resume?>");
        ignoredSections.add("<?SORTPOM Ignore?>def1<?sortPom reSUME?>");

        String replaced = ignoredSectionsStore.revertIgnoredSections(xml);
        assertThat(replaced, is("abc<?SORTPOM Ignore?>def1<?sortPom reSUME?>cbaabc<?sortpom ignore?>def0<?sortpom resume?>cba"));
    }

    @Test
    void revertTokensInMultipleLinesShouldPlaceTextInRightOrder() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n" +
                "\n" +
                "  <modelVersion>4.0.0</modelVersion>\n" +
                "\n" +
                "  <!-- Basics -->\n" +
                "  <groupId>something</groupId>\n" +
                "  <artifactId>maven-sortpom-sorter</artifactId>\n" +
                "  <version>1.0</version>\n" +
                "  <packaging>jar</packaging>\n" +
                "\n" +
                "  <dependencies>\n" +
                "    <dependency>\n" +
                "      <groupId>commons-io</groupId>\n" +
                "      <artifactId>commons-io</artifactId>\n" +
                "      <?sortpom token='1'?>\n" +
                "    </dependency>\n" +
                "    <dependency>\n" +
                "      <groupId>org.jdom</groupId>\n" +
                "      <artifactId>jdom</artifactId>\n" +
                "      <version>1.1.3</version>\n" +
                "    </dependency>\n" +
                "\n" +
                "    <!-- Test dependencies -->\n" +
                "    <dependency>\n" +
                "      <groupId>com.google.code.reflection-utils</groupId>\n" +
                "      <artifactId>reflection-utils</artifactId>\n" +
                "      <version>0.0.1</version>\n" +
                "      <scope>test</scope>\n" +
                "    </dependency>\n" +
                "    <dependency>\n" +
                "      <groupId>junit</groupId>\n" +
                "      <artifactId>junit</artifactId>\n" +
                "      <?sortpom token='0'?>\n" +
                "      <scope>test</scope>\n" +
                "    </dependency>\n" +
                "  </dependencies>\n" +
                "  <name>SortPom Sorter</name>\n" +
                "  <description>The sorting functionality</description>\n" +
                "\n" +
                "</project>\n";
        ignoredSections.add("<?sortpom ignore?>\n" +
                "      <version>4.11</version><!--$NO-MVN-MAN-VER$ -->\n" +
                "        <?sortpom resume?>");
        ignoredSections.add("<?sortpom ignore?>\n" +
                "      <version>2.1</version><!--$NO-MVN-MAN-VER$ -->\n" +
                "        <?sortpom resume?>");

        String replaced = ignoredSectionsStore.revertIgnoredSections(xml);
        assertThat(replaced, is("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n" +
                "\n" +
                "  <modelVersion>4.0.0</modelVersion>\n" +
                "\n" +
                "  <!-- Basics -->\n" +
                "  <groupId>something</groupId>\n" +
                "  <artifactId>maven-sortpom-sorter</artifactId>\n" +
                "  <version>1.0</version>\n" +
                "  <packaging>jar</packaging>\n" +
                "\n" +
                "  <dependencies>\n" +
                "    <dependency>\n" +
                "      <groupId>commons-io</groupId>\n" +
                "      <artifactId>commons-io</artifactId>\n" +
                "      <?sortpom ignore?>\n" +
                "      <version>2.1</version><!--$NO-MVN-MAN-VER$ -->\n" +
                "        <?sortpom resume?>\n" +
                "    </dependency>\n" +
                "    <dependency>\n" +
                "      <groupId>org.jdom</groupId>\n" +
                "      <artifactId>jdom</artifactId>\n" +
                "      <version>1.1.3</version>\n" +
                "    </dependency>\n" +
                "\n" +
                "    <!-- Test dependencies -->\n" +
                "    <dependency>\n" +
                "      <groupId>com.google.code.reflection-utils</groupId>\n" +
                "      <artifactId>reflection-utils</artifactId>\n" +
                "      <version>0.0.1</version>\n" +
                "      <scope>test</scope>\n" +
                "    </dependency>\n" +
                "    <dependency>\n" +
                "      <groupId>junit</groupId>\n" +
                "      <artifactId>junit</artifactId>\n" +
                "      <?sortpom ignore?>\n" +
                "      <version>4.11</version><!--$NO-MVN-MAN-VER$ -->\n" +
                "        <?sortpom resume?>\n" +
                "      <scope>test</scope>\n" +
                "    </dependency>\n" +
                "  </dependencies>\n" +
                "  <name>SortPom Sorter</name>\n" +
                "  <description>The sorting functionality</description>\n" +
                "\n" +
                "</project>\n"));
    }

}
