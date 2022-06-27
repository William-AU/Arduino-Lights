import app.ALBuilder;
import app.serial.ALConnection;

import java.io.IOException;

public class FullTest {
    public static void main(String[] args) {
        ALBuilder builder = new ALBuilder();
        builder.setPort("COM5");
        builder.setNoOfLEDs(137);
        try {
            ALConnection connection = builder.build();
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
