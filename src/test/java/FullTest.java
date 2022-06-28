import app.ALBuilder;
import app.serial.ALConnection;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class FullTest {
    public static void main(String[] args) {
        ALBuilder builder = new ALBuilder();
        builder.setPort("COM5");
        builder.setNoOfLEDs(137);
        try {
            ALConnection connection = builder.build();
            connection.setAll(50, 0, 0);
            connection.finishAndClose(5);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            System.out.println("WE TIMED OUT!");
        }
    }
}
