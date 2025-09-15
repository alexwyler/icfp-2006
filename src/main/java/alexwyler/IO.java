package alexwyler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public interface IO {

    IntSupplier getIn();

    IntConsumer getOut();

    boolean isDone();

    class SystemInOut implements IO {

        final IntSupplier in;
        final IntConsumer out;

        public SystemInOut() {
            in = () -> {
                try {
                    return System.in.read();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            };
            out = (it) -> System.out.print((char) (int) it);
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
            return false;
        }

    }

    class FileScript implements IO {

        final String resource;
        boolean exhausted = false;
        final IntSupplier in;
        final IntConsumer out;

        public FileScript(String resource) {
            this.resource = resource;
            try (InputStream in = IO.class.getClassLoader()
                .getResourceAsStream(resource)) {

                if (in == null) {
                    throw new IllegalArgumentException("Resource not found: " + resource);
                }

                byte[] data = in.readAllBytes();

                this.in = new IntSupplier() {
                    int index = 0;

                    @Override
                    public int getAsInt() {
                        int ret = data[index++] & 0xFF; // unsigned 8-bit
                        System.out.print((char) ret);
                        if (index == data.length) {
                            exhausted = true;
                        }
                        return ret;

                    }

                };
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            this.out = (it) -> System.out.print((char) (int) it);
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

    class StringIO implements IO {

        final String commands;
        final IntSupplier in;
        final IntConsumer out;
        boolean exhausted = false;

        public StringIO(String commands) {
            this.commands = commands;
            byte[] data = commands.getBytes(StandardCharsets.UTF_8);

            this.in = new IntSupplier() {
                int index = 0;

                @Override
                public int getAsInt() {
                    int ret = data[index++] & 0xFF; // unsigned 8-bit
                    System.out.print((char) ret);
                    if (index == data.length) {
                        exhausted = true;
                    }
                    return ret;

                }

            };

            this.out = (it) -> System.out.print((char) (int) it);
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

}
