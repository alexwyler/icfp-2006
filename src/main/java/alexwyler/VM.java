package alexwyler;

import alexwyler.IO.FileScript;
import alexwyler.IO.StringIO;
import alexwyler.IO.SystemInOut;
import alexwyler.StackSolver.Item;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntConsumer;

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
    private final PlatterArrays arrays;
    final private int[] registers = new int[8];
    private int[] program;
    private int pc = 0;

    private final List<IO> ios;

    public VM(int[] program, final List<IO> ios) {
        this.ios = ios;
        arrays = new PlatterArrays();
        arrays.alloc(program.length);
        arrays.set(0, program);
        this.program = program;
    }


    public CompletableFuture<Void> runAsync() {
        return CompletableFuture.runAsync(this::run);
    }

    private void run() {
        var registers = this.registers;
        var arrays = this.arrays;
        var pc = this.pc;
        var program = this.program;
        int ioIndex = -1;
        IO io = null;
        try {
            while (true) {
                int instr = program[pc++];
                int op = instr >>> 28;
                if (op == OP_ORTHO) {
                    int A = (instr >>> 25) & 7;
                    int val = instr & 0x1FFFFFF;
                    //System.out.println("pc=" + (pc - 1) + " instr=" + Integer.toHexString(instr) + " op=" + OP_ORTHO + " A=" + A + " val=" + val);
                    registers[A] = val;
                    continue;
                }
                int A = (instr >>> 6) & 7;
                int B = (instr >>> 3) & 7;
                int C = instr & 7;

                //System.out.println("pc=" + (pc - 1) + " instr=" + Integer.toHexString(instr) + " op=" + op + " A=" + A + " B=" + B + " C=" + C);

                switch (op) {
                    case OP_CMOV:
                        if (registers[C] != 0) {
                            registers[A] = registers[B];
                        }
                        break;
                    case OP_INDEX: {
                        int index = registers[B];
                        int offset = registers[C];
                        if (index == 0) {
                            registers[A] = program[offset];
                        } else {
                            registers[A] = arrays.get(index)[offset];
                        }
                    }
                    break;
                    case OP_AMEND: {
                        int index = registers[A];
                        int offset = registers[B];
                        if (index == 0 && arrays.activeAlias == -1) {
                            program[offset] = registers[C];
                        } else {
                            arrays.amend(index, offset, registers[C]);
                        }
                    }
                    break;
                    case OP_ADD:
                        registers[A] = (registers[B] + registers[C]);
                        break;
                    case OP_MUL:
                        registers[A] = (int) ((registers[B] & 0xFFFFFFFFL) * (registers[C] & 0xFFFFFFFFL));
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
                        if (io == null || io.isDone()) {
                            io = ios.get(++ioIndex);
                        }
                        IntConsumer out = io.getOut();
                        out.accept(registers[C] & 0xFF);
                        break;
                    case OP_IN:
                        if (io == null || io.isDone()) {
                            io = ios.get(++ioIndex);
                        }
                        registers[C] = io.getIn().getAsInt();
                        break;
                    case OP_LOAD:
                        program = arrays.load(registers[B]);
                        pc = registers[C];
                        break;
                    default:
                        throw new IllegalStateException("Invalid opcode " + op);
                }
            }
        } finally {
            this.pc = pc;
            this.program = program;
        }
    }
}
