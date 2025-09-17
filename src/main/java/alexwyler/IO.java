package alexwyler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

public interface IO {

    IntSupplier getIn();

    IntConsumer getOut();

    boolean isDone();

    default void log(Object message) {
        System.out.println("====LOG====> " + message);
    }

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

    class AsyncCallResponseIO implements IO {

        final IntSupplier asyncIn;
        final IntConsumer asyncOut;
        volatile boolean log = false;

        private final BlockingQueue<Integer> inputQueue = new LinkedBlockingQueue<>();
        private final BlockingQueue<String> outputQueue = new LinkedBlockingQueue<>();
        private final StringBuffer outputBuffer = new StringBuffer();


        public AsyncCallResponseIO() {

            asyncIn = () -> {
                if (inputQueue.isEmpty()) {
                    outputQueue.add(outputBuffer.toString());
                    outputBuffer.setLength(0);
                }
                try {
                    int ret = inputQueue.take();
                    if (log) {
                        System.out.print((char) ret);
                    }
                    return ret;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            };
            asyncOut = (it) -> {
                if (log) {
                    System.out.print((char) it);
                }
                outputBuffer.append((char) it);
            };
        }
        public String call(String command) {
            return call(command, false);
        }
        public String call(String command, boolean log) {
            this.log = log;
            command = Arrays.stream(command.split("\n"))
                .map(String::trim)
                .collect(Collectors.joining("\n"));

            var commandPlusEnter = command.endsWith("\n") ? command : command + "\n";
            for (char c : commandPlusEnter.toCharArray()) {
                try {
                    inputQueue.put((int) c);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            try {
                return outputQueue.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        boolean exausted = false;
        public void exaust() {
            exausted = true;
            inputQueue.add((int) '\n');
        }

        @Override
        public IntSupplier getIn() {
            return asyncIn;
        }

        @Override
        public IntConsumer getOut() {
            return asyncOut;
        }

        @Override
        public boolean isDone() {
            return exausted;
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
