import java.io.IOException;
import java.io.InputStream;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class ChicagoGridExplorerIO implements IO {

    boolean exhausted = false;
    final IntSupplier in;
    final IntConsumer out;

    public ChicagoGridExplorerIO() {
        in = () -> {
            // todo
            return -1;
        };
        out = (it) -> {
            // todo
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
