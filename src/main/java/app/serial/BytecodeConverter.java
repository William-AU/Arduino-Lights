package app.serial;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class BytecodeConverter {
    public static byte[] convertToBytes(String command) {
        String[] commands = command.split(";");
        List<byte[]> commandsByte = new ArrayList<>();
        for (int i = 0; i < commands.length; i++) {
            if (i == commands.length - 1) {
                // Last command
                commandsByte.add(convertSingleCommand(commands[i], true));
            } else {
                commandsByte.add(convertSingleCommand(commands[i], false));
            }
        }
        byte[] res = new byte[0];
        for (byte[] arr : commandsByte) {
            res = concatenate(res, arr);
        }
        return res;
    }

    private static byte[] convertSingleCommand(String command, boolean lastCommand) {
        String[] leftSplit = command.split("\\(");
        String commandName = leftSplit[0];
        byte hasParams = parseCommandOPType(commandName);
        String[] args;
        if (hasParams == (byte) 0x01) {
            String rawArgs = leftSplit[1].split("\\)")[0];
            args = rawArgs.split(",( )*");
        } else {
            args = new String[0];
        }
        byte nameOP = parseCommandName(commandName);

        List<byte[]> params = new ArrayList<>();
        int totalParamSize = 0;
        for (String arg : args) {
            byte[] byteParam = parseCommandParam(arg);
            params.add(byteParam);
            totalParamSize += byteParam.length;
        }
        byte commandTerminator;
        if (lastCommand) {
            commandTerminator = (byte) 0xFE;
        } else {
            commandTerminator = (byte) 0xFC;
        }
        byte[] res = new byte[totalParamSize + 4];  // (param) Size + (byte) size + param/noparam byte + name + command terminator
        res[0] = (byte) res.length;
        res[1] = hasParams;
        res[2] = nameOP;
        // Combine arrays of all params
        int index = 3;
        for (byte[] param : params) {
            for (int i = 0; i < param.length; i++) {
                res[index] = param[i];
                index++;
            }
        }
        res[res.length - 1] = commandTerminator;
        return res;
    }

    // Array concatenation magic stolen from SO:
    // https://stackoverflow.com/questions/80476/how-can-i-concatenate-two-arrays-in-java
    private static <T> T concatenate(T a, T b) {
        if (!a.getClass().isArray() || !b.getClass().isArray()) {
            throw new IllegalArgumentException();
        }

        Class<?> resCompType;
        Class<?> aCompType = a.getClass().getComponentType();
        Class<?> bCompType = b.getClass().getComponentType();

        if (aCompType.isAssignableFrom(bCompType)) {
            resCompType = aCompType;
        } else if (bCompType.isAssignableFrom(aCompType)) {
            resCompType = bCompType;
        } else {
            throw new IllegalArgumentException();
        }

        int aLen = Array.getLength(a);
        int bLen = Array.getLength(b);

        @SuppressWarnings("unchecked")
        T result = (T) Array.newInstance(resCompType, aLen + bLen);
        System.arraycopy(a, 0, result, 0, aLen);
        System.arraycopy(b, 0, result, aLen, bLen);
        return result;
    }

    private static byte[] parseCommandParam(String param) {
        int val = Integer.parseInt(param);
        byte[] res;
        if (val > 255) {
            res = new byte[Integer.BYTES + 1];  // If the size is too big, we must include extra bits to allow up to 2^32 - 1
            int length = Integer.BYTES;
            // Bit of byte magic to convert the int to a series of 4 bytes
            for (int i = 0; i < length; i++) {
                res[length - i - 1] = (byte) (val & 0xFF);;
                val >>= 8;
            }
        } else {
            res = new byte[2];
            res[0] = (byte) (val & 0xFF);
        }
        res[res.length - 1] = (byte) 0xFD;  // Param terminator
        return res;
    }

    private static byte parseCommandOPType(String commandName) {
        return switch (commandName) {
            case "clear" -> (byte) 0x00;
            default -> (byte) 0x01;
        };
    }

    private static byte parseCommandName(String commandName) {
        return switch (commandName.toLowerCase(Locale.ROOT)) {
            case "clear" -> (byte) 0x00;
            case "setled" -> (byte) 0x01;
            case "setall" -> (byte) 0x02;
            case "delay" -> (byte) 0x03;
            default -> (byte) 0xFF;
        };
    }
}
