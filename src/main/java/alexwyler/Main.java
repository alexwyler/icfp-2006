package alexwyler;

import alexwyler.IO.FileScript;
import alexwyler.IO.SystemInOut;
import alexwyler.StackSolver.Item;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

public class Main {


    public static void main(String[] args) throws IOException, URISyntaxException {
        ///runCodex();
        //runSandmark();
        //runDump("adventure_examine_input.txt");
        runUmixAdventure();

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

    private static void runDump(String inputFile) {

        List<IO> ios = List.of(new FileScript(inputFile), new SystemInOut());

        int[] program = decodeProgram("/dump-1757795378648.um");
        VM vm = new VM(program, ios);
        long start = System.currentTimeMillis();
        vm.runAsync().join();
        System.out.println("took " + (System.currentTimeMillis() - start) + "ms");
    }

    static void runUmixAdventure() {
        var asyncIO = new IO.AsyncCallResponseIO();
        VM vm = new VM(decodeProgram("/umix.um"), List.of(asyncIO, new SystemInOut()));
        var vmCF = vm.runAsync();

        asyncIO.call("""
            howie
            xyzzy
        """);
        asyncIO.call("""
            adventure
            switch sexp
        """);
        {
            asyncIO.call("""
                go north
                take bolt
                """);
            var inventory = SexpToItems.parseStack(asyncIO.call("inventory"))._2;
            var parsed = SexpToItems.parseStack(asyncIO.call("examine"));
            var stackSolver = new StackSolver(parsed._2, inventory, new Item("keypad", null, List.of()));
            stackSolver.solve().forEach(asyncIO::call);
        }
        asyncIO.call("""
            go south
            use keypad
            inc keypad
            take /etc/passwd
            inc /etc/passwd
            take note
            inc note
        """);

        ChicagoSolver solver = new ChicagoSolver(asyncIO);
        solver.solve();


//        {
//            String chicagoSexp = asyncIO.call("inventory");
//            System.out.println(chicagoSexp);
//            var chicagoItems = SexpToItems.parseStack(chicagoSexp)._2;
//            System.out.println(chicagoItems);
//        }


        asyncIO.exaust();
        vmCF.join();

    }


    private static void runSandmark() {
        int[] program = decodeProgram("/sandmark.umz");
        List<IO> ios = List.of(new SystemInOut());
        VM vm = new VM(program, ios);
        long start = System.currentTimeMillis();
        vm.runAsync().join();
        System.out.println("took " + (System.currentTimeMillis() - start) + "ms");
    }


}
