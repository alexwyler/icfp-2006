import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;

class PlatterArrays {

    int lastLoadedIndex = -1;
    final private ArrayList<int[]> allocated = new ArrayList<>();
    final private ArrayDeque<Integer> abandoned = new ArrayDeque<>();

    public int[] get(int index) {
        return allocated.get(index);
    }

    public int[] set(int index, int[] dest) {
        return allocated.set(index, dest);
    }

    public int alloc(int numPlatters) {
        var array = new int[numPlatters];
        final int index;
        if (!abandoned.isEmpty()) {
            index = abandoned.removeLast();
            allocated.set(index, array);
        } else {
            index = allocated.size();
            allocated.add(array);
        }
        return index;
    }

    public void abandon(int index) {
        allocated.set(index, null);
        if (lastLoadedIndex == index) {
            lastLoadedIndex = 0;
        }
        abandoned.add(index);
    }

    public int[] load(int index) {
        var program = allocated.get(index);
        allocated.set(0, program);
        lastLoadedIndex = index;
        return program;
    }

    public void amend(int index, int offset, int value) {
        if (lastLoadedIndex == index) {
            int[] curProgram = allocated.get(0);
            allocated.set(index, Arrays.copyOf(curProgram, curProgram.length));
            lastLoadedIndex = -1;
        }
        allocated.get(index)[offset] = value;
    }

}
