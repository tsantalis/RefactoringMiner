package gui.webdiff.viewers.vanilla;

import com.github.gumtreediff.actions.Diff;
import com.github.gumtreediff.actions.TreeClassifier;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.utils.SequenceAlgorithms;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.refactoringminer.astDiff.models.ASTDiff;
import org.refactoringminer.astDiff.actions.classifier.ExtendedTreeClassifier;
import org.refactoringminer.astDiff.actions.model.MultiMove;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class VanillaDiffHtmlBuilder {

    private static final String SRC_MV_SPAN = "<span class=\"%s\" id=\"move-src-%d\" data-title=\"%s\">";
    private static final String DST_MV_SPAN = "<span class=\"%s\" id=\"move-dst-%d\" data-title=\"%s\">";
    private static final String ADD_DEL_SPAN = "<span class=\"%s\" data-title=\"%s\">";

    private static final String MoveIn_SPAN = "<span class=\"%s\" data-toggle=\"tooltip\" title=\"%s\">";

    private static final String MoveOut_SPAN = "<span class=\"%s\" data-toggle=\"tooltip\" title=\"%s\">";

    private static final String MM_SPAN = "<span class=\"%s\" gid=\"%x\" data-title=\"%s\">";

    private static final String UPD_SPAN = "<span class=\"cupd\">";
    private static final String ID_SPAN = "<span class=\"marker\" id=\"mapping-%d\"></span>";
    private static final String END_SPAN = "</span>";

    private String srcDiff;

    private String dstDiff;

    private ArrayList<Tree> srcMM = new ArrayList<>();
    private ArrayList<Tree> dstMM = new ArrayList<>();

    private String srcContent;

    private String dstContent;

    private Diff input;

    public VanillaDiffHtmlBuilder(String srcContent, String dstContent, Diff diff) {
        this.srcContent = srcContent;
        this.dstContent = dstContent;
        this.input = diff;
    }

    public void produce() throws IOException {
        TagIndex rtags = new TagIndex();
        TagIndex ltags = new TagIndex();
        if (input instanceof ASTDiff) {
            ASTDiff diff = (ASTDiff) input;
            ExtendedTreeClassifier c = (ExtendedTreeClassifier) diff.createRootNodesClassifier();
            Object2IntMap<Tree> mappingIds = new Object2IntOpenHashMap<>();

            int uId = 1;
            int mId = 1;


            for (Tree t : diff.src.getRoot().preOrder()) {
                if (c.getMovedSrcs().contains(t)) {
                    mappingIds.put(diff.getAllMappings().getDsts(t).iterator().next(), mId);
                    ltags.addStartTag(t.getPos(), String.format(ID_SPAN, uId++));
                    ltags.addTags(t.getPos(), String.format(
                            SRC_MV_SPAN, "token mv", mId++, tooltip(diff.src, t)), t.getEndPos(), END_SPAN);
                }
                else if (c.getUpdatedSrcs().contains(t)) {
                    mappingIds.put(diff.getAllMappings().getDsts(t).iterator().next(), mId);
                    ltags.addStartTag(t.getPos(), String.format(ID_SPAN, uId++));
                    ltags.addTags(t.getPos(), String.format(
                            SRC_MV_SPAN, "token upd", mId++, tooltip(diff.src, t)), t.getEndPos(), END_SPAN);
                    List<int[]> hunks = SequenceAlgorithms.hunks(t.getLabel(), diff.getAllMappings().getDsts(t).iterator().next().getLabel());
                    for (int[] hunk : hunks)
                        ltags.addTags(t.getPos() + hunk[0], UPD_SPAN, t.getPos() + hunk[1], END_SPAN);

                }
                else if (c.getDeletedSrcs().contains(t)) {
                    ltags.addStartTag(t.getPos(), String.format(ID_SPAN, uId++));
                    ltags.addTags(t.getPos(), String.format(
                            ADD_DEL_SPAN, "token del", tooltip(diff.src, t)), t.getEndPos(), END_SPAN);
                }
                else if (c.getMultiMapSrc().containsKey(t)) {
                    if (!srcMM.contains(t)) {
                        int gid = ((MultiMove) (c.getMultiMapSrc().get(t))).getGroupId();
                        ltags.addStartTag(t.getPos(), String.format(ID_SPAN, uId++));
                        boolean updated = ((MultiMove) (c.getMultiMapSrc().get(t))).isUpdated();
                        String htmlClass = "token mm";
                        if (updated) htmlClass += " updOnTop";
                        ltags.addTags(t.getPos(), String.format(
                                MM_SPAN, htmlClass, gid, tooltip(diff.src, t)), t.getEndPos(), END_SPAN);
                        srcMM.add(t);
                    }
                }
                else if (c.getSrcMoveOutTreeMap().containsKey(t)) {
                    ltags.addStartTag(t.getPos(), String.format(ID_SPAN, uId++));
                    ltags.addTags(t.getPos(), String.format(
                            MoveOut_SPAN, "token moveOut", c.getSrcMoveOutTreeMap().get(t).toString()), t.getEndPos(), END_SPAN);
                }
                else {
                    if (diff.getAllMappings().isSrcMapped(t)){
                        mappingIds.put(diff.getAllMappings().getDsts(t).iterator().next(), mId);
                        ltags.addStartTag(t.getPos(), String.format(ID_SPAN, uId++));
                        ltags.addTags(t.getPos(), String.format(
                                SRC_MV_SPAN, "token non", mId++, tooltip(diff.src, t)), t.getEndPos(), END_SPAN);
                    }
                }
            }
            for (Tree t : diff.dst.getRoot().preOrder()) {
                if (c.getMovedDsts().contains(t)) {
                    int dId = mappingIds.getInt(t);
                    rtags.addStartTag(t.getPos(), String.format(ID_SPAN, uId++));
                    rtags.addTags(t.getPos(), String.format(
                            DST_MV_SPAN, "token mv", dId, tooltip(diff.dst, t)), t.getEndPos(), END_SPAN);
                }
                else if (c.getUpdatedDsts().contains(t)) {
                    int dId = mappingIds.getInt(t);
                    rtags.addStartTag(t.getPos(), String.format(ID_SPAN, uId++));
                    rtags.addTags(t.getPos(), String.format(
                            DST_MV_SPAN, "token upd", dId, tooltip(diff.dst, t)), t.getEndPos(), END_SPAN);
                    List<int[]> hunks = SequenceAlgorithms.hunks(diff.getAllMappings().getSrcs(t).iterator().next().getLabel(), t.getLabel());
                    for (int[] hunk : hunks)
                        rtags.addTags(t.getPos() + hunk[2], UPD_SPAN, t.getPos() + hunk[3], END_SPAN);
                }
                else if (c.getInsertedDsts().contains(t)) {
                    rtags.addStartTag(t.getPos(), String.format(ID_SPAN, uId++));
                    rtags.addTags(t.getPos(), String.format(
                            ADD_DEL_SPAN, "token add", tooltip(diff.dst, t)), t.getEndPos(), END_SPAN);
                }
                else if (c.getMultiMapDst().containsKey(t)) {
                    if (!dstMM.contains(t)) {
                        int gid = ((MultiMove) (c.getMultiMapDst().get(t))).getGroupId();
                        boolean updated = ((MultiMove) (c.getMultiMapDst().get(t))).isUpdated();
                        rtags.addStartTag(t.getPos(), String.format(ID_SPAN, uId++));
                        String htmlClass = "token mm";
                        if (updated) htmlClass += " updOnTop";
                        rtags.addTags(t.getPos(), String.format(
                                MM_SPAN, htmlClass, gid, tooltip(diff.dst, t)), t.getEndPos(), END_SPAN);
                        dstMM.add(t);
                    }
                }
                else if (c.getDstMoveInTreeMap().containsKey(t)) {
                    rtags.addStartTag(t.getPos(), String.format(ID_SPAN, uId++));
                    rtags.addTags(t.getPos(), String.format(
                            MoveIn_SPAN, "token moveIn", c.getDstMoveInTreeMap().get(t).toString()), t.getEndPos(), END_SPAN);
                }
                else {
                    //no action associated with this subtree
                    if (diff.getAllMappings().isDstMapped(t)){
                        int dId = mappingIds.getInt(t);
                        rtags.addStartTag(t.getPos(), String.format(ID_SPAN, uId++));
                        rtags.addTags(t.getPos(), String.format(
                                DST_MV_SPAN, "token non", dId, tooltip(diff.dst, t)), t.getEndPos(), END_SPAN);
                    }
                }
            }
        }
        else{
            Diff diff = input;
            TreeClassifier c = diff.createRootNodesClassifier();
            Object2IntMap<Tree> mappingIds = new Object2IntOpenHashMap<>();

            int uId = 1;
            int mId = 1;

            for (Tree t: diff.src.getRoot().preOrder()) {
                if (c.getMovedSrcs().contains(t)) {
                    mappingIds.put(diff.mappings.getDstForSrc(t), mId);
                    ltags.addStartTag(t.getPos(), String.format(ID_SPAN, uId++));
                    ltags.addTags(t.getPos(), String.format(
                            SRC_MV_SPAN, "token mv", mId++, tooltip(diff.src, t)), t.getEndPos(), END_SPAN);
                }
                if (c.getUpdatedSrcs().contains(t)) {
                    mappingIds.put(diff.mappings.getDstForSrc(t), mId);
                    ltags.addStartTag(t.getPos(), String.format(ID_SPAN, uId++));
                    ltags.addTags(t.getPos(), String.format(
                            SRC_MV_SPAN, "token upd", mId++, tooltip(diff.src, t)), t.getEndPos(), END_SPAN);
                    List<int[]> hunks = SequenceAlgorithms.hunks(t.getLabel(), diff.mappings.getDstForSrc(t).getLabel());
                    for (int[] hunk: hunks)
                        ltags.addTags(t.getPos() + hunk[0], UPD_SPAN, t.getPos() + hunk[1], END_SPAN);

                }
                if (c.getDeletedSrcs().contains(t)) {
                    ltags.addStartTag(t.getPos(), String.format(ID_SPAN, uId++));
                    ltags.addTags(t.getPos(), String.format(
                            ADD_DEL_SPAN, "token del", tooltip(diff.src, t)), t.getEndPos(), END_SPAN);
                }
            }

            for (Tree t: diff.dst.getRoot().preOrder()) {
                if (c.getMovedDsts().contains(t)) {
                    int dId = mappingIds.getInt(t);
                    rtags.addStartTag(t.getPos(), String.format(ID_SPAN, uId++));
                    rtags.addTags(t.getPos(), String.format(
                            DST_MV_SPAN, "token mv", dId, tooltip(diff.dst, t)), t.getEndPos(), END_SPAN);
                }
                if (c.getUpdatedDsts().contains(t)) {
                    int dId = mappingIds.getInt(t);
                    rtags.addStartTag(t.getPos(), String.format(ID_SPAN, uId++));
                    rtags.addTags(t.getPos(), String.format(
                            DST_MV_SPAN, "token upd", dId, tooltip(diff.dst, t)), t.getEndPos(), END_SPAN);
                    List<int[]> hunks = SequenceAlgorithms.hunks(diff.mappings.getSrcForDst(t).getLabel(), t.getLabel());
                    for (int[] hunk: hunks)
                        rtags.addTags(t.getPos() + hunk[2], UPD_SPAN, t.getPos() + hunk[3], END_SPAN);
                }
                if (c.getInsertedDsts().contains(t)) {
                    rtags.addStartTag(t.getPos(), String.format(ID_SPAN, uId++));
                    rtags.addTags(t.getPos(), String.format(
                            ADD_DEL_SPAN, "token add", tooltip(diff.dst, t)), t.getEndPos(), END_SPAN);
                }
            }
        }

        StringWriter w1 = new StringWriter();
//        BufferedReader r = Files.newBufferedReader(fSrc.toPath(), Charset.forName("UTF-8"));
        Reader inputString = new StringReader(srcContent);
        BufferedReader r = new BufferedReader(inputString);
        int cursor = 0;
        for (char cr : srcContent.toCharArray())
        {
            w1.append(ltags.getEndTags(cursor));
            w1.append(ltags.getStartTags(cursor));
            append(cr, w1);
            cursor++;
        }
        w1.append(ltags.getEndTags(cursor));
        r.close();
        srcDiff = w1.toString();

        StringWriter w2 = new StringWriter();
//        r = Files.newBufferedReader(fDst.toPath(), Charset.forName("UTF-8"));
        inputString = new StringReader(dstContent);
        r = new BufferedReader(inputString);
        cursor = 0;

        for (char cr : dstContent.toCharArray())
        {
            w2.append(rtags.getEndTags(cursor));
            w2.append(rtags.getStartTags(cursor));
            append(cr, w2);
            cursor++;
        }
        w2.append(rtags.getEndTags(cursor));
        r.close();

        dstDiff = w2.toString();
    }

    public String getSrcDiff() {
        return srcDiff;
    }

    public String getDstDiff() {
        return dstDiff;
    }

    private static String tooltip(TreeContext ctx, Tree t) {
        return (t.getParent() != null)
                ? t.getParent().getType() + "/" + t.getType() : t.getType().toString();
    }

    private static void append(char cr, Writer w) throws IOException {
        if (cr == '<') w.append("&lt;");
        else if (cr == '>') w.append("&gt;");
        else if (cr == '&') w.append("&amp;");
        else w.append(cr);
    }

    private static class TagIndex {

        private Map<Integer, List<String>> startTags;

        private Map<Integer, List<String>> endTags;

        public TagIndex() {
            startTags = new HashMap<Integer, List<String>>();
            endTags = new HashMap<Integer, List<String>>();
        }

        public void addTags(int pos, String startTag, int endPos, String endTag) {
            addStartTag(pos, startTag);
            addEndTag(endPos, endTag);
        }

        public void addStartTag(int pos, String tag) {
            if (!startTags.containsKey(pos)) startTags.put(pos, new ArrayList<String>());
            startTags.get(pos).add(tag);
        }

        public void addEndTag(int pos, String tag) {
            if (!endTags.containsKey(pos)) endTags.put(pos, new ArrayList<String>());
            endTags.get(pos).add(tag);
        }

        public String getEndTags(int pos) {
            if (!endTags.containsKey(pos)) return "";
            StringBuilder b = new StringBuilder();
            for (String s: endTags.get(pos)) b.append(s);
            return b.toString();
        }

        public String getStartTags(int pos) {
            if (!startTags.containsKey(pos))
                return "";
            StringBuilder b = new StringBuilder();
            for (String s: startTags.get(pos))
                b.append(s);
            return b.toString();
        }

    }
}
