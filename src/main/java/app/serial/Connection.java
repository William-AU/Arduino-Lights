package app.serial;

import app.common.SerialConstants;
import com.fazecast.jSerialComm.SerialPort;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Connection {
    private final SerialPort serialPort;
    private final CommandQueue commandQueue;
    public Connection(SerialPort serialPort) {
        this.serialPort = serialPort;
        this.commandQueue = new CommandQueue(serialPort);
        boolean handShakeFound = false;
        byte first = (byte) 0xFF;
        byte second = (byte) 0xFF;
        boolean inSequence = false;

        while (serialPort.bytesAvailable() > 0) {
            try {
                // Empty the initial buffer
                System.out.println(serialPort.getInputStream().read());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            System.out.println("Initiating handshake");
            this.serialPort.getOutputStream().write((byte) 0xAA);
            this.serialPort.getOutputStream().flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (!handShakeFound) {
            try {
                byte received = (byte) this.serialPort.getInputStream().read();
                System.out.println(received);
                if (received == (byte) 0xBB) {
                    System.out.println("Received first byte");
                    first = (byte) 0xBB;
                    second = (byte) 0xFF;
                    inSequence = true;
                }
                else if (received == (byte) 0xCC && inSequence) {
                    System.out.println("Received second byte");
                    second = (byte) 0xCC;
                }
                else if (received == (byte) 0xDD && inSequence) {
                    System.out.println("Received third byte, handshake finished");
                    handShakeFound = true;
                }
                else if (inSequence){
                    System.out.println("Sequence broken!");
                    inSequence = false;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String formatCommand(String name, Object... args) {
        StringBuilder res = new StringBuilder();
        res.append(name);
        res.append('(');
        String prefix = "";
        for (int i = 0; i < args.length; i++) {
            res.append(prefix).append(args[i]);
            prefix = ",";
        }
        res.append(')');
        return res.toString();
    }

    public void setLED(int LEDNumber, int r, int g, int b) {
        if (LEDNumber > SerialConstants.MAX_LED) return;
        String command = formatCommand("setLED", LEDNumber, r, g, b);
        send(command);
    }

    public void setAll(int r, int g, int b) {
        String command = formatCommand("setALL", r, g, b);
        send(command);
    }

    public void clear() {
        String command = formatCommand("clear");
        send(command);
    }

    public void setLEDNoClump(int LEDNumber, int r, int g, int b) {
        if (LEDNumber > SerialConstants.MAX_LED) return;
        String command = formatCommand("setLED", LEDNumber, r, g, b);
        send(command);
    }

    public void delay(int millis) {
        String command = formatCommand("delay", millis);
        send(command);
    }


    private void send(String command) {
        commandQueue.addCommand(command);
    }

    public void debugSendByte(byte toSend) {
        try {
            System.out.println("SENDING BYTE: " + toSend);
            serialPort.getOutputStream().write(toSend);
            byte received = (byte) serialPort.getInputStream().read();
            System.out.println("RECEIVED BYTE: " + received);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
