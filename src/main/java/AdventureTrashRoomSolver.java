import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.TopologicalOrderIterator;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdventureTrashRoomSolver {

    final Multimap<String, String> edges = LinkedHashMultimap.create();

    public static AdventureTrashRoomSolver parse(String text) {
        AdventureTrashRoomSolver p = new AdventureTrashRoomSolver();
        String[] lines = text.replace("\r\n", "\n").split("\n");
        Pattern subjP = Pattern.compile("^>?:?\\s*The\\s+(.+?)\\s+is\\b.*", Pattern.CASE_INSENSITIVE);
        Pattern brokenP = Pattern.compile("^\\s*Also,\\s+it\\s+is\\s+broken:\\s+it\\s+is\\s+(.+)", Pattern.CASE_INSENSITIVE);

        String subject = null;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            Matcher ms = subjP.matcher(line);
            if (ms.find()) {
                subject = ms.group(1).trim();
            }

            Matcher mb = brokenP.matcher(line);
            if (mb.find() && subject != null) {
                String tail = mb.group(1).trim();
                int j = i + 1;
                while (!tail.endsWith(".") && j < lines.length) {
                    String nxt = lines[j].trim();
                    if (nxt.startsWith(">:") || nxt.isEmpty()) {
                        break;
                    }
                    tail += " " + nxt;
                    j++;
                }
                p.addBroken(subject, tail);
            }
        }
        return p;
    }

    static String stripDot(String s) {
        return s.endsWith(".") ? s.substring(0, s.length() - 1).trim() : s;
    }

    static String peel(String s) {
        while (s.startsWith("(") && s.endsWith(")") && balanced(s)) {
            s = s.substring(1, s.length() - 1).trim();
        }
        return s;
    }

    static boolean balanced(String s) {
        int d = 0;
        for (char c : s.toCharArray()) {
            if (c == '(') {
                d++;
            } else if (c == ')') {
                d--;
                if (d < 0) {
                    return false;
                }
            }
        }
        return d == 0;
    }

    static int topMissing(String s) {
        String needle = " missing ";
        int d = 0;
        for (int i = 0; i <= s.length() - needle.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') {
                d++;
            } else if (c == ')') {
                d = Math.max(0, d - 1);
            }
            if (d == 0 && s.regionMatches(true, i, needle, 0, needle.length())) {
                return i;
            }
        }
        return -1;
    }

    static List<String> splitTopAnd(String s) {
        List<String> out = new ArrayList<>();
        int d = 0, last = 0;
        for (int i = 0; i < s.length() - 4; i++) {
            char c = s.charAt(i);
            if (c == '(') {
                d++;
            } else if (c == ')') {
                d = Math.max(0, d - 1);
            }
            if (d == 0 && s.regionMatches(true, i, " and ", 0, 5)) {
                out.add(s.substring(last, i).trim());
                last = i + 5;
            }
        }
        out.add(s.substring(last).trim());
        return out;
    }

    static String norm(String s) {
        s = peel(s);
        s = s.replaceFirst("^(?i)(a|an|the)\\s+", "");
        s = s.replaceFirst("^\\(\\s*(?i:broken)\\s*\\)\\s*", "");
        s = s.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
        return s;
    }

    private static String readResourceOrDie(String name) {
        var in = AdventureTrashRoomSolver.class.getClassLoader().getResourceAsStream(name);
        if (in == null) {
            throw new RuntimeException("Resource not found: " + name);
        }
        try (in) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        String txt = readResourceOrDie("adventure_trash_room_output.txt");

        AdventureTrashRoomSolver deps = AdventureTrashRoomSolver.parse(txt);
        deps.printAdjacency(System.out);
        System.out.println();
    }

    void addBroken(String subject, String tail) {
        String subj = norm(subject);
        String txt = stripDot(tail);
        int k = topMissing(txt);
        if (k < 0) {
            return;
        }
        String rhs = txt.substring(k + " missing ".length()).trim();
        for (String part : splitTopAnd(rhs)) {
            String node = buildReqNode(part);
            addEdge(subj, node);
        }
    }

    String buildReqNode(String phrase) {
        phrase = peel(phrase.trim());
        int k = topMissing(phrase);
        if (k < 0) {
            return norm(phrase);
        }

        String base = norm(phrase.substring(0, k).trim());
        String rhs = phrase.substring(k + " missing ".length()).trim();
        List<String> kids = new ArrayList<>();
        for (String part : splitTopAnd(rhs)) {
            kids.add(buildReqNode(part));
        }

        String state = base + " (missing " + String.join(", ", kids) + ")";
        for (String c : kids) {
            addEdge(state, c);
        }
        // ensure base appears (no-op edge)
        edges.put(base, base);
        return state;
    }

    void addEdge(String a, String b) {edges.put(a, b);}

    void printAdjacency(PrintStream out) {
        for (var k : new LinkedHashSet<>(edges.keySet())) {
            var deps = new LinkedHashSet<>(edges.get(k));
            deps.remove(k);
            if (deps.isEmpty()) {
                continue;
            }
            out.println(k + " -> " + String.join(", ", deps));
        }
    }

    void printDOT(PrintStream out) {
        out.println("digraph deps { rankdir=LR; node [shape=box,fontname=Helvetica];");
        Set<String> all = new LinkedHashSet<>(edges.keySet());
        for (var a : all) {
            for (var b : edges.get(a)) {
                if (a.equals(b)) {
                    continue;
                }
                out.printf("  \"%s\" -> \"%s\";%n", a, b);
            }
            if (edges.get(a).isEmpty()) {
                out.printf("  \"%s\";%n", a);
            }
        }
        out.println("}");
    }

    List<String> topo(String target) {
        Graph<String, DefaultEdge> g = new DefaultDirectedGraph<>(DefaultEdge.class);
        for (var a : edges.keySet()) {
            g.addVertex(a);
            for (var b : edges.get(a)) {
                g.addVertex(b);
                if (!a.equals(b)) {
                    g.addEdge(a, b);
                }
            }
        }
        List<String> order = new ArrayList<>();
        var it = new TopologicalOrderIterator<>(g);
        while (it.hasNext()) {
            order.add(it.next());
        }
        // filter to the cone-of-influence of 'target'
        Set<String> needed = new LinkedHashSet<>();
        collect(target, needed);
        order.retainAll(needed);
        return order;
    }

    void collect(String n, Set<String> acc) {
        if (!acc.add(n)) {
            return;
        }
        // Multimap#get never returns null; use emptyList when key absent
        Collection<String> deps = edges.containsKey(n) ? edges.get(n) : Collections.emptyList();
        for (String d : deps) {
            if (!d.equals(n)) {
                collect(d, acc);
            }
        }
    }

}
