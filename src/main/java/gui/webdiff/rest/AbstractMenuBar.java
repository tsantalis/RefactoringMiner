package gui.webdiff.rest;

import org.refactoringminer.astDiff.models.DiffMetaInfo;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.rendersnake.HtmlAttributesFactory.class_;
import static org.rendersnake.HtmlAttributesFactory.href;

/* Created by pourya on 2024-06-03*/
public abstract class AbstractMenuBar implements Renderable {
    private final String toolName;
    private final int id;
    private final int numOfDiffs;
    private final String routePath;
    private final DiffMetaInfo metaInfo;
    private final List<String> deletedFilePaths;
    private final List<String> addedFilePaths;
    private final String filePath;
    private final boolean isMovedDiff;
    private static final String BACK_BUTTON_TEXT = "Overview";
    private static final String PREV_BUTTON_TEXT = "Prev";
    private static final String NEXT_BUTTON_TEXT = "Next";
    private static final String QUIT_BUTTON_TEXT = "Quit";

    private String getPrevButtonText() {
        String txt = PREV_BUTTON_TEXT;
        if (id != -1) {
            int rem = id;
            return txt + " (" + rem + " remaining)";
        }
        int deletedIndex = deletedFilePaths.indexOf(filePath);
        if (deletedIndex != -1) {
            int rem = deletedIndex + numOfDiffs;
            return txt + " (" + rem + " remaining)";
        }
        int addedIndex = addedFilePaths.indexOf(filePath);
        int rem = addedIndex + deletedFilePaths.size() + numOfDiffs;
        return txt + " (" + rem + " remaining)";
    }
    private String getNextButtonText() {
        String txt = NEXT_BUTTON_TEXT;
        if (id != -1) {
            int rem = numOfDiffs - id - 1 + deletedFilePaths.size() + addedFilePaths.size();
            return txt + " (" + rem + " remaining)";
        }
        int deletedIndex = deletedFilePaths.indexOf(filePath);
        if (deletedIndex != -1) {
            int rem = deletedFilePaths.size() - deletedIndex - 1 + addedFilePaths.size();
            return txt + " (" + rem + " remaining)";
        }
        int addedIndex = addedFilePaths.indexOf(filePath);
        int rem = addedFilePaths.size() - addedIndex - 1;
        return txt + " (" + rem + " remaining)";
    }

    private String getNextHRef(){
        if (id != -1) {
            if (id == numOfDiffs -1) {
                if (deletedFilePaths.size() > 0) {
                    String nextFilePath = deletedFilePaths.get(0);
                    return "/content?side=deleted&path=" + URLEncoder.encode(nextFilePath, StandardCharsets.UTF_8);
                }
                else if(addedFilePaths.size() > 0) {
                    String nextFilePath = addedFilePaths.get(0);
                    return "/content?side=added&path=" + URLEncoder.encode(nextFilePath, StandardCharsets.UTF_8);
                }
            }
            return routePath + (id + 1) % numOfDiffs;
        }
        int deletedIndex = deletedFilePaths.indexOf(filePath);
        if (deletedIndex != -1) {
            if(deletedIndex < deletedFilePaths.size()-1) {
                String nextFilePath = deletedFilePaths.get(deletedIndex+1);
                return routePath + "?side=deleted&path=" + URLEncoder.encode(nextFilePath, StandardCharsets.UTF_8);
            }
            else if(addedFilePaths.size() > 0) {
                String nextFilePath = addedFilePaths.get(0);
                return routePath + "?side=added&path=" + URLEncoder.encode(nextFilePath, StandardCharsets.UTF_8);
            }
        }
        int addedIndex = addedFilePaths.indexOf(filePath);
        if (addedIndex < addedFilePaths.size()-1) {
            String nextFilePath = addedFilePaths.get(addedIndex+1);
            return routePath + "?side=added&path=" + URLEncoder.encode(nextFilePath, StandardCharsets.UTF_8);
        }
        return "";
    }
    private String getPrevHRef(){
        if (id != -1) {
            return routePath + (id - 1 + numOfDiffs) % numOfDiffs;
        }
        int deletedIndex = deletedFilePaths.indexOf(filePath);
        if (deletedIndex != -1) {
            if(deletedIndex == 0) {
                return "/monaco-page/" + String.valueOf(numOfDiffs-1);
            }
            else if(deletedIndex > 0) {
                String prevFilePath = deletedFilePaths.get(deletedIndex-1);
                return routePath + "?side=deleted&path=" + URLEncoder.encode(prevFilePath, StandardCharsets.UTF_8);
            }
        }
        int addedIndex = addedFilePaths.indexOf(filePath);
        if(addedIndex == 0 && deletedFilePaths.size() > 0) {
             String prevFilePath = deletedFilePaths.get(deletedFilePaths.size()-1);
             return routePath + "?side=deleted&path=" + URLEncoder.encode(prevFilePath, StandardCharsets.UTF_8);
        }
        else if(addedIndex == 0 && deletedFilePaths.size() == 0) {
            return "/monaco-page/" + String.valueOf(numOfDiffs-1);
        }
        else if (addedIndex > 0) {
            String prevFilePath = addedFilePaths.get(addedIndex-1);
            return routePath + "?side=added&path=" + URLEncoder.encode(prevFilePath, StandardCharsets.UTF_8);
        }
        return "";
    }

