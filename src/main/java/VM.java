import io.vavr.control.Try;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class VM {

    private static final int OP_CMOV = 0;
    private static final int OP_INDEX = 1;
    private static final int OP_AMEND = 2;
    private static final int OP_ADD = 3;
    private static final int OP_MUL = 4;
    private static final int OP_DIV = 5;
    private static final int OP_NAND = 6;
    private static final int OP_HALT = 7;
    private static final int OP_ALLOC = 8;
    private static final int OP_ABANON = 9;
    private static final int OP_OUT = 10;
    private static final int OP_IN = 11;
    private static final int OP_LOAD = 12;
    private static final int OP_ORTHO = 13;
    final private int[] registers = new int[8];
    private final PlatterArrays arrays;
    private int[] program;
    private int pc = 0;

    private final Supplier<Integer> in;
    private final Consumer<Integer> out;

    public VM(int[] program, final Supplier<Integer> in, final Consumer<Integer> out) {
        this.in = in;
        this.out = out;
        arrays = new PlatterArrays();
        arrays.alloc(program.length);
        arrays.set(0, program);
        this.program = program;
    }


    public void run() {
        while (true) {
            int instr = program[pc++];
            int op = instr >>> 28;
            if (op == OP_ORTHO) {
                int A = (instr >>> 25) & 7;
                int val = instr & 0x1FFFFFF;
                //System.out.println("pc=" + (pc - 1) + " instr=" + Integer.toHexString(instr) + " op=" + OP_ORTHO + " A=" + A + " val=" + val);
                registers[A] = val;
            } else {
                int A = (instr >>> 6) & 7;
                int B = (instr >>> 3) & 7;
                int C = instr & 7;

                //System.out.println("pc=" + (pc - 1) + " instr=" + Integer.toHexString(instr) + " op=" + op + " A=" + A + " B=" + B + " C=" + C);

                try {
                    switch (op) {
                        case OP_CMOV:
                            if (registers[C] != 0) {
                                registers[A] = registers[B];
                            }
                            break;
                        case OP_INDEX:
                            registers[A] = arrays.get(registers[B])[registers[C]];
                            break;
                        case OP_AMEND:
                            arrays.amend(registers[A], registers[B], registers[C]);
                            break;
                        case OP_ADD:
                            registers[A] = (registers[B] + registers[C]);
                            break;
                        case OP_MUL:
                            registers[A] = (int)((registers[B] & 0xFFFFFFFFL) * (registers[C] & 0xFFFFFFFFL));
                            break;
                        case OP_DIV:
                            registers[A] = Integer.divideUnsigned(registers[B], registers[C]);
                            break;
                        case OP_NAND:
                            registers[A] = ~(registers[B] & registers[C]);
                            break;
                        case OP_HALT:
                            return;
                        case OP_ALLOC:
                            registers[B] = arrays.alloc(registers[C]);
                            break;
                        case OP_ABANON:
                            arrays.abandon(registers[C]);
                            break;
                        case OP_OUT:
                            out.accept(registers[C] & 0xFF);
                            break;
                        case OP_IN:
                            registers[C] = in.get();
                            break;
                        case OP_LOAD:
                            program = arrays.load(registers[B]);
                            pc = registers[C];
                            break;
                        default:
                            throw new IllegalStateException("Invalid opcode " + op);
                    }
                } catch (RuntimeException e) {
                    System.err.println("Error at pc=" + (pc - 1) + " instr=" + Integer.toHexString(instr) + " op=" + op + " A=" + A + " B=" + B + " C=" + C);
                    throw e;
                }
            }
        }
    }

    static int[] decodeProgram(String resource) {
        try (InputStream in = VM.class.getResourceAsStream(resource)) {
            byte[] bytes = IOUtils.toByteArray(in);
            int[] program = new int[bytes.length / 4];
            ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).asIntBuffer().get(program);
            return program;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException, URISyntaxException {
        //runCodex();

        runDump();
    }

    private static void runDump() {
        int[] program = decodeProgram("/dump-1757795378648.um");
        VM vm = new VM(program, () -> Try.of(() -> System.in.read()).get(), (v) -> System.out.print((char) (int) v));
        vm.run();
    }

    public static void runCodex() {
        int[] program = decodeProgram("/codex.umz");
        final AtomicInteger keyIndex = new AtomicInteger(0);
        String keyToInject = "(\\b.bb)(\\v.vv)06FHPVboundvarHRAk";
        AtomicBoolean decryptNextInput = new AtomicBoolean(false);
        StringBuilder lastOutput = new StringBuilder();
        AtomicBoolean dumping = new AtomicBoolean(false);
        File dumpFile = new File("dump-" + System.currentTimeMillis() + ".um");
        try (OutputStream dumpOut = new BufferedOutputStream(new FileOutputStream(dumpFile))) {

            Supplier<Integer> in = () -> {
                if (decryptNextInput.get()) {
                    if (keyIndex.get() < keyToInject.length()) {
                        return (int) keyToInject.charAt(keyIndex.getAndIncrement());
                    } else {
                        decryptNextInput.set(false);
                        keyIndex.set(0);
                        return (int) '\n';
                    }
                } else {
                    try {
                        int read = System.in.read();
                        return read;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            };

            Consumer<Integer> out = (v) -> {
                char c = (char) (int) v;

                // Write to dump if active
                if (dumping.get()) {
                    try {
                        dumpOut.write(v);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    lastOutput.append(c);
                    // Trigger key injection
                    if (lastOutput.toString().endsWith("enter decryption key:")) {
                        decryptNextInput.set(true);
                        lastOutput.setLength(0);
                    }
                    // Trigger dumping based on specific prompt
                    if (lastOutput.toString().endsWith("UM program follows colon:")) {
                        dumping.set(true);
                        lastOutput.setLength(0);
                    }
                    System.out.print(c);
                }
            };

            VM vm = new VM(program, in, out);
            vm.run();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
