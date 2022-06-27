package app.serial;

import app.common.SerialConstants;
import com.fazecast.jSerialComm.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CommandScheduler {
    private final SerialPort serialPort;
    private static Queue<String> queue;
    private final Logger logger;

    public CommandScheduler(SerialPort serialPort) {
        this.serialPort = serialPort;
        queue = new ArrayDeque<>();
        logger = LoggerFactory.getLogger(CommandScheduler.class);
    }

    public void addCommand(String command) {
        queue.add(command);
    }

    @Scheduled(fixedDelay = 1)
    private void send() {
        if (!queue.isEmpty()) {
            StringBuilder command = new StringBuilder(queue.remove());
            if (command.toString().toLowerCase().contains("setled") && !queue.isEmpty() && queue.peek().toLowerCase().contains("setled")) {
                while (queue.peek() != null && queue.peek().toLowerCase().contains("setled")) {
                    command.append(";").append(queue.remove());
                }
                for (byte[] bytes : clumpCommands(command.toString())) {
                    int ack = send(bytes);
                    if (ack != 0xC8) break;
                }
            } else {
                send(BytecodeConverter.convertToBytes(command.toString()));
            }
        }
    }

    // TODO: add a timeout somewhere here
    private int send(byte[] command) {
        logger.debug("SENDING: " + Arrays.toString(command) + " size: " + command.length);
        try {
            serialPort.getOutputStream().write(command);
            serialPort.getOutputStream().flush();
            //listen();   // THREAD TERMINATING DEBUG ONLY
            int ack = serialPort.getInputStream().read();
            //int ack = 200;
            if (ack != 0xC8) {
                printError();
            }
            logger.debug("Commmand acknowledged");
            return ack;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private void listen() {
        logger.debug("Started listening");
        try {
            while (true) {
                logger.debug("R: " + serialPort.getInputStream().read());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // This method is pretty dumb and slow, but this will basically never be the bottleneck, so it doesn't actually matter
    private List<byte[]> clumpCommands(String command) {
        // The idea here is to create chunks of commands that are as close to the serial buffer size as possible
        String[] commandArr = command.split(";");
        StringBuilder currentGuess = new StringBuilder();
        List<byte[]> res = new ArrayList<>();
        for (int i = 0; i < commandArr.length; i++) {
            int newLength;
            if (currentGuess.isEmpty()) {
                newLength = BytecodeConverter.convertToBytes(commandArr[i]).length;
            } else {
                newLength = BytecodeConverter.convertToBytes(currentGuess + ";" + commandArr[i]).length;
            }
            // Safety range of 10
            if (newLength > (SerialConstants.BUFFER_SIZE - 10)) {
                res.add(BytecodeConverter.convertToBytes(currentGuess.toString()));
                i--;
                currentGuess = new StringBuilder();
            } else {
                if (currentGuess.isEmpty()) {
                    currentGuess.append(commandArr[i]);
                } else {
                    currentGuess.append(";").append(commandArr[i]);
                }
            }
        }
        res.add(BytecodeConverter.convertToBytes(currentGuess.toString()));
        return res;
    }

    private void printError() {
        try {
            byte errorCode = (byte) serialPort.getInputStream().read();
            switch (errorCode) {
                case 0x00 -> logger.error("Command timed out");
                case 0x01 -> {
                    int expectedLength = serialPort.getInputStream().read();
                    int actualLength = serialPort.getInputStream().read();
                    logger.error("Unexpected command size, received length: " + actualLength
                            + " expected length: " + expectedLength);
                }
                case 0x02 -> logger.error("Missing command terminator (" + (byte) 0xFE + ")");
                case 0x03 -> {
                    int code = serialPort.getInputStream().read();
                    logger.error("Unexpected command opcode, received: " + code);
                }
                case 0x04 -> logger.error("Incorrect number of arguments");
                case (byte) 0xFF -> logger.error("An unexpected error occurred");
                default -> logger.error("Unexpected error message signature received");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
