package app.serial;

import app.common.SerialConstants;
import com.fazecast.jSerialComm.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class ALConnection {
    private final SerialPort serialPort;
    private final CommandScheduler commandScheduler;
    private final Logger logger = LoggerFactory.getLogger(ALConnection.class);
    public ALConnection(SerialPort serialPort) {
        logger.info("Connection established");
        this.serialPort = serialPort;
        this.commandScheduler = new CommandScheduler(serialPort);
        boolean handShakeFound = false;
        byte first = (byte) 0xFF;
        byte second = (byte) 0xFF;
        boolean inSequence = false;

        try {
            // For some reason upon first start, the serial buss sends a bunch of garbage data, we need to remove this,
            // and ensure the serial bus is clean to initiate the handshake
            logger.debug("Flushing serial");
            this.serialPort.getOutputStream().write((byte) 0xAA);
            this.serialPort.getOutputStream().flush();
            // This call is needed because of how java reads from serial, it will not put any data into the stream
            // buffer before at least one byte has been read, therefore we need to first write a byte,
            // which will prompt the arduino to write *something* so that we can get a more accurate read
            this.serialPort.getInputStream().read();
            logger.debug("Serial bytes available: " + this.serialPort.getInputStream().available());
            while (this.serialPort.getInputStream().available() != 0) {
                this.serialPort.getInputStream().read();
            }

            logger.info("Initiating handshake");
            this.serialPort.getOutputStream().write((byte) 0xAA);
            this.serialPort.getOutputStream().flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (!handShakeFound) {
            try {
                byte received = (byte) this.serialPort.getInputStream().read();
                logger.debug("Received byte [" + received + "]");
                if (received == (byte) 0xBB) {
                    logger.debug("Received first byte");
                    first = (byte) 0xBB;
                    second = (byte) 0xFF;
                    inSequence = true;
                }
                else if (received == (byte) 0xCC && inSequence) {
                    logger.debug("Received second byte");
                    second = (byte) 0xCC;
                }
                else if (received == (byte) 0xDD && inSequence) {
                    logger.debug("Received third byte, handshake finished");
                    handShakeFound = true;
                }
                else if (inSequence){
                    logger.warn("Handshake sequence broken");
                    inSequence = false;
                }
            } catch (IOException e) {
                logger.error("Unable to finish handshake");
                e.printStackTrace();
            }
        }
    }

    public SerialPort getSerialPort() {
        return serialPort;
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
        commandScheduler.addCommand(command);
    }

    public void debugSendByte(byte toSend) {
        try {
            logger.debug("SENDING BYTE: " + toSend);
            serialPort.getOutputStream().write(toSend);
            byte received = (byte) serialPort.getInputStream().read();
            logger.debug("RECEIVED BYTE: " + received);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Waits until any current commands have been sent and acknowledged, afterwards closes the connection
     * @param timeoutSeconds Number of seconds to wait before TimeoutException is thrown
     * @throws TimeoutException If the commandqueue can not be emptied in the given time
     */
    public void finishAndClose(int timeoutSeconds) throws TimeoutException {
        //TODO: Fix busy waiting, it shouldn't be a huge problem since this will likely only run a few seconds at most, but still bad practice
        int timer = 0;
        while (!commandScheduler.queueEmpty()) {
            if (timer > timeoutSeconds) throw new TimeoutException("Failed to close connection, could not empty command queue");
            timer++;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.error("Unable to sleep in thread", e);
            }
        }
        commandScheduler.close();
        ConnectionManager.removeConnection(this);
    }

    /**
     * Forcefully ends any ongoing connection
     */
    public void stopAndClose() {
        commandScheduler.close();
        ConnectionManager.removeConnection(this);
    }
}
