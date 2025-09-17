package alexwyler;

import io.vavr.Tuple2;

import java.util.*;
import java.util.stream.IntStream;

public class StackSolver {

    static final int CAPACITY = 6;
    final List<Item> stack;
    final List<Item> inventory;
    final Item targetItem;

    public StackSolver(final List<Item> initialStack, List<Item> inventory, final Item targetItem) {
        this.stack = initialStack;
        this.targetItem = targetItem;
        this.inventory = inventory;
    }

    private static List<Tuple2<Integer, Integer>> allPairs(int n) {
        return IntStream.range(0, n)
            .boxed()
            .flatMap(i -> IntStream.range(i + 1, n)
                .mapToObj(j -> new Tuple2<>(i, j)))
            .toList();
    }



    static Tuple2<Item, Item> unorderedPair(Item a, Item b) {
        return a.compareTo(b) <= 0 ? new Tuple2<>(a, b) : new Tuple2<>(b, a);
    }

    private boolean dfs(
        int idx,
        List<Item> inv,
        Set<String> seen,
        List<String> plan
    ) {

        if (idx == stack.size() && inv.contains(targetItem)) {
            return true;
        }
        List<String> sigs = inv.stream().map(Item::toString).sorted().toList();
        if (!seen.add(idx + "|" + String.join(",", sigs))) {
            return false;
        }

        // try combine
        for (var p : allPairs(inv.size())) {
            Item a = inv.get(p._1);
            Item b = inv.get(p._2);
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

            inv.remove(a);
            inv.remove(b);
            inv.add(result);
            plan.add(planString);

            if (dfs(idx, inv, seen, plan)) {
                return true;
            }

            plan.remove((int) (plan.size() - 1));
            inv.remove((int) (inv.size() - 1));
            inv.add(p._1, a);
            inv.add(p._2, b);
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
        }

        // incinerate
        for (int i = 0; i < inv.size(); i++) {
            Item x = inv.get(i);
            if (x.equals(targetItem) || this.inventory.contains(x)) {
                continue;
            }
            inv.remove(i);
            plan.add("incinerate " + x.toPlanString());
            if (dfs(idx, inv, seen, plan)) {
                return true;
            }
            inv.add(i, x);
            plan.remove(plan.size() - 1);
        }

        return false;
    }

    public List<String> solve() {
        List<String> plan = new ArrayList<>();
        List<Item> inventory = new ArrayList<>(this.inventory);
        Set<String> seen = new HashSet<>();

        if (!dfs(0, inventory, seen, plan)) {
            throw new RuntimeException("No solution found");
        }

        for (Item x : inventory) {
            if (x.equals(targetItem) || this.inventory.contains(x)) {
                continue;
            }
            plan.add("incinerate " + x.toPlanString());
        }

        return plan;
    }

    public record Item(String name, String adjective, List<Item> missing) implements Comparable<Item> {

        public String toPlanString() {
            return (adjective != null ? adjective + " " : "") + name;
        }

        @Override
        public String toString() {
            return name + (adjective != null ? " (" + adjective + ")" : "") +
                   (missing.isEmpty() ? " *working* " : " missing=" + missing);
        }

        public boolean isEquivalent(Item other) {
            if (!this.name.equals(other.name)) {
                return false;
            }

            List<Item> otherMissing = new ArrayList<>(other.missing);

            if (this.missing.size() != otherMissing.size()) {
                return false;
            }

            boolean allMatched = this.missing.stream().allMatch(t -> {
                for (Item o : otherMissing) {
                    if (t.isEquivalent(o)) {
                        otherMissing.remove(o);
                        return true;
                    }
                }
                return false;
            });

            return allMatched;
        }

        public boolean canCombineWith(Item other) {
            if (this.name.equals(other.name)) {
                return false;
            }
            if (this.missing.isEmpty()) {
                return false;
            }

            if (!this.missing.stream()
                .anyMatch(r -> r.isEquivalent(other))) {
                return false;
            }
            return true;
        }

        public Item combineWith(Item other) {
            ArrayList<Item> newMissing = new ArrayList<>(this.missing);
            var toRemove = this.missing.stream()
                .filter(missing -> missing.isEquivalent(other))
                .findFirst()
                .get();
            newMissing.remove(toRemove);
            return new Item(this.name, this.adjective, newMissing);
        }

        @Override
        public int compareTo(final Item o) {
            return name.compareTo(o.name);
        }

    }

    public static final class CombineRules {

        final Map<Tuple2<Item, Item>, Item> pairToResult = new HashMap<>();

        public void add(Item a, Item b, Item result) {
            pairToResult.put(unorderedPair(a, b), result);
        }

        public Optional<Item> combine(Item a, Item b) {
            return Optional.ofNullable(pairToResult.get(unorderedPair(a, b)));
        }

        @Override
        public String toString() {
            return "Rules{" +
                   "pairToResult=" + pairToResult +
                   '}';
        }

    }

    record Key(int idx, List<Item> invCanon) {

        Key(int idx, Collection<Item> inv) {
            this(idx, sortedCopy(inv));
        }

