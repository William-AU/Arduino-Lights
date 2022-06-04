package app;

import app.common.SerialConstants;
import app.serial.Connection;
import app.serial.ConnectionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class Application implements CommandLineRunner {
    private final ConnectionManager connectionManager;

    @Autowired
    public Application(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    /**
     * Callback used to run the bean.
     *
     * @param args incoming main method arguments
     * @throws Exception on error
     */
    @Override
    public void run(String... args) throws Exception {
        Connection conn = connectionManager.getConnection();
        //conn.clear();
        //conn.setAll(12, 80, 120);
        //conn.setAll(0, 150, 0);

        for (int i = 0; i < SerialConstants.MAX_LED + 1; i++) {
            conn.setLED(i, 0, 0, 255);
        }

        //conn.setLED(0, 0, 150, 0);


    }
}
