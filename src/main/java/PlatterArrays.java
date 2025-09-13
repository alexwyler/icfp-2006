import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;

class PlatterArrays {

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
        abandoned.add(index);
    }

    public int[] load(int index) {
        var program = Arrays.copyOf(allocated.get(index), allocated.get(index).length);
        allocated.set(0, program);
        return program;
    }

}
