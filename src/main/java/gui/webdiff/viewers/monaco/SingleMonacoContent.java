package gui.webdiff.viewers.monaco;

import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import java.io.IOException;

public class SingleMonacoContent implements Renderable {

    private final boolean isAdded;
    private final String path;
    private final String escapedContent;

    public SingleMonacoContent(boolean isAdded, String path, String escapedContent) {
        this.isAdded = isAdded;
        this.path = path;
        this.escapedContent = escapedContent;
    }

    @Override
    public void renderOn(HtmlCanvas htmlCanvas) throws IOException {
        htmlCanvas.write(singleMonaco(isAdded, path, escapedContent));
    }

    private static String singleMonaco(boolean isAdded, String path, String escapedContent) {
        String boxColor = isAdded ? "#d4edda" : "#f8d7da";  // Green or red
        String textColor = isAdded ? "#155724" : "#721c24"; // Dark green or dark red
        String borderColor = isAdded ? "#c3e6cb" : "#f5c6cb";

        String headerHtml = "<div style=\"background-color:" + boxColor + ";" +
                            "color:" + textColor + ";" +
                            "border: 1px solid " + borderColor + ";" +
                            "padding: 10px; border-radius: 5px; font-weight: bold;\">" +
                            path +
                            "</div>";

        return "<div style=\"padding:10px\">" +
               headerHtml +
               "<pre style=\"white-space: pre-wrap; background:#f8f8f8; padding:10px; border-radius:5px; margin-top:10px;\">" +
               escapedContent +
               "</pre>" +
               "</div>";
    }
}

