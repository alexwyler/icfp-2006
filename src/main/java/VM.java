import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

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
    public VM(int[] program) {
        arrays = new PlatterArrays();
        arrays.alloc(program.length);
        arrays.set(0, program);
        this.program = program;
    }


    public void run() throws IOException {
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
                            arrays.get(registers[A])[registers[B]] = registers[C];
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
                            System.out.print((char) (registers[C] & 0xFF));
                            break;
                        case OP_IN:
                            registers[C] = System.in.read();
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

    static int[] decodeProgram(String resource) throws IOException {
        try (InputStream in = VM.class.getResourceAsStream(resource)) {
            byte[] bytes = IOUtils.toByteArray(in);
            int[] program = new int[bytes.length / 4];
            ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).asIntBuffer().get(program);
            return program;
        }
    }

    public static void main(String[] args) throws IOException, URISyntaxException {
        int[] program = decodeProgram("/codex.umz");

        VM vm = new VM(program);
        vm.run();
    }
}
