import app.ALBuilder;

import java.io.IOException;

public class FullTest {
    public static void main(String[] args) {
        ALBuilder builder = new ALBuilder();
        builder.setPort("COM5");
        builder.setNoOfLEDs(137);
        try {
            builder.build();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
