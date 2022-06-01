import app.serial.BytecodeConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestBytecodeConverter {

    @BeforeEach
    public void setup() {
    }

    @Test
    public void shouldParseNoParams() {
        String command = "clear()";
        byte[] expected = {0x04, 0x00, 0x00, (byte) 0xFE};
        byte[] result = BytecodeConverter.convertToBytes(command);
        //System.out.println("TEST1: " + Arrays.toString(expected));
        //System.out.println("TEST2: " + Arrays.toString(result));
        assertThat(result, is(expected));
    }

    @Test
    public void shouldParseParams() {
        String command = "setLED(120, 50, 150, 0)";
        byte[] expected = {0x0C, 0x01, 0x01, 0x78, (byte) 0xFD, 0x32, (byte) 0xFD, (byte) 0x96, (byte) 0xFD, 0x00, (byte) 0xFD, (byte) 0xFE};
        byte[] result = BytecodeConverter.convertToBytes(command);
        //System.out.println("Expected: " + Arrays.toString(expected));
        //System.out.println("Received: " + Arrays.toString(result));
        assertThat(result, is(expected));
    }

    @Test
    public void shouldHandleIntegers() {
        String command = "setLED(2500, 50, 150, 0)";
        byte[] expected = {0x0F, 0x01, 0x01, 0x00, 0x00, 0x09, (byte) 0xC4, (byte) 0xFD, 0x32, (byte) 0xFD, (byte) 0x96, (byte) 0xFD, 0x00, (byte) 0xFD, (byte) 0xFE};
        byte[] result = BytecodeConverter.convertToBytes(command);
        //System.out.println("Expected: " + Arrays.toString(expected));
        //System.out.println("Received: " + Arrays.toString(result));
        assertThat(result, is(expected));
    }

    @Test
    public void shouldClumpWhenPossible() {
        String command = "setLED(1, 2, 3, 4);setLED(2, 3, 4, 5)";
        byte[] expected = {0x0C, 0x01, 0x01, 0x01, (byte) 0xFD, 0x02, (byte) 0xFD, 0x03, (byte) 0xFD, 0x04, (byte) 0xFD, (byte) 0xFC,
                           0x0C, 0x01, 0x01, 0x02, (byte) 0xFD, 0x03, (byte) 0xFD, 0x04, (byte) 0xFD, 0x05, (byte) 0xFD, (byte) 0xFE};
        byte[] result = BytecodeConverter.convertToBytes(command);
        //System.out.println("Expected: " + Arrays.toString(expected));
        //System.out.println("Received: " + Arrays.toString(result));
        assertThat(result, is(expected));
    }
}
