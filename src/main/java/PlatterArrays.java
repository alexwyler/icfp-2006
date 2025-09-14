import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

class PlatterArrays {
    final private ArrayList<int[]> allocated = new ArrayList<>();
    final private IntStack abandoned = new IntStack();
    public int[] get(int index) {
        return allocated.get(index);
    }

    public void set(int index, int[] dest) {
        allocated.set(index, dest);
    }

    public int alloc(int numPlatters) {
        var array = new int[numPlatters];
        final int index;
        if (!abandoned.isEmpty()) {
            index = abandoned.pop();
            allocated.set(index, array);
        } else {
            index = allocated.size();
            allocated.add(array);
        }
        return index;
    }

    public void abandon(int index) {
        abandoned.push(index);
    }

    public int[] load(int index) {
        var program = allocated.get(index);
        if (index != 0) {
            allocated.set(0, Arrays.copyOf(program, program.length));
        }
        return program;
    }

    public void amend(int index, int offset, int value) {
        int[] target = allocated.get(index);
        target[offset] = value;
    }

    final static class IntStack {
        private int[] stack = new int[1024];
        private int top = 0;

        void push(int value) {
            if (top == stack.length) {
                stack = Arrays.copyOf(stack, stack.length * 2);
            }
            stack[top++] = value;
        }

        int pop() {
            return stack[--top];
        }

        boolean isEmpty() {
            return top == 0;
        }
    }

}
