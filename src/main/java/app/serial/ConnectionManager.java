package app.serial;

import com.fazecast.jSerialComm.SerialPort;
import org.springframework.stereotype.Service;
import app.common.SerialConstants;

import java.io.IOException;

@Service
public class ConnectionManager {
    private final SerialPort serialPort;

    public ConnectionManager() throws IOException {
        serialPort = SerialPort.getCommPort(SerialConstants.PORT);

        serialPort.setComPortParameters(57600, 8, 1, 0);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0);
        if (!serialPort.openPort()) {
            throw new IOException("Could not open serial port, likely already in use");
        }
    }

    public SerialPort getPort() {
        return serialPort;
    }

    public ALConnection getConnection() {
        return new ALConnection(serialPort);
    }
}
