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
            return ack;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
