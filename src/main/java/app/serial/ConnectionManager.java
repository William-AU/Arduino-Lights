package app.serial;

import com.fazecast.jSerialComm.SerialPort;
import org.springframework.stereotype.Service;
import app.common.SerialConstants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ConnectionManager {
    private static final List<ALConnection> connections = new ArrayList<>();


    public static void addConnection(ALConnection connection) {
        connections.add(connection);
    }

    public static void removeConnection(ALConnection connection) {
        connections.remove(connection);
        connection.getSerialPort().closePort();
    }

    public static int getNumberOfConnections() {
        return connections.size();
    }
}
