// https://github.com/oogre/StackArray
#include <StackArray.h>
#include <Adafruit_NeoPixel.h>
#ifdef __AVR__
#include <avr/power.h> // Required for 16 MHz Adafruit Trinket
#endif

// Which pin on the Arduino is connected to the NeoPixels?
#define PIN        4 // On Trinket or Gemma, suggest changing this to 1

// How many NeoPixels are attached to the Arduino?
#define NUMPIXELS 138 // Popular NeoPixel ring size

// When setting up the NeoPixel library, we tell it how many pixels,
// and which pin to use to send signals. Note that for older NeoPixel
// strips you might need to change the third parameter -- see the
// strandtest example for more information on possible values.
Adafruit_NeoPixel pixels(NUMPIXELS, PIN, NEO_GRB + NEO_KHZ800);

void setup() {
  Serial.begin(57600);
  while (!Serial) {
    ; // Wait for serial connection
  }
  // These lines are specifically to support the Adafruit Trinket 5V 16 MHz.
  // Any other board, you can remove this part (but no harm leaving it):
#if defined(__AVR_ATtiny85__) && (F_CPU == 16000000)
  clock_prescale_set(clock_div_1);
#endif
  // END of Trinket-specific code.

  pixels.begin(); // INITIALIZE NeoPixel strip object (REQUIRED)
}

void loop() {
    if (Serial.available()) {
        byte buffer[1] = {};
        Serial.readBytes(buffer, 1);
        int noOfBytes = int(buffer[0]);
        readCommand(noOfBytes);
    }
}

void readCommand(int length) {
    byte buffer[length - 1] = {};
    int received = Serial.readBytes(buffer, length - 1);
    if (received != length - 1) {
        byte error[4] = {0xFF, 0x00, length, received + 1};
        flushSerial();
        Serial.write(error, 4);
        return;
    }

    if (buffer[buffer.length - 1] != 0xFE && buffer[length - 1] != 0xFC) {
        byte error[2] = {0xFF, 0x02};
        flushSerial();
        Serial.write(error, 2);
        return;
    }

    bool hasParams = buffer[0] != 0; // Dirty trick to read 0x00 as false and everything else (but in reality only 0x01) as true
    bool mustAcknowledge = false;
    if (buffer[buffer.length - 1] == 254) {
        mustAcknowledge = true;
    }
    StackArray <byte> commandStack;
    for (int i = buffer.length - 1; i > 2; i--) {    // A bit scuffed and should probably be a queue
        commandStack.push(buffer[i]);
    }

    switch (buffer[2]) {
        case 0x00:
            // Clear
            clear();
            break;
        case 0x01:
            // Set LED
            setLED(commandStack);
            break;
        case 0x02:
            // Set ALL LEDs
            setALL(commandStack);
            break;
        case 0x03:
            // Delay
            delay(commandStack);
            break;
        default:
            byte error[3] = {0xFF, 0x03, buffer[2]};
            flushSerial();
            Serial.write(error, 3);

    }

    if (mustAcknowledge) {
        ACK();
    }
}

void delay(StackArray commandStack) {
    int millis = extractIntFromStackAndVerify(commandStack);
    if (millis == -1) return;
    if (commandStack.pop() != 0xFE && commandStack.pop() != 0xFC) {
        wrongNumberOfArgsError();
        return;
    }
    delay(millis);
}

void flushSerial() {
    while (Serial.available() {
        char c = Serial.read();
    }
}


void setLED(StackArray commandStack) {
    int LEDNumber = extractIntFromStackAndVerify(commandStack);
    int r = extractIntFromStackAndVerify(commandStack);
    int g = extractIntFromStackAndVerify(commandStack);
    int b = extractIntFromStackAndVerify(commandStack);

    // Error handling
    if (noOfLEDs == -1 || r == -1 || g == -1 || b == -1) return;
    if (commandStack.peek() != 0xFE && commandStack.peek() != 0xFC) {
        wrongNumberOfArgsError();
        return;
    }

    pixels.setPixelColor(LEDNumber, r, g, b);
}

void wrongNumberOfArgsError() {
    byte error[2] = {0xFF, 0x04};
    flushSerial();
    Serial.write(error, 2);
}

int extractIntFromStackAndVerify(StackArray stack) {
    byte firstByte = commandStack.pop();
        int value = 0;
        if (commandStack.peek() != 0xFD) {
            byte number[4] = {};
            number[0] = firstByte;
            number[1] = commandStack.pop();
            number[2] = commandStack.pop();
            number[3] = commandStack.pop();
            for (int i = 0; i < 4; i++) {
                value = (value << 8) + (number[0] & 0xFF);
            }
        } else {
            value = (int) firstByte;
        }
    byte terminator = stack.pop();
    if (terminator != 0xFD) {
        wrongNumberOfArgsError();
        return -1;
    }
    return value;
}


void clear() {
  pixels.clear();
}

void setALL(StackArray commandStack) {
    int r = extractIntFromStackAndVerify(commandStack);
    int g = extractIntFromStackAndVerify(commandStack);
    int b = extractIntFromStackAndVerify(commandStack);

    if (r == -1  || g == -1 || b == -1) return;
    if (stack.pop() != 0xFD && stack.pop() != 0xFC) {
        wrongNumberOfArgsError();
        return;
    }

    // TODO: This should not be NUMPIXELS but instead get it as a parameter
    for (int i = 0; i < NUMPIXELS; i++) {
        pixels.setPixelColor(i, r, g, b);
    }
}

void ACK() {
  Serial.write(0xC8);
}