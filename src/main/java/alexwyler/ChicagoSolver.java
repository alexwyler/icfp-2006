package alexwyler;

import alexwyler.IO.AsyncCallResponseIO;
import alexwyler.IO.SystemInOut;
import alexwyler.StackSolver.Item;

import java.util.List;
import java.util.Set;

public class ChicagoSolver {

    final AsyncCallResponseIO asyncIO;

    public ChicagoSolver(AsyncCallResponseIO asyncIO) {
        this.asyncIO = asyncIO;
    }

    public void log(Object message) {
        asyncIO.log(String.valueOf(message));
    }

    public void solve() {
        List<Item> coreNeeds = SexpToItems.parseStack(asyncIO.call("examine"))._2;
        log("Core needs: " + coreNeeds);
        for (Item coreNeed : coreNeeds) {
            asyncIO.call("take " + coreNeed.name(), true);
            log("Inventory: " + SexpToItems.parseStack(asyncIO.call("inventory")));
            for (Item coreNeedMissing : coreNeed.missing()) {
                log("Trying to build " + coreNeedMissing);
                boolean found = dfsLooking(coreNeedMissing, new java.util.HashSet<>());
                if (!found) {
                    throw new RuntimeException("Could not build " + coreNeedMissing);
                } else {
                    log("Built " + coreNeedMissing);
                    log("Inventory: " + SexpToItems.parseStack(asyncIO.call("inventory")));
                    asyncIO.call("combine " + coreNeed.name() + " with " + coreNeedMissing.name(), true);
                    log(SexpToItems.parseStack(asyncIO.call("inventory"))._2);
                    log(SexpToItems.parseStack(asyncIO.call("examine"))._3);

                }
            }
        }

    }

    private boolean dfsLooking(Item lookingFor, Set<String> visitedRooms) {
        String rawSexp = asyncIO.call("examine");
        var roomInfo = SexpToItems.parseStack(rawSexp);
        if (!roomInfo._1) {
            return false;
        }
        var roomName = roomInfo._3;
        if (!visitedRooms.add(roomName)) {
            return false;
        }
        var inRoom = roomInfo._2;
        log("In room " + roomName + ", items: " + inRoom);
        var foundInRoom = inRoom.stream()
            .filter(lf -> lf.name().equals(lookingFor.name()))
            .findFirst()
            .orElse(null);

        if (foundInRoom != null) {
            log("Found " + lookingFor + " in room " + roomName + ", room: " + inRoom);
            log("Raw room info: " + rawSexp);
            var inventory = SexpToItems.parseStack(asyncIO.call("inventory"))._2;
            log("Inventory: " + inventory);
            log("Solving to get " + lookingFor + " from " + inRoom + " with inventory " + inventory);
            StackSolver stackSolver = new StackSolver(roomInfo._2, inventory, lookingFor);
            var plan = stackSolver.solve();
            log("Plan to get " + lookingFor + ": " + plan);
            plan.forEach(planLine -> {
                log("Inventory: " + SexpToItems.parseStack(asyncIO.call("inventory", true))._2);
                if (!SexpToItems.parseStack(asyncIO.call(planLine, true))._1) {
                    throw new RuntimeException("Failed to execute plan line: " + planLine + " to get " + lookingFor);
                }
            });
            return true;
        }

        for (var directions : List.of(List.of("north", "south"), List.of("east", "west"))) {
            for (var direction : directions) {
                var goInfo = SexpToItems.parseStack(asyncIO.call("go " + direction, true));
                if (!goInfo._1) {
                    continue;
                }

                boolean result = dfsLooking(lookingFor, visitedRooms);
                String opposite = directions.get((directions.indexOf(direction) + 1) % 2);
                asyncIO.call("go " + opposite, true);
                return result;
            }
        }

        return false;
    }


}
