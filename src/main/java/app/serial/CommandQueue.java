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
    private static CommandBundle commandBundle;

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
            String command = queue.peek();
            CommandBundle bundle = new CommandBundle();
            if (command.startsWith("CLUMP")) {
                bundle.addCommand(queue.remove());
                boolean moreToClump = queue.peek() != null && queue.peek().startsWith("CLUMP");
                while (moreToClump) {
                    bundle.addCommand(queue.remove());
                    moreToClump = queue.peek() != null && queue.peek().startsWith("CLUMP");
                }
            }
            if (bundle.isEmpty()) {
                int ack = send(command);
                if (ack != 200) {
                    System.out.println("INCORRECT ACKNOWLEDGEMENT, RECEIVED " + ack + " RESENDING COMMAND");
                    return;
                }
                queue.remove();
            } else {
                while (!bundle.isEmpty()) {
                    int ack = send(bundle.getNextCommandClump());
                    if (ack == 200) {
                        bundle.remove();
                    } else {
                        System.out.println("INCORRECT ACKNOWLEDGEMENT, RECEIVED " + ack + " RESENDING COMMAND");
                    }
                }
            }
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

    private class CommandBundle {
        private final Queue<String> clumps;

        private CommandBundle() {
            this.clumps = new ArrayDeque<>();
        }

        protected void addCommand(String command) {
            if (clumps.peek() == null) {
                clumps.add(command + ';');
            } else {
                String prevClump = clumps.peek();
                int prevSize = prevClump.getBytes().length;
                int commandSize = command.getBytes().length;
                // We undershoot the buffersize by 20 to account for inconsistencies and acknowledgements and stuff
                if (prevSize + commandSize + 1 > SerialConstants.BUFFER_SIZE - 20) {
                    queue.add(queue.remove() + command + ';');
                } else {
                    queue.add(command + ';');
                }
            }
        }

        protected boolean isEmpty() {
            return queue.isEmpty();
        }

        protected void remove() {
            queue.remove();
        }

        protected String getNextCommandClump() {
            if (isEmpty()) throw new IllegalArgumentException("Tried to access command but none exist");
            return queue.peek();
        }


    }
}
