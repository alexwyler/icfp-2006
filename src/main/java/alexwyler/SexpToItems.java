package alexwyler;

import java.util.ArrayList;
import java.util.List;

// Courtesy of ChatGPT
public final class SexpToItems {

    public static List<StackSolver.Item> parseStack(String s) {
        Object root = new P(s).any();
        List<?> items = find(root, "items");
        if (items == null || ((List<?>) items).size() < 2) {
            throw new IllegalArgumentException("no (items ...)");
        }
        List<StackSolver.Item> out = new ArrayList<>();
        for (Object o : list(((List<?>) items).get(1))) {
            if (headIs(o, "item")) {
                flatten((List<?>) o, out);
            }
        }
        return out;
    }

    private static void flatten(List<?> item, List<StackSolver.Item> out) {
        out.add(toItem(item));
        List<?> po = field(item, "piled_on");
        if (po != null && ((List<?>) po).size() > 1) {
            for (Object o : list(((List<?>) po).get(1))) {
                if (headIs(o, "item")) {
                    flatten((List<?>) o, out);
                }
            }
        }
    }

    private static StackSolver.Item toItem(List<?> it) {
        String name = str(field(it, "name"), 1);
        String adj = firstAdj(field(it, "adjectives"));
        List<StackSolver.Item> miss = missing(field(it, "condition"));
        return new StackSolver.Item(name, adj, miss);
    }

    private static String firstAdj(Object adjs) {
        if (!(adjs instanceof List<?> l) || l.size() < 2) {
            return null;
        }
        for (Object o : list(l.get(1))) {
            if (headIs(o, "adjective")) {
                return str((List<?>) o, 1);
            }
        }
        return null;
    }

    private static List<StackSolver.Item> missing(Object cond) {
        if (!(cond instanceof List<?> l) || l.size() < 2) {
            return List.of();
        }
        return fromState(l.get(1));
    }

    private static List<StackSolver.Item> fromState(Object st) {
        if (!(st instanceof List<?> s) || s.isEmpty()) {
            return List.of();
        }
        if ("pristine".equals(s.get(0))) {
            return List.of();
        }
        if (!"broken".equals(s.get(0))) {
            return List.of();
        }

        List<StackSolver.Item> out = new ArrayList<>();
        // direct (missing ((kind ...)...))
        for (Object ch : s) {
            if (headIs(ch, "missing") && ((List<?>) ch).size() >= 2) {
                collectKinds(((List<?>) ch).get(1), out);
            }
        }

        // nested (condition <state>)
        for (Object ch : s) {
            if (headIs(ch, "condition") && ((List<?>) ch).size() >= 2) {
                out.addAll(fromState(((List<?>) ch).get(1)));
            }
        }
        return out;
    }

    private static void collectKinds(Object node, List<StackSolver.Item> out) {
        if (headIs(node, "kind")) {
            out.add(kind((List<?>) node));
            return;
        }
        if (node instanceof List<?> l) {
            for (Object c : l) {
                collectKinds(c, out);
            }
        }
    }

    private static StackSolver.Item kind(List<?> k) {
        String name = str(field(k, "name"), 1);
        String adj = firstAdj(field(k, "adjectives"));
        List<StackSolver.Item> miss = missing(field(k, "condition"));
        return new StackSolver.Item(name, adj, miss);
    }

    // ------- tiny S-expr helpers -------
    private static List<?> field(List<?> l, String key) {
        for (Object o : l) {
            if (headIs(o, key)) {
                return (List<?>) o;
            }
        }
        return null;
    }

    private static boolean headIs(Object o, String h) {return (o instanceof List<?> l) && !l.isEmpty() && h.equals(l.get(0));}

    private static String str(List<?> l, int i) {
        Object v = l.size() > i ? l.get(i) : null;
        return v == null ? null : String.valueOf(v);
    }

    private static List<?> list(Object o) {return (o instanceof List) ? (List<?>) o : List.of();}

    private static List<?> find(Object n, String key) {
        if (headIs(n, key)) {
            return (List<?>) n;
        }
        if (n instanceof List<?> l) {
            for (Object c : l) {
                List<?> r = find(c, key);
                if (r != null) {
                    return r;
                }
            }
        }
        return null;
    }

