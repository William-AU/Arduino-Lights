package app.serial;

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

    @Scheduled(fixedDelay = 10)
    private void send() {
        addCommand("test");
        if (!queue.isEmpty()) {
            //System.out.println("EXECUTING COMMAND IN QUEUE");
            String command = queue.peek();
            try {
                //System.out.println("SENDING COMMAND");
                serialPort.getOutputStream().write(command.getBytes(StandardCharsets.UTF_8));
                serialPort.getOutputStream().flush();
                //System.out.println("AWAITING ACKNOWLEDGEMENT");
                int ack = serialPort.getInputStream().read();
                if (ack != 200) {
                    System.out.println("WRONG ACKNOWLEDGEMENT, RECEIVED: " + ack + " RESENDING COMMAND");
                    return;
                }


                queue.remove();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //System.out.println("COMMAND QUEUE IS EMPTY WITH LENGTH: " + queue.size());
    }
}
