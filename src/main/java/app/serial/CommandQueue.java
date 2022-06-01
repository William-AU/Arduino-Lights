package app.serial;

import app.common.SerialConstants;
import com.fazecast.jSerialComm.SerialPort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Queue;

@Component
public class CommandQueue {
    private final SerialPort serialPort;
    private static Queue<String> queue;

    public CommandQueue(SerialPort serialPort) {
        this.serialPort = serialPort;
        queue = new ArrayDeque<>();
    }

    public void addCommand(String command) {
        queue.add(command);
    }

    @Scheduled(fixedDelay = 1)
    private void send() {
        if (!queue.isEmpty()) {

        }
    }

    // TODO: add a timeout somewhere here
    private int send(String command) {
        try {
            serialPort.getOutputStream().write(command.getBytes(StandardCharsets.UTF_8));
            serialPort.getOutputStream().flush();
            int ack = serialPort.getInputStream().read();
            if (ack != 0xC8) {
                printError();
            }
            return ack;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private void printError() {
        try {
            byte errorCode = (byte) serialPort.getInputStream().read();
            switch (errorCode) {
                case 0x00 -> System.out.println("ERROR: Command timed out");
                case 0x01 -> {
                    int expectedLength = serialPort.getInputStream().read();
                    int actualLength = serialPort.getInputStream().read();
                    System.out.println("ERROR: Unexpected command size, received length: " + actualLength
                            + "expected length: " + expectedLength);
                }
                case 0x02 -> System.out.println("ERROR: Missing command terminator");
                case 0x03 -> {
                    int code = serialPort.getInputStream().read();
                    System.out.println("ERROR: Unexpected command opcode, received: " + code);
                }
                case 0x04 -> System.out.println("ERROR: Incorrect number of arguments");
                case (byte) 0xFF -> System.out.println("ERROR: An unexpected error occurred");
                default -> System.out.println("ERROR: Unexpected error message signature received");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
