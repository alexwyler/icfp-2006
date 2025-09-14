import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

class PlatterArrays {
    final private ArrayList<int[]> allocated = new ArrayList<>();
    final private IntStack abandoned = new IntStack();

    // Copy-on-write between array 0 and the last-loaded array
    public int activeAlias = -1;
    // Only copy up to the highest written offset when copyng-on-write
    final private IntArrayList highestWrittenOffset = new IntArrayList();

    public int[] get(int index) {
        return allocated.get(index);
    }

    public void set(int index, int[] dest) {
        int largestWriteIndex = -1;
        for (int i = dest.length - 1; i >= 0; --i) {
            if (dest[i] != 0) {
                largestWriteIndex = i;
                break;
            }
        }

        highestWrittenOffset.set(index, largestWriteIndex);
        allocated.set(index, dest);
    }

    public int alloc(int numPlatters) {
        var array = new int[numPlatters];
        final int index;
        if (!abandoned.isEmpty()) {
            index = abandoned.pop();
            allocated.set(index, array);
            highestWrittenOffset.set(index, -1);

        } else {
            index = allocated.size();
            allocated.add(array);
            highestWrittenOffset.add(-1);
        }
        return index;
    }

    public void abandon(int index) {
        if (activeAlias == index) {
            activeAlias = -1;
        }
        abandoned.push(index);
    }

    public int[] load(int index) {
        var program = allocated.get(index);
        if (index != 0) {
            highestWrittenOffset.set(0, highestWrittenOffset.getInt(index));
            allocated.set(0, program);
            activeAlias = index;
        }
        return program;
    }

    public void amend(int index, int offset, int value) {
        if ((activeAlias == index && activeAlias != 0) || (index == 0 && activeAlias > 0)) {
            int[] curProgram = allocated.getFirst();
            int largestWriteIndexProgram = highestWrittenOffset.getFirst();
            int[] allocatedCopy = new int[curProgram.length];
            System.arraycopy(curProgram, 0, allocatedCopy, 0, largestWriteIndexProgram + 1);
            allocated.set(index, allocatedCopy);
            activeAlias = -1;
        }

        int highestWrittenOffsetTarget = this.highestWrittenOffset.getInt(index);
        int[] target = allocated.get(index);
        target[offset] = value;
        if (value != 0 && (offset > highestWrittenOffsetTarget)) {
            highestWrittenOffset.set(index, offset);
        }
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