    public static void main(String[] args) {
        String s = """
                (success (command (go (room (name "Junk Room")(description "You are in a room with a pile of junk. A hallway leads south. ")(items ((item (name "bolt")(description "quite useful for securing all sorts of things")(adjectives )(condition (pristine ))(piled_on ((item (name "spring")(description "tightly coiled")(adjectives )(condition (pristine ))(piled_on ((item (name "button")(description "labeled 6")(adjectives )(condition (pristine ))(piled_on ((item (name "processor")(description "from the elusive 19x86 line")(adjectives )(condition (broken (condition (pristine ))(missing ((kind (name "cache")(condition (pristine ))) ))))(piled_on ((item (name "pill")(description "tempting looking")(adjectives ((adjective "red") ))(condition (pristine ))(piled_on ((item (name "radio")(description "a hi-fi AM/FM stereophonic radio")(adjectives )(condition (broken (condition (pristine ))(missing ((kind (name "transistor")(condition (pristine ))) ((kind (name "antenna")(condition (pristine ))) )))))(piled_on ((item (name "cache")(description "fully-associative")(adjectives )(condition (pristine ))(piled_on ((item (name "transistor")(description "PNP-complete")(adjectives ((adjective "blue") ))(condition (pristine ))(piled_on ((item (name "antenna")(description "appropriate for receiving transmissions between 30 kHz and 30 MHz")(adjectives )(condition (pristine ))(piled_on ((item (name "screw")(description "not from a Dutch company")(adjectives )(condition (pristine ))(piled_on ((item (name "motherboard")(description "well-used")(adjectives )(condition (broken (condition (pristine ))(missing ((kind (name "A-1920-IXB")(condition (pristine ))) ((kind (name "screw")(condition (pristine ))) )))))(piled_on ((item (name "A-1920-IXB")(description "an exemplary instance of part number A-1920-IXB")(adjectives )(condition (broken (condition (broken (condition (pristine ))(missing ((kind (name "transistor")(condition (pristine ))) ))))(missing ((kind (name "radio")(condition (broken (condition (pristine ))(missing ((kind (name "antenna")(condition (pristine ))) ))))) ((kind (name "processor")(condition (pristine ))) ((kind (name "bolt")(condition (pristine ))) ))))))(piled_on ((item (name "transistor")(description "NPN-complete")(adjectives ((adjective "red") ))(condition (pristine ))(piled_on ((item (name "keypad")(description "labeled \\"use me\\"")(adjectives )(condition (broken (condition (pristine ))(missing ((kind (name "motherboard")(condition (pristine ))) ((kind (name "button")(condition (pristine ))) )))))(piled_on ((item (name "trash")(description "of absolutely no value")(adjectives )(condition (pristine ))(piled_on )) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))))))
            """;

        List<StackSolver.Item> items = parseStack(s);
        for (StackSolver.Item it : items) {
            System.out.println(it);
        }
    }

    // ------- minimal S-expr parser -------
    static final class P {

        final String s;
        int i = 0;

        P(String s) {this.s = s;}

        Object any() {
            skip();
            if (i >= s.length()) {
                return List.of();
            }
            char c = s.charAt(i);
            if (c == '(') {
                return list();
            }
            if (c == '"') {
                return str();
            }
            return atom();
        }

        List<Object> list() {
            i++;
            List<Object> out = new ArrayList<>();
            skip();
            while (i < s.length() && s.charAt(i) != ')') {
                out.add(any());
                skip();
            }
            i++;
            return out;
        }

        String str() {
            i++;
            StringBuilder b = new StringBuilder();
            while (i < s.length()) {
                char c = s.charAt(i++);
                if (c == '\\') {
                    if (i < s.length()) {
                        char n = s.charAt(i++);
                        b.append(n == 'n' ? '\n' : n == 't' ? '\t' : n == 'r' ? '\r' : n == '"' ? '"' : n == '\\' ? '\\' : n);
                    }
                } else if (c == '"') {
                    break;
                } else {
                    b.append(c);
                }
            }
            return b.toString();
        }

        String atom() {
            int j = i;
            while (i < s.length()) {
                char c = s.charAt(i);
                if (Character.isWhitespace(c) || c == '(' || c == ')') {
                    break;
                }
                i++;
            }
            return s.substring(j, i);
        }

        void skip() {
            while (i < s.length()) {
                char c = s.charAt(i);
                if (Character.isWhitespace(c)) {
                    i++;
                    continue;
                }
                if (c == ';') {
                    while (i < s.length() && s.charAt(i) != '\n') {
                        i++;
                    }
                    continue;
                }
                break;
            }
        }

    }

}