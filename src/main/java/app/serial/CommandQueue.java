package app.serial;

import app.common.SerialConstants;
import com.fazecast.jSerialComm.SerialPort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

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
            StringBuilder command = new StringBuilder(queue.remove());
            if (command.toString().toLowerCase().contains("setled")) {
                while (queue.peek() != null && queue.peek().toLowerCase().contains("setled")) {
                    command.append(";").append(queue.remove());
                }
            }
            for (byte[] bytes : clumpCommands(command.toString())) {
                int ack = send(bytes);
                if (ack != 0xC8) break;
            }
        }
    }

    // TODO: add a timeout somewhere here
    private int send(byte[] command) {
        try {
            serialPort.getOutputStream().write(command);
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

    // This method is pretty dumb and slow, but this will basically never be the bottleneck, so it doesn't actually matter
    private List<byte[]> clumpCommands(String command) {
        // The idea here is to create chunks of commands that are as close to the serial buffer size as possible
        String[] commandArr = command.split(";");
        StringBuilder currentGuess = new StringBuilder();
        List<byte[]> res = new ArrayList<>();
        int currentLength = 0;
        for (int i = 0; i < commandArr.length; i++) {
            int newLength;
            if (currentGuess.isEmpty()) {
                newLength = currentLength + BytecodeConverter.convertToBytes(commandArr[i]).length;
            } else {
                newLength = currentLength + BytecodeConverter.convertToBytes(currentGuess + ";" + commandArr[i]).length;
            }
            // Safety range of 10
            if (newLength > (SerialConstants.BUFFER_SIZE - 10)) {
                res.add(BytecodeConverter.convertToBytes(currentGuess + ";" + commandArr[i]));
                currentLength = 0;
                currentGuess = new StringBuilder();
            } else {
                if (currentGuess.isEmpty()) {
                    currentGuess.append(commandArr[i]);
                } else {
                    currentGuess.append(";").append(commandArr[i]);
                }
                currentLength = newLength;
            }
        }
        return res;
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
