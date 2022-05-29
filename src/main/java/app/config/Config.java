package app.config;

import app.serial.ConnectionManager;
import com.fazecast.jSerialComm.SerialPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {
    @Bean
    public SerialPort serialPort(ConnectionManager connectionManager) {
        return connectionManager.getPort();
    }
}