        private static List<Item> sortedCopy(Collection<Item> inv) {
            List<Item> copy = new ArrayList<>(inv);
            Collections.sort(copy);
            return copy;
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
            String rawSexp = "(success (command (look (room (name \"54th Street and Dorchester Avenue\")(description \"You are standing at the corner of 54th Street and Dorchester Avenue. From here, you can go north, east, south, or west. \")(items ((item (name \"X-9247-GWE\")(description \"an exemplary instance of part number X-9247-GWE\")(adjectives ((adjective \"orange-red\") ))(condition (pristine ))(piled_on ((item (name \"V-0010-XBD\")(description \"an exemplary instance of part number V-0010-XBD\")(adjectives ((adjective \"magenta\") ))(condition (broken (condition (pristine ))(missing ((kind (name \"X-9247-GWE\")(condition (pristine ))) ))))(piled_on ((item (name \"F-1403-QDS\")(description \"an exemplary instance of part number F-1403-QDS\")(adjectives ((adjective \"pumpkin\") ))(condition (pristine ))(piled_on ((item (name \"P-5065-WQO\")(description \"an exemplary instance of part number P-5065-WQO\")(adjectives ((adjective \"heavy\") ))(condition (broken (condition (broken (condition (broken (condition (pristine ))(missing ((kind (name \"T-6678-BTV\")(condition (pristine ))) ))))(missing ((kind (name \"B-4832-LAL\")(condition (pristine ))) ))))(missing ((kind (name \"F-1403-QDS\")(condition (pristine ))) ))))(piled_on ((item (name \"B-4832-LAL\")(description \"an exemplary instance of part number B-4832-LAL\")(adjectives ((adjective \"taupe\") ))(condition (pristine ))(piled_on ((item (name \"L-6458-RNH\")(description \"an exemplary instance of part number L-6458-RNH\")(adjectives ((adjective \"gray40\") ))(condition (broken (condition (pristine ))(missing ((kind (name \"P-5065-WQO\")(condition (broken (condition (pristine ))(missing ((kind (name \"T-6678-BTV\")(condition (pristine ))) ))))) ))))(piled_on ((item (name \"T-9887-OFC\")(description \"an exemplary instance of part number T-9887-OFC\")(adjectives ((adjective \"eggplant\") ))(condition (broken (condition (broken (condition (pristine ))(missing ((kind (name \"X-6458-TIJ\")(condition (pristine ))) ))))(missing ((kind (name \"H-9887-MKY\")(condition (pristine ))) ))))(piled_on ((item (name \"Z-1623-CEK\")(description \"an exemplary instance of part number Z-1623-CEK\")(adjectives ((adjective \"indigo\") ))(condition (broken (condition (broken (condition (pristine ))(missing ((kind (name \"D-4292-HHR\")(condition (pristine ))) ))))(missing ((kind (name \"L-6458-RNH\")(condition (pristine ))) ))))(piled_on ((item (name \"H-9887-MKY\")(description \"an exemplary instance of part number H-9887-MKY\")(adjectives ((adjective \"yellow-green\") ))(condition (pristine ))(piled_on ((item (name \"F-6678-DOX\")(description \"an exemplary instance of part number F-6678-DOX\")(adjectives ((adjective \"shiny\") ))(condition (broken (condition (broken (condition (pristine ))(missing ((kind (name \"J-9247-IRG\")(condition (pristine ))) ))))(missing ((kind (name \"V-0010-XBD\")(condition (pristine ))) ))))(piled_on ((item (name \"R-1403-SXU\")(description \"an exemplary instance of part number R-1403-SXU\")(adjectives ((adjective \"pale-green\") ))(condition (pristine ))(piled_on ((item (name \"USB cable\")(description \"compatible with all high-speed Universal Sand Bus 2.0 devices\")(adjectives )(condition (broken (condition (broken (condition (broken (condition (pristine ))(missing ((kind (name \"T-9887-OFC\")(condition (broken (condition (pristine ))(missing ((kind (name \"X-6458-TIJ\")(condition (pristine ))) ))))) ))))(missing ((kind (name \"F-6678-DOX\")(condition (pristine ))) ))))(missing ((kind (name \"N-4832-NUN\")(condition (pristine ))) ))))(piled_on ((item (name \"N-4832-NUN\")(description \"an exemplary instance of part number N-4832-NUN\")(adjectives ((adjective \"sienna\") ))(condition (pristine ))(piled_on ((item (name \"J-9247-IRG\")(description \"an exemplary instance of part number J-9247-IRG\")(adjectives ((adjective \"slate-gray\") ))(condition (pristine ))(piled_on ((item (name \"B-5065-YLQ\")(description \"an exemplary instance of part number B-5065-YLQ\")(adjectives ((adjective \"dim-gray\") ))(condition (pristine ))(piled_on )) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))) ))))))";
            var roomInfo = SexpToItems.parseStack(rawSexp);
            StackSolver solver = new StackSolver(roomInfo._2, new ArrayList<>(), new Item("USB cable", null, List.of()));
            List<String> plan = solver.solve();
            System.out.println("Plan:");
            for (String step : plan) {
                System.out.println(step);
            }

        }



    }

}