    public AbstractMenuBar(String toolName, String routePath, int id, int numOfDiffs, DiffMetaInfo metaInfo, List<String> deletedFilePaths, List<String> addedFilePaths, boolean isMovedDiff) {
        this.toolName = toolName;
        this.routePath = routePath;
        this.id = id;
        this.numOfDiffs = numOfDiffs;
        this.metaInfo = metaInfo;
        this.deletedFilePaths = deletedFilePaths;
        this.addedFilePaths = addedFilePaths;
        this.filePath = null;
        this.isMovedDiff = isMovedDiff;
    }

    public AbstractMenuBar(String toolName, String routePath, String filePath, int numOfDiffs, DiffMetaInfo metaInfo, List<String> deletedFilePaths, List<String> addedFilePaths) {
        this.toolName = toolName;
        this.routePath = routePath;
        this.id = -1;
        this.numOfDiffs = numOfDiffs;
        this.metaInfo = metaInfo;
        this.deletedFilePaths = deletedFilePaths;
        this.addedFilePaths = addedFilePaths;
        this.filePath = filePath;
        this.isMovedDiff = false;
    }

    @Override
    public void renderOn(HtmlCanvas html) throws IOException {
        boolean shouldDisablePrev = id == 0;
        boolean shouldDisableNext = false;
        if (id != -1) {
            shouldDisableNext = id == numOfDiffs + deletedFilePaths.size() + addedFilePaths.size() - 1;
        }
        else if(filePath != null) {
            if(addedFilePaths.size() > 0 && addedFilePaths.contains(filePath)) {
                shouldDisableNext = addedFilePaths.indexOf(filePath) == addedFilePaths.size() - 1;
            }
            else if(deletedFilePaths.size() > 0 && deletedFilePaths.contains(filePath) && addedFilePaths.size() == 0) {
                shouldDisableNext = deletedFilePaths.indexOf(filePath) == deletedFilePaths.size() - 1;
            }
        }
        html
                .div(class_("col"))
                    .write("Generated by " + toolName + " | ")
                    .if_(metaInfo != null)
                    .a(href(metaInfo.getUrl()).target("_blank")).content(metaInfo.getInfo())
                    ._if()
                    .if_(isMovedDiff)
                    .write(" | " + "<strong>This diff shows moved code between different files</strong>", false)
                    ._if()
                ._div()
                .div(class_("col"))
                .div(class_("btn-toolbar justify-content-end"))
                .div(class_("btn-group mr-2"))
                .button(class_("btn btn-primary btn-sm").id("legend")
                        .add("data-bs-container", "body")
                        .add("data-bs-toggle", "popover")
                        .add("data-bs-placement", "bottom")
                        .add("data-bs-html", "true")
                        .add("data-bs-content", getLegendValue(), false)
                ).content("Legend")
                .button(class_("btn btn-primary btn-sm").id("shortcuts")
                        .add("data-bs-toggle", "popover")
                        .add("data-bs-placement", "bottom")
                        .add("data-bs-html", "true")
                        .add("data-bs-content", getShortcutDescriptions(), false)
                )
                .content("Shortcuts")
                ._div()
                .div(class_("btn-group"))
                .a(class_("btn btn-default btn-sm btn-primary").href("/list")).content(BACK_BUTTON_TEXT)
                .if_(id >= 0 || filePath != null)
                .a(class_("btn btn-default btn-sm btn-primary" + (shouldDisablePrev ? " disabled" : ""))
                        .href(shouldDisablePrev ? "#" : getPrevHRef()))
                .content(getPrevButtonText())
                .a(class_("btn btn-default btn-sm btn-primary" + (shouldDisableNext ? " disabled" : ""))
                        .href(shouldDisableNext ? "#" : getNextHRef()))
                .content(getNextButtonText())
                ._if()
                //.a(class_("btn btn-default btn-sm btn-danger").href("/quit")).content(QUIT_BUTTON_TEXT)
                ._div()
                ._div()
                ._div();
    }

    public abstract String getLegendValue() ;

    public String getShortcutDescriptions() {
        String OS = System.getProperty("os.name");
        String alt = OS.contains("Mac") ? "⌥" : "Alt";
        return "<b>" + alt + " + q</b> quit<br><b>" + alt + " + l</b> list<br>"
             + "<b>" + alt + " + t</b> top<br><b>" + alt + " + b</b> bottom <br>";
    }
}
