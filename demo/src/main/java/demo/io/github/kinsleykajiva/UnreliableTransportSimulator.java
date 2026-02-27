package demo.io.github.kinsleykajiva;

import java.util.Random;

public class UnreliableTransportSimulator {
    private static final int UT_BUF = 160;
    private int[] buffer = new int[UT_BUF];
    private int index;
    private Random random = new Random();

    public UnreliableTransportSimulator() {
        init();
    }

    public void init() {
        for (int i = 0; i < UT_BUF; i++) {
            buffer[i] = i;
        }
        index = UT_BUF - 1;
        shuffle();
    }

    private void shuffle() {
        for (int i = buffer.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = buffer[i];
            buffer[i] = buffer[j];
            buffer[j] = temp;
        }
    }

    public int nextIndex() {
        int next = buffer[0];
        index++;
        buffer[0] = index;
        shuffle();
        return next;
    }
}
