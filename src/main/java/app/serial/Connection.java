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
}
