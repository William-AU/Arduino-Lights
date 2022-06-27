package app;

import app.common.SerialConstants;
import app.serial.ALConnection;
import app.serial.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Random;

@EnableScheduling
@SpringBootApplication
public class Application implements CommandLineRunner {
    private final ConnectionManager connectionManager;
    private static boolean isRunning = false;

    @Autowired
    public Application(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    protected static void main(String[] args) {
        if (!isRunning) {
            Logger logger = LoggerFactory.getLogger(Application.class);
            logger.info("ALLibrary loaded");
            SpringApplication.run(Application.class, args);
            isRunning = true;
        }
    }

    /**
     * Callback used to run the bean.
     *
     * @param args incoming main method arguments
     * @throws Exception on error
     */
    @Override
    public void run(String... args) throws Exception {
    }
}
