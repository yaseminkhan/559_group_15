package com.server;

public class LogicalClock {
    private int time;

    record Send(Object m, int sequenceNo) {
    }

    record Receive(Object m, int sequenceNo) {
    }

    public LogicalClock() {
        time = 0;
    }

    public void tick() {
        ++time;
    }

    // public void onEvent(Object event) {
    //     switch (event) {
    //         case Send(Object m, int sequenceNo) -> {
    //             System.out.println("SEND " + m);
    //         }
    //         case Receive(Object m, int sequenceNo) -> {
    //             System.out.println("RECEIVE " + m);
    //         }
    //         default -> System.out.println("Unexpected value.");
    //     }
    // }

    // public static void main(String[] args) {
    //     var clk = new LogicalClock();
    //     var e1 = new Send("MESSAGE", 0);
    //     var e2 = new Receive("MESSAGE 2", 9);

    //     clk.onEvent(e1);
    //     clk.onEvent(e2);
    // }
}
