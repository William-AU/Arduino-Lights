package app;

import app.common.SerialConstants;
import app.serial.ALConnection;
import com.fazecast.jSerialComm.SerialPort;

import java.io.IOException;

/**
 * Factory for establishing a serial connection, must specify the port and number of LEDs,
 */
public class ALBuilder {
    private ALConnection connection;
    private String port;
    private int noOfLEDs = -1;
    // Default settings
    private int baudRate = 57600;
    private int dataBits = 8;
    private int stopBits = 1;
    private int parityBits = 0;
    private int readTimeout = 0;
    private int writeTimeout = 0;



    public ALConnection build() throws IllegalArgumentException, IOException {
        Application.main(new String[]{});
        if (port == null) throw new IllegalArgumentException("Must specify the port");
        if (noOfLEDs == -1) throw new IllegalArgumentException("Must specify number of LEDs");
        SerialPort serialPort = SerialPort.getCommPort(port);

        serialPort.setComPortParameters(baudRate, dataBits, stopBits, parityBits);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, readTimeout, writeTimeout);
        if (!serialPort.openPort()) {
            throw new IOException("Could not open serial port, likely already in use");
        }

        return new ALConnection(serialPort);
    }


    public void setPort(String port) {
        this.port = port;
    }

    public void setNoOfLEDs(int noOfLEDs) {
        this.noOfLEDs = noOfLEDs;
    }

    public void setBaudRate(int baudRate) {
        this.baudRate = baudRate;
    }

    public void setDataBits(int dataBits) {
        this.dataBits = dataBits;
    }

    public void setStopBits(int stopBits) {
        this.stopBits = stopBits;
    }

    public void setParityBits(int parityBits) {
        this.parityBits = parityBits;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public void setWriteTimeout(int writeTimeout) {
        this.writeTimeout = writeTimeout;
    }
}