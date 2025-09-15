package alexwyler;

import alexwyler.StackSolver.Item;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class StackSolverIO implements IO {

    final IntSupplier in;
    final IntConsumer out;
    boolean exhausted = false;
    StringBuilder sb = new StringBuilder();
    final Item targetItem;

    public StackSolverIO(final Item targetItem) {
        this.targetItem = targetItem;
        AtomicInteger planStep = new AtomicInteger(0);
        AtomicInteger charIndex = new AtomicInteger(0);
        List<String> plan = new ArrayList<>();
        in = () -> {
            if (plan.isEmpty()) {
                System.out.println(sb.toString());
                var parsed = SexpToItems.parseStack(sb.toString());
                System.out.println(parsed);
                StackSolver stackSolver = new StackSolver(parsed, targetItem);
                plan.addAll(stackSolver.solve());
                System.out.println(plan);
            }

//            if (planStep.get() >= 0) {
//                try {
//                    return System.in.read();
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            }

            String current = plan.get(planStep.get());
            int ret;
            if (charIndex.get() == current.length()) {
                charIndex.set(0);
                planStep.incrementAndGet();
                ret = '\n';
            } else {
                ret = current.charAt(charIndex.getAndIncrement());
            }

            if (planStep.get() >= plan.size()) {
                exhausted = true;
            }
            System.out.print((char) ret);
            return ret;
        };
        out = it -> {
            if (plan.isEmpty()) {
                sb.append((char) it);
            }
            System.out.print((char) it);
        };
    }

    @Override
    public IntSupplier getIn() {
        return in;
    }

    @Override
    public IntConsumer getOut() {
        return out;
    }

    @Override
    public boolean isDone() {
        return exhausted;
    }

}
