package gui.webdiff;

import com.github.gumtreediff.actions.Diff;
import org.rendersnake.DocType;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import static org.rendersnake.HtmlAttributesFactory.*;

public class VanillaDiffView implements Renderable {
    private VanillaDiffHtmlBuilder rawHtmlDiff;
    private String toolName;
    private String srcFileName;
    private String dstFileName;
    private int id;
    private int numOfDiffs;
    private boolean dump;

    public VanillaDiffView(String toolName, String srcFileName, String dstFileName, String srcFileContent, String dstFileContent, Diff diff, int id, boolean dump, int numOfDiffs) throws IOException {
        this.toolName = toolName;
        this.srcFileName = srcFileName;
        this.dstFileName = dstFileName;
        this.dump = dump;
        this.id = id;
        this.numOfDiffs = numOfDiffs;
        rawHtmlDiff = new VanillaDiffHtmlBuilder(srcFileContent, dstFileContent, diff);
        rawHtmlDiff.produce();
    }

    @Override
    public void renderOn(HtmlCanvas html) throws IOException {
        html
        .render(DocType.HTML5)
        .html(lang("en"))
            .render(new Header(dump))
            .body()
                .div(class_("container-fluid"))
                    .div(class_("row"))
                        .render(new MenuBar(toolName, id, numOfDiffs))
                    ._div()
                    .div(class_("row"))
                        .div(class_("col-6"))
                            .h5().content(srcFileName)
                            .pre(class_("pre-scrollable").id("left")).content(rawHtmlDiff.getSrcDiff(), false)
                        ._div()
                        .div(class_("col-6"))
                            .h5().content(dstFileName)
                            .pre(class_("pre-scrollable").id("right")).content(rawHtmlDiff.getDstDiff(), false)
                        ._div()
                    ._div()
                ._div()
            ._body()
        ._html();
    }
    private static class Header implements Renderable {

        private boolean dump;

        public Header(boolean dump) throws IOException {
            this.dump = dump;
        }

        @Override
        public void renderOn(HtmlCanvas html) throws IOException {
            if (!dump) {
                html
                        .head()
                           .meta(charset("utf8"))
                           .meta(name("viewport").content("width=device-width, initial-scale=1.0"))
                           .title().content("RefactoringMiner")
                           .macros().stylesheet(WebDiff.BOOTSTRAP_CSS_URL)
                           .macros().stylesheet("/dist/vanilla.css")
                           .macros().javascript(WebDiff.JQUERY_JS_URL)
                           .macros().javascript(WebDiff.BOOTSTRAP_JS_URL)
                           .macros().javascript("/dist/shortcuts.js")
                           .macros().javascript("/dist/vanilla.js")
                        ._head();
            }
            else {
                html
                        .head()
                           .meta(charset("utf8"))
                           .meta(name("viewport").content("width=device-width, initial-scale=1.0"))
                           .title().content("RefactoringMiner")
                           .macros().stylesheet(WebDiff.BOOTSTRAP_CSS_URL)
                           .style(type("text/css"))
                           .write(readFile("web/dist/vanilla.css"))
                           ._style()
                           .macros().javascript(WebDiff.JQUERY_JS_URL)
                           .macros().javascript(WebDiff.BOOTSTRAP_JS_URL)
                           .macros().script(readFile("web/dist/shortcuts.js"))
                           .macros().script(readFile("web/dist/vanilla.js"))
                        ._head();
            }
        }

        private static String readFile(String resourceName)  throws IOException {
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            InputStream inputStream = classloader.getResourceAsStream(resourceName);
            InputStreamReader streamReader = new InputStreamReader(inputStream, Charset.defaultCharset());
            BufferedReader reader = new BufferedReader(streamReader);
            String content = "";
            for (String line; (line = reader.readLine()) != null;) {
                content += line + "\n";
            }
            return content;
        }
    }
}
