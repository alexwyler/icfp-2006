package alexwyler;

import io.vavr.Tuple2;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StackSolver {

    private final Set<Item> neededItems;

    private static Set<Item> computeNeededIems(Item root) {
        Set<Item> items = new HashSet<>();
        Set<Item> visited = new HashSet<>();
        Deque<Item> dq = new ArrayDeque<>();
        dq.push(root);
        while (!dq.isEmpty()) {
            Item cur = dq.pop();
            for (Item m : cur.missing) {
                items.add(m);
                if (visited.add(m)) dq.push(m);
            }
        }
        return items;
    }

    static final int CAPACITY = 6;
    final List<Item> stack;
    final List<Item> inventory;
    final Item targetItem;

    public StackSolver(final List<Item> initialStack, List<Item> inventory, final Item targetItem) {
        this.stack = initialStack;
        this.targetItem = targetItem;
        this.inventory = inventory;
        neededItems = computeNeededIems(targetItem);
    }

    static Tuple2<Item, Item> unorderedPair(Item a, Item b) {
        return a.compareTo(b) <= 0 ? new Tuple2<>(a, b) : new Tuple2<>(b, a);
    }

    private boolean dfs(
        int idx,
        List<Item> inv,
        Set<Integer> seen,
        List<String> plan
    ) {

        if (inv.contains(targetItem)) {
            return true;
        }

        int stateHash = Objects.hash(idx, new HashSet<>(inv));
        if (!seen.add(stateHash)) {
            return false;
        }

        // take
        if (inv.size() < CAPACITY && idx < stack.size()) {
            Item next = stack.get(idx);
            inv.add(next);
            plan.add("take " + next.toPlanString());
            if (dfs(idx + 1, inv, seen, plan)) {
                return true;
            }
            inv.remove((int) (inv.size() - 1));
            plan.remove((int) (plan.size() - 1));
        } else {
            // try combine
            for (int i = 0; i < inv.size(); i++) {
                Item a = inv.get(i);
                for (int j = i + 1; j < inv.size(); j++) {
                    Item b = inv.get(j);

                    Item result;
                    String planString;
                    if (a.canCombineWith(b)) {
                        result = a.combineWith(b);
                        planString = "combine " + a.toPlanString() + " with " + b.toPlanString();
                    } else if (b.canCombineWith(a)) {
                        result = b.combineWith(a);
                        planString = "combine " + b.toPlanString() + " with " + a.toPlanString();
                    } else {
                        continue;
                    }

                    inv.remove(j);
                    inv.remove(i);
                    inv.add(result);
                    plan.add(planString);

                    if (dfs(idx, inv, seen, plan))
                        return true;

                    plan.remove(plan.size() - 1);
                    inv.remove(inv.size() - 1);
                    inv.add(i, a);
                    inv.add(j, b);
                }
            }

            // incinerate
            for (int i = 0; i < inv.size(); i++) {
                Item x = inv.get(i);
                if (x.equals(targetItem) || this.inventory.contains(x)) {
                    continue;
                }
                if (neededItems.contains(x)) {
                    continue;
                }

                inv.remove(i);
                plan.add("incinerate " + x.toPlanString());
                if (dfs(idx, inv, seen, plan)) {
                    return true;
                }
                inv.add(i, x);
                plan.removeLast();
            }
        }

        return false;
    }

    public List<String> solve() {
        long startTime = System.currentTimeMillis();
        List<String> plan = new ArrayList<>();
        List<Item> inventory = new ArrayList<>(this.inventory);
        Set<Integer> seen = new HashSet<>();

        if (!dfs(0, inventory, seen, plan)) {
            throw new RuntimeException("No solution found");
        }

        for (Item x : inventory) {
            if (x.equals(targetItem) || this.inventory.contains(x)) {
                continue;
            }
            plan.add("incinerate " + x.toPlanString());
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Solved in " + (endTime - startTime) + "ms, plan length " + plan.size());
        return plan;
    }

    public static class Item implements Comparable<Item> {

        private final String name;
        private final String adjective;
        private final Set<Item> missing;
        private final String planName;
        // Cached fields
        private Integer cachedHash = null;
        private final Map<Item, Boolean> canCombineCache = new HashMap<>();

        public Item(String name, String adjective, Set<Item> missing) {
            this.name = name;
            this.adjective = adjective;
            this.missing = Collections.unmodifiableSet(new HashSet<>(missing));
            this.planName = (adjective != null ? adjective + " " : "") + name;
        }

        // Record-style getters
        public String name() { return name; }
        public String adjective() { return adjective; }
        public Set<Item> missing() { return missing; }

        public String toPlanString() {
            return planName;
        }

        @Override
        public String toString() {
            return name + (adjective != null ? " (" + adjective + ")" : "") +
                   (missing.isEmpty() ? " *working* " : " missing=" + missing);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Item item)) return false;
            return Objects.equals(name, item.name)
                   && Objects.equals(adjective, item.adjective)
                   && Objects.equals(missing, item.missing);
        }

        @Override
        public int hashCode() {
            if (cachedHash == null) {
                cachedHash = Objects.hash(name, adjective, missing);
            }
            return cachedHash;
        }

        @Override
        public int compareTo(Item o) {
            int cmp = name.compareTo(o.name);
            if (cmp != 0) return cmp;
            cmp = Objects.compare(adjective, o.adjective, Comparator.nullsFirst(String::compareTo));
            if (cmp != 0) return cmp;
            cmp = Integer.compare(missing.size(), o.missing.size());
            if (cmp != 0) return cmp;
            return missing.toString().compareTo(o.missing.toString());
        }

        public boolean canCombineWith(Item other) {
            return canCombineCache.computeIfAbsent(other, (o) -> missing.stream().anyMatch(m -> m.isEquivalentSansAdjective(o)));
        }

        public Item combineWith(Item other) {
            Set<Item> newMissing = missing.stream()
                .filter(m -> !m.isEquivalentSansAdjective(other))
                .collect(Collectors.toSet());
            return new Item(name, adjective, newMissing);
        }

        /** Recursive comparison ignoring adjectives */
        public boolean isEquivalentSansAdjective(Item other) {
            if (!name.equals(other.name)) return false;
            if (missing.size() != other.missing.size()) return false;

            for (Item m : missing) {
                boolean matched = other.missing.stream().anyMatch(om -> m.isEquivalentSansAdjective(om));
                if (!matched) return false;
            }
            return true;
        }
    }

    public static void main(String[] args) {
        //        List<Item> stack1 = new ArrayList<>();
        //        {
        //            Item redTransistor = new Item("transistor", "red", List.of());
        //            Item antenna = new Item("antenna", null, List.of());
        //            Item redPill = new Item("pill", "red", List.of());
        //            Item radio = new Item("radio", null, List.of(
        //                new Item("transistor", null, List.of()),
        //                new Item("antenna", null, List.of())
        //            ));
        //            Item blueTransistor = new Item("transistor", "blue", List.of());
        //            Item cache = new Item("cache", null, List.of());
        //            Item screw = new Item("screw", null, List.of());
        //            Item processor = new Item("processor", null, List.of(
        //                new Item("cache", null, List.of())));
        //            Item bolt = new Item("bolt", null, List.of());
        //            Item spring = new Item("spring", null, List.of());
        //            Item button = new Item("button", null, List.of());
        //            Item a1920IXB = new Item("A-1920-IXB", null, List.of(
        //                new Item("radio", null, List.of(new Item("antenna", null, List.of()))),
        //                new Item("processor", null, List.of()),
        //                new Item("bolt", null, List.of())
        //            ));
        //            Item motherboard = new Item("motherboard", null, List.of(
        //                new Item("A-1920-IXB", null, List.of()),
        //                new Item("screw", null, List.of())
        //            ));
        //            Item keypad = new Item("keypad", null, List.of(
        //                new Item("button", null, List.of()),
        //                new Item("motherboard", null, List.of())
        //            ));
        //
        //            stack1.add(bolt);
        //            stack1.add(spring);
        //            stack1.add(button);
        //            stack1.add(processor);
        //            stack1.add(redPill);
        //            stack1.add(radio);
        //            stack1.add(cache);
        //            stack1.add(blueTransistor);
        //            stack1.add(antenna);
        //            stack1.add(screw);
        //            stack1.add(motherboard);
        //            stack1.add(a1920IXB);
        //            stack1.add(redTransistor);
        //            stack1.add(keypad);
        //            stack1.add(new Item("trash", null, List.of()));
        //
        //            System.out.println(stack1);
        //            System.out.println(stack1.stream()
        //                .map(Object::toString)
        //                .collect(Collectors.joining("\n")));
        //
        //            StackSolver solver = new StackSolver(stack1, new ArrayList<>(), new Item("keypad", null, List.of()));
        //            List<String> plan = solver.solve();
        //            System.out.println("Plan:");
        //            for (String step : plan) {
        //                System.out.println(step);
        //            }
        //
        //        }
        //        Tuple3<Boolean, List<Item>, String> stack2 = null;
        //        {
        //            String sexp = """
        //                 (success (command (go (room (name "Junk Room")(description "You are in a room with a pile of junk. A hallway leads south. ")(items ((item (name "bolt")(description "quite useful for securing all sorts of things")(adjectives )(condition (pristine ))(piled_on ((item (name "spring")(description "tightly coiled")(adjectives )(condition (pristine ))(piled_on ((item (name "button")(description "labeled 6")(adjectives )(condition (pristine ))(piled_on ((item (name "processor")(description "from the elusive 19x86 line")(adjectives )(condition (broken (condition (pristine ))(missing ((kind (name "cache")(condition (pristine ))) ))))(piled_on ((item (name "pill")(description "tempting looking")(adjectives ((adjective "red") ))(condition (pristine ))(piled_on ((item (name "radio")(description "a hi-fi AM/FM stereophonic radio")(adjectives )(condition (broken (condition (pristine ))(missing ((kind (name "transistor")(condition (pristine ))) ((kind (name "antenna")(condition (pristine ))) )))))(piled_on ((item (name "cache")(description "fully-associative")(adjectives )(condition (pristine ))(piled_on ((item (name "transistor")(description "PNP-complete")(adjectives ((adjective "blue") ))(condition (pristine ))(piled_on ((item (name "antenna")(description "appropriate for receiving transmissions between 30 kHz and 30 MHz")(adjectives )(condition (pristine ))(piled_on ((item (name "screw")(description "not from a Dutch company")(adjectives )(condition (pristine ))(piled_on ((item (name "motherboard")(description "well-used")(adjectives )(condition (broken (condition (pristine ))(missing ((kind (name "A-1920-IXB")(condition (pristine ))) ((kind (name "screw")(condition (pristine ))) )))))(piled_on ((item (name "A-1920-IXB")(description "an exemplary instance of part number A-1920-IXB")(adjectives )(condition (broken (condition (broken (condition (pristine ))(missing ((kind (name "transistor")(condition (pristine ))) ))))(missing ((kind (name "radio")(condition (broken (condition (pristine ))(missing ((kind (name "antenna")(condition (pristine ))) ))))) ((kind (name "processor")(condition (pristine ))) ((kind (name "bolt")(condition (pristine ))) ))))))(piled_on ((item (name "transistor")(description "NPN-complete")(adjectives ((adjective "red") ))(condition (pristine ))(piled_on ((item (name "keypad")(description "labeled \\"use me\\"")(adjectives )(condition (broken (condition (pristine ))(missing ((kind (name "motherboard")(condition (pristine ))) ((kind (name "button")(condition (pristine ))) )))))(piled_on ((item (name "trash")(description "of absolutely no value")(adjectives )(condition (pristine ))(piled_on )) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))))))
        //                """;
        //
        //            stack2 = SexpToItems.parseStack(sexp);
        //            System.out.println(stack1);
        //            System.out.println(stack2);
        //            System.out.println(stack1.equals(stack2));
        //            StackSolver solver = new StackSolver(stack2._2, new ArrayList<>(), new Item("keypad", null, List.of()));
        //            List<String> plan = solver.solve();
        //            System.out.println("Plan:");
        //            for (String step : plan) {
        //                System.out.println(step);
        //            }
        //
        //            System.out.println("VS.");
        //            System.out.println("""
        //                take bolt
        //                take spring
        //                incinerate spring
        //                take button
        //                take processor
        //                take red pill
        //                incinerate red pill
        //                take radio
        //                take cache
        //                combine processor with cache
        //                take blue transistor
        //                combine radio with blue transistor
        //                take antenna
        //                incinerate antenna
        //                take screw
        //                take motherboard
        //                combine motherboard with screw
        //                take A-1920-IXB
        //                examine A-1920-IXB
        //                combine A-1920-IXB with radio
        //                take red transistor
        //                combine A-1920-IXB with processor
        //                combine A-1920-IXB with bolt
        //                combine A-1920-IXB with red transistor
        //                combine motherboard with A-1920-IXB
        //                take keypad
        //                combine keypad with motherboard
        //                combine keypad with button""");
        //        }
        {
            String rawSexp = "(success (command (look (room (name \"53th Street and Dorchester Avenue\")(description \"You are standing at the corner of 53th Street and Dorchester Avenue. From here, you can go north, east, or south. \")(items ((item (name \"N-1623-AOE\")(description \"an exemplary instance of part number N-1623-AOE\")(adjectives ((adjective \"fern-green\") ))(condition (pristine ))(piled_on ((item (name \"R-4292-FRL\")(description \"an exemplary instance of part number R-4292-FRL\")(adjectives ((adjective \"burgundy\") ))(condition (broken (condition (broken (condition (pristine ))(missing ((kind (name \"V-9887-KUS\")(condition (pristine ))) ))))(missing ((kind (name \"Z-6458-PXZ\")(condition (broken (condition (pristine ))(missing ((kind (name \"D-5065-UBI\")(condition (pristine ))) ))))) ))))(piled_on ((item (name \"F-6458-DDN\")(description \"an exemplary instance of part number F-6458-DDN\")(adjectives ((adjective \"pale-magenta\") ))(condition (broken (condition (pristine ))(missing ((kind (name \"J-5065-IGU\")(condition (pristine ))) ))))(piled_on ((item (name \"H-1623-MYO\")(description \"an exemplary instance of part number H-1623-MYO\")(adjectives ((adjective \"peach-yellow\") ))(condition (broken (condition (broken (condition (pristine ))(missing ((kind (name \"L-4292-RCV\")(condition (pristine ))) ))))(missing ((kind (name \"T-6458-BIL\")(condition (broken (condition (broken (condition (broken (condition (pristine ))(missing ((kind (name \"X-5065-GLS\")(condition (pristine ))) ))))(missing ((kind (name \"T-5065-OQC\")(condition (broken (condition (pristine ))(missing ((kind (name \"B-6678-LOZ\")(condition (pristine ))) ))))) ))))(missing ((kind (name \"F-9247-QRI\")(condition (pristine ))) ))))) ((kind (name \"P-9887-WFE\")(condition (pristine ))) )))))(piled_on ((item (name \"H-4292-ZHF\")(description \"an exemplary instance of part number H-4292-ZHF\")(adjectives ((adjective \"rotating\") ))(condition (broken (condition (pristine ))(missing ((kind (name \"J-4832-VUP\")(condition (pristine ))) ))))(piled_on ((item (name \"R-6458-FXP\")(description \"an exemplary instance of part number R-6458-FXP\")(adjectives ((adjective \"low-carb\") ))(condition (broken (condition (pristine ))(missing ((kind (name \"V-5065-KBW\")(condition (pristine ))) ((kind (name \"H-1623-MYO\")(condition (broken (condition (broken (condition (pristine ))(missing ((kind (name \"L-4292-RCV\")(condition (pristine ))) ))))(missing ((kind (name \"P-9887-WFE\")(condition (pristine ))) ))))) ((kind (name \"H-4292-ZHF\")(condition (broken (condition (pristine ))(missing ((kind (name \"J-4832-VUP\")(condition (pristine ))) ))))) ))))))(piled_on ((item (name \"T-6458-BIL\")(description \"an exemplary instance of part number T-6458-BIL\")(adjectives ((adjective \"mysterious\") ))(condition (broken (condition (broken (condition (broken (condition (pristine ))(missing ((kind (name \"X-5065-GLS\")(condition (pristine ))) ))))(missing ((kind (name \"T-5065-OQC\")(condition (broken (condition (pristine ))(missing ((kind (name \"B-6678-LOZ\")(condition (pristine ))) ))))) ))))(missing ((kind (name \"F-9247-QRI\")(condition (pristine ))) ))))(piled_on ((item (name \"R-9247-SMK\")(description \"an exemplary instance of part number R-9247-SMK\")(adjectives ((adjective \"brass\") ))(condition (broken (condition (pristine ))(missing ((kind (name \"V-4832-XPR\")(condition (pristine ))) ))))(piled_on ((item (name \"Z-1403-CSY\")(description \"an exemplary instance of part number Z-1403-CSY\")(adjectives ((adjective \"puce\") ))(condition (broken (condition (pristine ))(missing ((kind (name \"D-0010-HVH\")(condition (pristine ))) ))))(piled_on ((item (name \"N-6678-NJD\")(description \"an exemplary instance of part number N-6678-NJD\")(adjectives ((adjective \"pink\") ))(condition (broken (condition (pristine ))(missing ((kind (name \"R-9247-SMK\")(condition (broken (condition (pristine ))(missing ((kind (name \"V-4832-XPR\")(condition (pristine ))) ))))) ))))(piled_on ((item (name \"X-4292-TWX\")(description \"an exemplary instance of part number X-4292-TWX\")(adjectives ((adjective \"jade\") ))(condition (broken (condition (pristine ))(missing ((kind (name \"N-6678-NJD\")(condition (pristine ))) ((kind (name \"B-9887-YAG\")(condition (pristine ))) )))))(piled_on ((item (name \"Z-6678-PEF\")(description \"an exemplary instance of part number Z-6678-PEF\")(adjectives ((adjective \"flax\") ))(condition (broken (condition (pristine ))(missing ((kind (name \"X-4292-TWX\")(condition (broken (condition (pristine ))(missing ((kind (name \"B-9887-YAG\")(condition (pristine ))) ))))) ))))(piled_on ((item (name \"H-4832-ZKT\")(description \"an exemplary instance of part number H-4832-ZKT\")(adjectives ((adjective \"pale-blue\") ))(condition (broken (condition (pristine ))(missing ((kind (name \"L-1403-ENC\")(condition (pristine ))) ))))(piled_on ((item (name \"P-0010-JQJ\")(description \"an exemplary instance of part number P-0010-JQJ\")(adjectives ((adjective \"gray60\") ))(condition (broken (condition (broken (condition (pristine ))(missing ((kind (name \"P-9247-WCO\")(condition (pristine ))) ))))(missing ((kind (name \"T-1623-OTQ\")(condition (pristine ))) ))))(piled_on ((item (name \"J-1403-IDG\")(description \"an exemplary instance of part number J-1403-IDG\")(adjectives ((adjective \"olive-green\") ))(condition (broken (condition (pristine ))(missing ((kind (name \"H-4832-ZKT\")(condition (broken (condition (pristine ))(missing ((kind (name \"L-1403-ENC\")(condition (pristine ))) ))))) ))))(piled_on ((item (name \"D-9247-UHM\")(description \"an exemplary instance of part number D-9247-UHM\")(adjectives ((adjective \"swamp-green\") ))(condition (pristine ))(piled_on ((item (name \"N-6458-NDX\")(description \"an exemplary instance of part number N-6458-NDX\")(adjectives ((adjective \"khaki\") ))(condition (broken (condition (broken (condition (pristine ))(missing ((kind (name \"L-6678-RYH\")(condition (pristine ))) ))))(missing ((kind (name \"Z-6678-PEF\")(condition (pristine ))) ((kind (name \"J-1403-IDG\")(condition (pristine ))) ((kind (name \"P-9247-WCO\")(condition (pristine ))) ))))))(piled_on ((item (name \"V-9887-KUS\")(description \"an exemplary instance of part number V-9887-KUS\")(adjectives ((adjective \"red-violet\") ))(condition (broken (condition (pristine ))(missing ((kind (name \"R-6458-FXP\")(condition (broken (condition (pristine ))(missing ((kind (name \"V-5065-KBW\")(condition (pristine ))) ))))) ((kind (name \"T-4832-BFV\")(condition (pristine ))) ((kind (name \"H-6678-ZEP\")(condition (broken (condition (pristine ))(missing ((kind (name \"V-9887-KUS\")(condition (pristine ))) ))))) ))))))(piled_on ((item (name \"N-0010-NGN\")(description \"an exemplary instance of part number N-0010-NGN\")(adjectives ((adjective \"tea-green\") ))(condition (broken (condition (pristine ))(missing ((kind (name \"N-6458-NDX\")(condition (broken (condition (broken (condition (pristine ))(missing ((kind (name \"L-6678-RYH\")(condition (pristine ))) ))))(missing ((kind (name \"P-9247-WCO\")(condition (pristine ))) ))))) ((kind (name \"R-1623-SJU\")(condition (pristine ))) )))))(piled_on ((item (name \"X-1403-GIE\")(description \"an exemplary instance of part number X-1403-GIE\")(adjectives ((adjective \"cinnamon\") ))(condition (broken (condition (broken (condition (pristine ))(missing ((kind (name \"B-0010-LLL\")(condition (pristine ))) ))))(missing ((kind (name \"F-1623-QOS\")(condition (broken (condition (broken (condition (pristine ))(missing ((kind (name \"J-4292-VRZ\")(condition (pristine ))) ))))(missing ((kind (name \"N-9887-AUI\")(condition (pristine ))) ))))) ))))(piled_on ((item (name \"T-4832-BFV\")(description \"an exemplary instance of part number T-4832-BFV\")(adjectives ((adjective \"gray20\") ))(condition (pristine ))(piled_on ((item (name \"D-6458-HSR\")(description \"an exemplary instance of part number D-6458-HSR\")(adjectives ((adjective \"beige\") ))(condition (broken (condition (pristine ))(missing ((kind (name \"H-5065-MVY\")(condition (pristine ))) ))))(piled_on ((item (name \"F-4832-DAX\")(description \"an exemplary instance of part number F-4832-DAX\")(adjectives ((adjective \"ghost-white\") ))(condition (broken (condition (pristine ))(missing ((kind (name \"J-1403-IDG\")(condition (pristine ))) ((kind (name \"D-6458-HSR\")(condition (broken (condition (pristine ))(missing ((kind (name \"H-5065-MVY\")(condition (pristine ))) ))))) ((kind (name \"V-4292-XMD\")(condition (broken (condition (pristine ))(missing ((kind (name \"Z-9887-CPK\")(condition (pristine ))) ))))) ((kind (name \"N-0010-NGN\")(condition (broken (condition (pristine ))(missing ((kind (name \"R-1623-SJU\")(condition (pristine ))) ))))) )))))))(piled_on ((item (name \"V-4292-XMD\")(description \"an exemplary instance of part number V-4292-XMD\")(adjectives ((adjective \"olive-green\") ))(condition (broken (condition (pristine ))(missing ((kind (name \"Z-9887-CPK\")(condition (pristine ))) ))))(piled_on ((item (name \"H-4292-ZHF\")(description \"an exemplary instance of part number H-4292-ZHF\")(adjectives ((adjective \"light-brown\") ))(condition (broken (condition (pristine ))(missing ((kind (name \"L-9887-EKM\")(condition (pristine ))) ))))(piled_on ((item (name \"X-6678-TTJ\")(description \"an exemplary instance of part number X-6678-TTJ\")(adjectives ((adjective \"lawn-green\") ))(condition (broken (condition (broken (condition (pristine ))(missing ((kind (name \"B-9247-YWQ\")(condition (pristine ))) ))))(missing ((kind (name \"X-9887-GFO\")(condition (pristine ))) ((kind (name \"F-4832-DAX\")(condition (broken (condition (pristine ))(missing ((kind (name \"J-1403-IDG\")(condition (pristine ))) ))))) )))))(piled_on ((item (name \"T-4292-BCH\")(description \"an exemplary instance of part number T-4292-BCH\")(adjectives ((adjective \"aquamarine\") ))(condition (broken (condition (pristine ))(missing ((kind (name \"X-9887-GFO\")(condition (pristine ))) ((kind (name \"X-6678-TTJ\")(condition (broken (condition (broken (condition (pristine ))(missing ((kind (name \"B-9247-YWQ\")(condition (pristine ))) ))))(missing ((kind (name \"X-9887-GFO\")(condition (pristine ))) ))))) )))))(piled_on ((item (name \"P-6458-JNT\")(description \"an exemplary instance of part number P-6458-JNT\")(adjectives ((adjective \"maroon\") ))(condition (broken (condition (pristine ))(missing ((kind (name \"T-5065-OQC\")(condition (pristine ))) ))))(piled_on ((item (name \"Z-0010-PBP\")(description \"an exemplary instance of part number Z-0010-PBP\")(adjectives ((adjective \"rust\") ))(condition (broken (condition (pristine ))(missing ((kind (name \"D-1623-UEW\")(condition (pristine ))) ((kind (name \"H-4292-ZHF\")(condition (broken (condition (pristine ))(missing ((kind (name \"L-9887-EKM\")(condition (pristine ))) ))))) )))))(piled_on ((item (name \"F-5065-QLE\")(description \"an exemplary instance of part number F-5065-QLE\")(adjectives ((adjective \"cyan\") ))(condition (broken (condition (broken (condition (broken (condition (pristine ))(missing ((kind (name \"J-6678-VOL\")(condition (pristine ))) ))))(missing ((kind (name \"N-9247-ARS\")(condition (pristine ))) ))))(missing ((kind (name \"Z-0010-PBP\")(condition (broken (condition (pristine ))(missing ((kind (name \"D-1623-UEW\")(condition (pristine ))) ))))) ((kind (name \"R-4832-FUZ\")(condition (broken (condition (pristine ))(missing ((kind (name \"V-1403-KXI\")(condition (pristine ))) ))))) )))))(piled_on ((item (name \"V-9887-KUS\")(description \"an exemplary instance of part number V-9887-KUS\")(adjectives ((adjective \"lavender-blush\") ))(condition (broken (condition (pristine ))(missing ((kind (name \"H-1403-MSK\")(condition (pristine ))) ))))(piled_on ((item (name \"B-6458-LIV\")(description \"an exemplary instance of part number B-6458-LIV\")(adjectives ((adjective \"olive-drab\") ))(condition (pristine ))(piled_on ((item (name \"D-5065-UBI\")(description \"an exemplary instance of part number D-5065-UBI\")(adjectives ((adjective \"plum\") ))(condition (broken (condition (broken (condition (broken (condition (pristine ))(missing ((kind (name \"R-5065-SGG\")(condition (pristine ))) ))))(missing ((kind (name \"V-6678-XJN\")(condition (pristine ))) ))))(missing ((kind (name \"Z-9247-CMU\")(condition (pristine ))) ((kind (name \"T-4292-BCH\")(condition (broken (condition (pristine ))(missing ((kind (name \"X-9887-GFO\")(condition (pristine ))) ))))) )))))(piled_on ((item (name \"L-9247-EHW\")(description \"an exemplary instance of part number L-9247-EHW\")(adjectives ((adjective \"magenta\") ))(condition (broken (condition (pristine ))(missing ((kind (name \"P-4832-JKF\")(condition (broken (condition (pristine ))(missing ((kind (name \"T-1403-ONM\")(condition (pristine ))) ))))) ((kind (name \"D-5065-UBI\")(condition (broken (condition (broken (condition (broken (condition (pristine ))(missing ((kind (name \"R-5065-SGG\")(condition (pristine ))) ))))(missing ((kind (name \"V-6678-XJN\")(condition (pristine ))) ))))(missing ((kind (name \"Z-9247-CMU\")(condition (pristine ))) ))))) ((kind (name \"P-1623-WYY\")(condition (pristine ))) ))))))(piled_on ((item (name \"P-1623-WYY\")(description \"an exemplary instance of part number P-1623-WYY\")(adjectives ((adjective \"ochre\") ))(condition (pristine ))(piled_on ((item (name \"D-4832-HPD\")(description \"an exemplary instance of part number D-4832-HPD\")(adjectives ((adjective \"gray60\") ))(condition (broken (condition (pristine ))(missing ((kind (name \"H-1403-MSK\")(condition (broken (condition (pristine ))(missing ((kind (name \"L-0010-RVR\")(condition (pristine ))) ))))) ))))(piled_on ((item (name \"B-1623-YTC\")(description \"an exemplary instance of part number B-1623-YTC\")(adjectives ((adjective \"chestnut\") ))(condition (broken (condition (pristine ))(missing ((kind (name \"F-4292-DWJ\")(condition (pristine ))) ((kind (name \"Z-6458-PXZ\")(condition (broken (condition (pristine ))(missing ((kind (name \"J-9887-IAQ\")(condition (pristine ))) ))))) )))))(piled_on ((item (name \"Z-6458-PXZ\")(description \"an exemplary instance of part number Z-6458-PXZ\")(adjectives ((adjective \"rust\") ))(condition (broken (condition (pristine ))(missing ((kind (name \"N-6458-NDX\")(condition (pristine ))) ))))(piled_on ((item (name \"Z-6458-PXZ\")(description \"an exemplary instance of part number Z-6458-PXZ\")(adjectives ((adjective \"robin-egg-blue\") ))(condition (broken (condition (pristine ))(missing ((kind (name \"J-9887-IAQ\")(condition (pristine ))) ))))(piled_on ((item (name \"display\")(description \"a handheld device for showing textual data\")(adjectives )(condition (broken (condition (pristine ))(missing ((kind (name \"L-9247-EHW\")(condition (broken (condition (pristine ))(missing ((kind (name \"P-4832-JKF\")(condition (broken (condition (pristine ))(missing ((kind (name \"T-1403-ONM\")(condition (pristine ))) ))))) ))))) ((kind (name \"B-1623-YTC\")(condition (broken (condition (pristine ))(missing ((kind (name \"F-4292-DWJ\")(condition (pristine ))) ))))) ((kind (name \"R-4292-FRL\")(condition (broken (condition (broken (condition (pristine ))(missing ((kind (name \"V-9887-KUS\")(condition (pristine ))) ))))(missing ((kind (name \"Z-6458-PXZ\")(condition (broken (condition (pristine ))(missing ((kind (name \"D-5065-UBI\")(condition (pristine ))) ))))) ))))) ((kind (name \"N-1623-AOE\")(condition (pristine ))) )))))))(piled_on ((item (name \"X-0010-TQT\")(description \"an exemplary instance of part number X-0010-TQT\")(adjectives ((adjective \"navajo-white\") ))(condition (broken (condition (pristine ))(missing ((kind (name \"H-6678-ZEP\")(condition (pristine ))) ))))(piled_on )) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))))))";
            var roomInfo = SexpToItems.parseStack(rawSexp);
            System.out.println(roomInfo);
            StackSolver solver = new StackSolver(roomInfo._2, new ArrayList<>(), new Item("display", null, Set.of()));
            List<String> plan = solver.solve();
            System.out.println("Plan:");
            for (String step : plan) {
                System.out.println(step);
            }

        }



    }

}
