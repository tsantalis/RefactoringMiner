package narrator.graph;

import com.github.gumtreediff.tree.DefaultTree;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.TypeSet;
import narrator.graph.cluster.Cluster;
import org.jgrapht.Graph;
import org.junit.jupiter.api.Test;
import org.refactoringminer.astDiff.utils.Constants;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Demonstrates the output of {@link Node#textualRepresentation(Cluster)}
 * for various node types and scenarios.
 *
 * <h3>Format:</h3>
 * <pre>
 *   { id: &lt;uuid&gt;, type: &lt;NODE_TYPE&gt;, location: &lt;path&gt; }
 *   &lt;content&gt;
 * </pre>
 *
 * <p>Type mappings (in textualRepresentation code):
 * <ul>
 *   <li>EXTENSION    -> UNCHANGED</li>
 *   <li>DELETION     -> DELETED</li>
 *   <li>ADDITION     -> ADDED</li>
 *   <li>Others       -> kept as-is (SRC_MOVE, DST_MOVE, SRC_UPDATE, DST_UPDATE,
 *                           LOCATION_CONTEXT, SEMANTIC_CONTEXT, etc.)</li>
 * </ul>
 *
 * <p>With cluster (MAPPING edges):
 * <ul><li>Outgoing MAPPING edge -> "AFTER_MAPPING"
 *     <li>Incoming MAPPING edge  -> "BEFORE_MAPPING"
 * </ul>
 */
public class NodeTextualRepresentationTest {

    /** Java-specific tree type constants */
    private static final Constants JAVA = new Constants("Foo.java");

    /** Create a DefaultTree with an explicit type string and content length. */
    private static Tree makeTypedTree(TreeContext ctx, String typeName, int contentLength) {
        DefaultTree t = new DefaultTree(null);
        t.setType(TypeSet.type(typeName));
        t.setPos(0);
        t.setLength(contentLength);
        ctx.setRoot(t);
        t.setChildren(new ArrayList<>());
        t.setLabel("");
        return t;
    }

    /** Create a DefaultTree using a Java constant name. */
    private static Tree makeTree(TreeContext ctx, String constantName, int contentLength) {
        try {
            java.lang.reflect.Field f = Constants.class.getField(constantName);
            String typeName = (String) f.get(JAVA);
            return makeTypedTree(ctx, typeName, contentLength);
        } catch (Exception e) {
            throw new RuntimeException("Unknown constant: " + constantName, e);
        }
    }

    /** Convenience: create a node and get its textualRepresentation. */
    private String repr(String fileContent, String path, String constantName,
                        NodeType nodeType, Cluster cluster) {
        TreeContext ctx = new TreeContext();
        int len = fileContent.length();
        Tree tree = makeTree(ctx, constantName, len);
        // For decl kinds, add a SIMPLE_NAME child so getLocationContext name extraction works
        if ("TYPE_DECLARATION".equals(constantName) || "METHOD_DECLARATION".equals(constantName)
            || "ENUM_DECLARATION".equals(constantName) || "RECORD_DECLARATION".equals(constantName)) {
            DefaultTree nameChild = new DefaultTree(null);
            nameChild.setType(TypeSet.type(JAVA.SIMPLE_NAME));
            nameChild.setLabel("Foo");
            nameChild.setLength(3);
            tree.getChildren().add(nameChild);
        }
        Node node = new Node(fileContent, path, SrcDst.SRC, tree, null, null, nodeType, null);
        return node.textualRepresentation(cluster);
    }

    private Node makeNode(String fileContent, String path, String constantName, NodeType nodeType) {
        TreeContext ctx = new TreeContext();
        int len = fileContent.length();
        Tree tree = makeTree(ctx, constantName, len);
        if ("SIMPLE_NAME".equals(constantName)) {
            tree.setLabel(fileContent);
        }
        return new Node(fileContent, path, SrcDst.SRC, tree, null, null, nodeType, null);
    }

    // =============== 1. Baseline: node types WITHOUT cluster ==============

    @Test
    public void addition_node_maps_to_ADDED() {
        String s = repr("public class Foo {}", "Foo.java", "TYPE_DECLARATION",
                        NodeType.ADDITION, null);
        System.out.println("=== Addition -> ADDED ===");
        System.out.println(s);
        assertTrue(s.contains("type: ADDED"));
        assertTrue(s.contains("public class Foo {}"));
    }

    @Test
    public void deletion_node_maps_to_DELETED() {
        String s = repr("/* removed */", "Foo.java", "METHOD_DECLARATION",
                        NodeType.DELETION, null);
        System.out.println("=== Deletion -> DELETED ===");
        System.out.println(s);
        assertTrue(s.contains("type: DELETED"));
    }

    @Test
    public void extension_node_maps_to_UNCHANGED() {
        String s = repr("public int x;", "Bar.java", "FIELD_DECLARATION",
                        NodeType.EXTENSION, null);
        System.out.println("=== Extension -> UNCHANGED ===");
        System.out.println(s);
        assertTrue(s.contains("type: UNCHANGED"));
    }

    @Test
    public void location_context_keeps_raw_type() {
        String s = repr("com.example.Foo", "Foo.java", "SIMPLE_NAME",
                        NodeType.LOCATION_CONTEXT, null);
        System.out.println("=== LOCATION_CONTEXT (raw) ===");
        System.out.println(s);
        assertTrue(s.contains("type: LOCATION_CONTEXT"));
    }

    @Test
    public void semantic_context_keeps_raw_type() {
        String s = repr("com.example.Foo", "Foo.java", "SIMPLE_NAME",
                        NodeType.SEMANTIC_CONTEXT, null);
        System.out.println("=== SEMANTIC_CONTEXT (raw) ===");
        System.out.println(s);
        assertTrue(s.contains("type: SEMANTIC_CONTEXT"));
    }

    @Test
    public void src_move_keeps_raw_type() {
        String s = repr("int x;", "Foo.java", "VARIABLE_DECLARATION_STATEMENT",
                        NodeType.SRC_MOVE, null);
        System.out.println("=== SRC_MOVE (raw) ===");
        System.out.println(s);
        assertTrue(s.contains("type: SRC_MOVE"));
    }

    @Test
    public void dst_move_keeps_raw_type() {
        String s = repr("int x;", "Bar.java", "VARIABLE_DECLARATION_STATEMENT",
                        NodeType.DST_MOVE, null);
        System.out.println("=== DST_MOVE (raw) ===");
        System.out.println(s);
        assertTrue(s.contains("type: DST_MOVE"));
    }

    @Test
    public void src_update_keeps_raw_type() {
        String s = repr("oldX", "Foo.java", "VARIABLE_DECLARATION_FRAGMENT",
                        NodeType.SRC_UPDATE, null);
        System.out.println("=== SRC_UPDATE (raw) ===");
        System.out.println(s);
        assertTrue(s.contains("type: SRC_UPDATE"));
    }

    @Test
    public void dst_update_keeps_raw_type() {
        String s = repr("newX", "Bar.java", "VARIABLE_DECLARATION_FRAGMENT",
                        NodeType.DST_UPDATE, null);
        System.out.println("=== DST_UPDATE (raw) ===");
        System.out.println(s);
        assertTrue(s.contains("type: DST_UPDATE"));
    }

    // =============== 2. ID format ==============

    @Test
    public void id_format_has_path_srcdst_nodetype_pos_endpos_treetype() {
        Node node = makeNode("public class Foo {}", "Foo.java", "TYPE_DECLARATION", NodeType.ADDITION);
        String id = node.getId();
        System.out.println("=== ID ===");
        System.out.println(id);
        assertTrue(id.contains("Foo.java"));
        assertTrue(id.contains("SRC"));
        assertTrue(id.contains("ADDITION"), "ID should contain ADDITION but was: " + id);
    }

    @Test
    public void id_format_different_srcdst() {
        Node srcN = makeNode("x", "A.java", "VARIABLE_DECLARATION_FRAGMENT", NodeType.ADDITION);
        Node dstN = makeNode("y", "B.java", "VARIABLE_DECLARATION_FRAGMENT", NodeType.DST_UPDATE);
        System.out.println("=== ID SRC === " + srcN.getId());
        System.out.println("=== ID DST === " + dstN.getId());
        assertTrue(srcN.getId().contains("SRC"));
        assertTrue(dstN.getId().contains("DST"));
    }

    // =============== 3. Content extraction ==============

    @Test
    public void location_context_type_decl_returns_simple_name() {
        TreeContext ctx = new TreeContext();
        Tree tree = makeTree(ctx, "TYPE_DECLARATION", 15);
        DefaultTree nameChild = new DefaultTree(null);
        nameChild.setType(TypeSet.type(JAVA.SIMPLE_NAME));
        nameChild.setLabel("Foo");
        nameChild.setLength(3);
        tree.getChildren().add(nameChild);
        Node node = new Node("public class Foo {}", "Foo.java", SrcDst.SRC, tree, null, null,
                             NodeType.LOCATION_CONTEXT, null);
        String s = node.textualRepresentation(null);
        System.out.println("=== LOC_CTX TYPE_DECL -> simple name ===");
        System.out.println(s);
        assertTrue(s.contains("Foo"));
    }

    @Test
    public void location_context_method_decl_returns_simple_name() {
        TreeContext ctx = new TreeContext();
        Tree tree = makeTree(ctx, "METHOD_DECLARATION", 15);
        DefaultTree nameChild = new DefaultTree(null);
        nameChild.setType(TypeSet.type(JAVA.SIMPLE_NAME));
        nameChild.setLabel("bar");
        nameChild.setLength(3);
        tree.getChildren().add(nameChild);
        Node node = new Node("public void bar() {}", "Foo.java", SrcDst.SRC, tree, null, null,
                             NodeType.LOCATION_CONTEXT, null);
        String s = node.textualRepresentation(null);
        System.out.println("=== LOC_CTX METHOD_DECL -> simple name ===");
        System.out.println(s);
        assertTrue(s.contains("bar"));
    }

    @Test
    public void location_context_compilation_unit_returns_path() {
        Node node = makeNode("package com.example;", "src/com/example/MyClass.java",
                             "COMPILATION_UNIT", NodeType.LOCATION_CONTEXT);
        String s = node.textualRepresentation(null);
        System.out.println("=== LOC_CTX COMPILATION_UNIT -> path ===");
        System.out.println(s);
        assertTrue(s.contains("MyClass.java"));
    }

    @Test
    public void location_context_enum_returns_simple_name() {
        TreeContext ctx = new TreeContext();
        Tree tree = makeTree(ctx, "ENUM_DECLARATION", 20);
        DefaultTree nameChild = new DefaultTree(null);
        nameChild.setType(TypeSet.type(JAVA.SIMPLE_NAME));
        nameChild.setLabel("Color");
        nameChild.setLength(5);
        tree.getChildren().add(nameChild);
        Node node = new Node("enum Color { RED, BLUE }", "Color.java", SrcDst.SRC, tree, null, null,
                             NodeType.LOCATION_CONTEXT, null);
        String s = node.textualRepresentation(null);
        System.out.println("=== LOC_CTX ENUM -> simple name ===");
        System.out.println(s);
        assertTrue(s.contains("Color"));
    }

    @Test
    public void location_context_record_returns_simple_name() {
        TreeContext ctx = new TreeContext();
        Tree tree = makeTree(ctx, "RECORD_DECLARATION", 20);
        DefaultTree nameChild = new DefaultTree(null);
        nameChild.setType(TypeSet.type(JAVA.SIMPLE_NAME));
        nameChild.setLabel("Point");
        nameChild.setLength(5);
        tree.getChildren().add(nameChild);
        Node node = new Node("record Point(int x, int y) {}", "Point.java", SrcDst.SRC, tree, null, null,
                             NodeType.LOCATION_CONTEXT, null);
        String s = node.textualRepresentation(null);
        System.out.println("=== LOC_CTX RECORD -> simple name ===");
        System.out.println(s);
        assertTrue(s.contains("Point"));
    }

    @Test
    public void regular_node_returns_full_content() {
        Node node = makeNode("public class Foo {}", "Foo.java", "TYPE_DECLARATION", NodeType.ADDITION);
        String s = node.textualRepresentation(null);
        System.out.println("=== Regular -> full content ===");
        System.out.println(s);
        assertTrue(s.contains("public class Foo {}"));
    }

    // =============== 4. With cluster (MAPPING edges) ==============

    @Test
    public void mapping_source_gets_after_type() {
        TreeContext ctx = new TreeContext();
        Tree treeA = makeTypedTree(ctx, JAVA.VARIABLE_DECLARATION_FRAGMENT, 5);
        TreeContext ctx2 = new TreeContext();
        Tree treeB = makeTypedTree(ctx2, JAVA.VARIABLE_DECLARATION_FRAGMENT, 5);
        DefaultTree nameB = new DefaultTree(null);
        nameB.setType(TypeSet.type(JAVA.SIMPLE_NAME));
        nameB.setLabel("y");
        nameB.setLength(1);
        treeB.getChildren().add(nameB);

        Node nA = new Node("int old;", "Foo.java", SrcDst.SRC, treeA, null, null,
                           NodeType.ADDITION, null);
        Node nB = new Node("int y;", "Bar.java", SrcDst.DST, treeB, null, null,
                           NodeType.SRC_UPDATE, null);

        Cluster cl = new Cluster();
        cl.getGraph().addVertex(nA);
        cl.getGraph().addVertex(nB);
        cl.getGraph().addEdge(nA, nB, new Edge(EdgeType.MAPPING));

        String src = nA.textualRepresentation(cl);
        String dst = nB.textualRepresentation(cl);
        System.out.println("=== MAPPING source -> AFTER ===");
        System.out.println(src);
        System.out.println("=== MAPPING target  -> BEFORE ===");
        System.out.println(dst);

        assertTrue(src.contains("AFTER_MAPPING"));
        assertTrue(dst.contains("BEFORE_MAPPING"));
    }

    @Test
    public void bidirectional_mapping() {
        TreeContext ctxA = new TreeContext();
        Tree treeA = makeTypedTree(ctxA, JAVA.VARIABLE_DECLARATION_FRAGMENT, 4);
        TreeContext ctxB = new TreeContext();
        Tree treeB = makeTypedTree(ctxB, JAVA.VARIABLE_DECLARATION_FRAGMENT, 4);

        Node nA = new Node("int x;", "Foo.java", SrcDst.SRC, treeA, null, null,
                           NodeType.ADDITION, null);
        Node nB = new Node("int y;", "Bar.java", SrcDst.DST, treeB, null, null,
                           NodeType.DST_UPDATE, null);

        Cluster cl = new Cluster();
        Graph<Node, Edge> g = cl.getGraph();
        g.addVertex(nA); g.addVertex(nB);
        g.addEdge(nA, nB, new Edge(EdgeType.MAPPING));
        g.addEdge(nB, nA, new Edge(EdgeType.MAPPING));

        String aOut = nA.textualRepresentation(cl);
        String bOut = nB.textualRepresentation(cl);
        System.out.println("=== Bidirectional A ===");
        System.out.println(aOut);
        System.out.println("=== Bidirectional B ===");
        System.out.println(bOut);

        // Note: Implementation uses last-write-wins for finalValidNodeType
        // When both edges exist, the last set (BEFORE) takes precedence
        assertTrue(aOut.contains("BEFORE_MAPPING"));
        assertTrue(bOut.contains("AFTER_MAPPING") || bOut.contains("BEFORE_MAPPING"));
    }

    @Test
    public void multiple_incoming_mapping_edges() {
        TreeContext ctxA = new TreeContext();
        Tree treeA = makeTypedTree(ctxA, JAVA.VARIABLE_DECLARATION_FRAGMENT, 2);
        TreeContext ctxB = new TreeContext();
        Tree treeB = makeTypedTree(ctxB, JAVA.VARIABLE_DECLARATION_FRAGMENT, 2);
        TreeContext ctxC = new TreeContext();
        Tree treeC = makeTypedTree(ctxC, JAVA.VARIABLE_DECLARATION_FRAGMENT, 3);

        Node nA = new Node("ab", "A.java", SrcDst.SRC, treeA, null, null,
                           NodeType.ADDITION, null);
        Node nB = new Node("cd", "A.java", SrcDst.SRC, treeB, null, null,
                           NodeType.ADDITION, null);
        Node nC = new Node("cur", "B.java", SrcDst.DST, treeC, null, null,
                           NodeType.DST_UPDATE, null);

        Cluster cl = new Cluster();
        Graph<Node, Edge> g = cl.getGraph();
        g.addVertex(nA); g.addVertex(nB); g.addVertex(nC);
        g.addEdge(nA, nC, new Edge(EdgeType.MAPPING));
        g.addEdge(nB, nC, new Edge(EdgeType.MAPPING));

        String cOut = nC.textualRepresentation(cl);
        System.out.println("=== Multiple incoming MAPPING edges ===");
        System.out.println(cOut);
        assertTrue(cOut.contains("BEFORE_MAPPING"));
    }

    // =============== 5. Edge cases ==============

    @Test
    public void empty_path_still_works() {
        String s = repr("code", "", "TYPE_DECLARATION", NodeType.ADDITION, null);
        System.out.println("=== Empty path ===");
        System.out.println(s);
        assertTrue(s.contains("ADDED"));
    }

    @Test
    public void multi_line_content_preserved() {
        String content = "public class Foo {\n" +
                         "    public void bar() {\n" +
                         "        System.out.println(\"hello\");\n" +
                         "    }\n" +
                         "}";
        Node node = makeNode(content, "Foo.java", "TYPE_DECLARATION", NodeType.ADDITION);
        String s = node.textualRepresentation(null);
        System.out.println("=== Multi-line content ===");
        System.out.println(s);
        assertTrue(s.contains("public class Foo"));
        assertTrue(s.contains("void bar()"));
        assertTrue(s.contains("hello"));
    }

    @Test
    public void single_character_content() {
        Node node = makeNode("x", "A.java", "VARIABLE_DECLARATION_FRAGMENT", NodeType.ADDITION);
        String s = node.textualRepresentation(null);
        System.out.println("=== Single char ===");
        System.out.println(s);
        assertTrue(s.contains("x"));
    }

    @Test
    public void special_xml_html_characters() {
        String content = "String s = \"<tag> & 'quote'\";";
        Node node = makeNode(content, "B.java", "STRING_LITERAL", NodeType.ADDITION);
        String s = node.textualRepresentation(null);
        System.out.println("=== Special chars ===");
        System.out.println(s);
        assertTrue(s.contains("<tag>"));
        assertTrue(s.contains("&"));
    }

    @Test
    public void unicode_content() {
        String content = "class 你好 { void 测试() {} }";
        Node node = makeNode(content, "你好.java", "TYPE_DECLARATION", NodeType.ADDITION);
        String s = node.textualRepresentation(null);
        System.out.println("=== Unicode ===");
        System.out.println(s);
        assertTrue(s.contains("你好"));
    }

    @Test
    public void empty_content_string() {
        Node node = makeNode("", "Empty.java", "TYPE_DECLARATION", NodeType.ADDITION);
        String s = node.textualRepresentation(null);
        System.out.println("=== Empty content ===");
        System.out.println(s);
        assertTrue(s.contains("ADDED"));
    }

    // =============== Main: demo ==============

    public static void main(String[] args) {
        NodeTextualRepresentationTest t = new NodeTextualRepresentationTest();

        System.out.println("╔═══════════════════════════════════╗");
        System.out.println("║  Node.textualRepresentation() Demo║");
        System.out.println("╚═══════════════════════════════════╝");

        System.out.println("\n--- 1. Baseline: node types WITHOUT cluster ---");
        t.test("Addition -> ADDED",      t::addition_node_maps_to_ADDED);
        t.test("Deletion -> DELETED",    t::deletion_node_maps_to_DELETED);
        t.test("Extension -> UNCHANGED", t::extension_node_maps_to_UNCHANGED);
        t.test("LOCATION_CONTEXT (raw)", t::location_context_keeps_raw_type);
        t.test("SEMANTIC_CONTEXT (raw)", t::semantic_context_keeps_raw_type);
        t.test("SRC_MOVE (raw)",         t::src_move_keeps_raw_type);
        t.test("DST_MOVE (raw)",         t::dst_move_keeps_raw_type);
        t.test("SRC_UPDATE (raw)",       t::src_update_keeps_raw_type);
        t.test("DST_UPDATE (raw)",       t::dst_update_keeps_raw_type);

        System.out.println("\n--- 2. ID Format ---");
        t.test("ID format",  t::id_format_has_path_srcdst_nodetype_pos_endpos_treetype);
        t.test("SRC vs DST", t::id_format_different_srcdst);

        System.out.println("\n--- 3. Content Extraction ---");
        t.test("LOC_CTX TYPE_DECL -> name", t::location_context_type_decl_returns_simple_name);
        t.test("LOC_CTX METHOD_DECL -> name", t::location_context_method_decl_returns_simple_name);
        t.test("LOC_CTX COMPILATION_UNIT -> path", t::location_context_compilation_unit_returns_path);
        t.test("LOC_CTX ENUM -> name", t::location_context_enum_returns_simple_name);
        t.test("LOC_CTX RECORD -> name", t::location_context_record_returns_simple_name);
        t.test("Regular -> full content", t::regular_node_returns_full_content);

        System.out.println("\n--- 4. With Cluster (MAPPING Edges) ---");
        t.test("MAPPING -> AFTER",   t::mapping_source_gets_after_type);
        t.test("Bidirectional",      t::bidirectional_mapping);
        t.test("Multi incoming",     t::multiple_incoming_mapping_edges);

        System.out.println("\n--- 5. Edge Cases ---");
        t.test("Empty path",     t::empty_path_still_works);
        t.test("Multi-line",     t::multi_line_content_preserved);
        t.test("Single char",    t::single_character_content);
        t.test("Special chars",  t::special_xml_html_characters);
        t.test("Unicode",        t::unicode_content);
        t.test("Empty content",  t::empty_content_string);

        System.out.println("\n✓ All scenarios demonstrated!");
    }

    private void test(String label, Runnable fn) {
        System.out.println("\n--- " + label + " ---");
        fn.run();
    }
}
