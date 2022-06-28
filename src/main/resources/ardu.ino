#include <SSVQueueStackArray.h>

// https://github.com/oogre/SSVQueueStackArray
//#include <SSVQueueStackArray .h>
#include <Adafruit_NeoPixel.h>
#include <iostream>
#ifdef __AVR__
#include <avr/power.h> // Required for 16 MHz Adafruit Trinket
#endif

// Which pin on the Arduino is connected to the NeoPixels?
#define PIN        2 // On Trinket or Gemma, suggest changing this to 1

// How many NeoPixels are attached to the Arduino?
#define NUMPIXELS 93 // Popular NeoPixel ring size

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
  pixels.clear();

}

void loop() {

    if (Serial.available()) {
        byte buffer[1] = {};
        Serial.readBytes(buffer, 1);
        if (buffer[0] == 0xAA) {
          byte toWrite[3] = {0xBB, 0xCC, 0xDD};
          Serial.write(toWrite, 3);
        } else {
          int noOfBytes = int(buffer[0]);
          readCommand(noOfBytes);
        }

    //allWHITE();
    pixels.show();
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

    if (buffer[sizeof(buffer) - 1] != 0xFE && buffer[sizeof(buffer) - 1] != 0xFC) {
        byte error[2] = {0xFF, 0x02};
        flushSerial();
        Serial.write(error, 2);
        return;
    }

    bool hasParams = buffer[0] != 0; // Dirty trick to read 0x00 as false and everything else (but in reality only 0x01) as true
    bool mustAcknowledge = false;
    if (buffer[sizeof(buffer) - 1] == 0xFE) {
        mustAcknowledge = true;
    }
    SSVQueueStackArray  <byte> reversedStack;
    SSVQueueStackArray <byte> commandStack;
    SSVQueueStackArray <byte> debugStack;
    for (int i = 0; i < sizeof(buffer); i++) {
      reversedStack.push(buffer[i]);
    }
    for (int i = 0; i < sizeof(buffer); i++) {
      byte toPush = reversedStack.pop();
      commandStack.push(toPush);
      debugStack.push(toPush);
    }
    commandStack.pop(); // This will always be the param byte, so is safely ignored
    debugStack.pop();
    byte debugMessage[sizeof(buffer) + 2] = {};
    debugMessage[0] = sizeof(commandStack);
    debugMessage[sizeof(buffer) - 1];
    for (int i = 0; i < sizeof(buffer) - 1; i++) {
      debugMessage[i] = debugStack.pop();
    }
    //Serial.write(debugMessage, sizeof(buffer) - 1);
    // Next pop is the command ID
    switch (commandStack.pop()) {
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

void delay(SSVQueueStackArray  <byte> commandStack) {
    int millis = extractIntFromStackAndVerify(commandStack);
    if (millis == -1) return;
    if (commandStack.pop() != 0xFE && commandStack.pop() != 0xFC) {
        wrongNumberOfArgsError();
        return;
    }
    delay(millis);
}

void flushSerial() {
    while (Serial.available()) {
        char c = Serial.read();
    }
}


void setLED(SSVQueueStackArray <byte>& commandStack) {
    int LEDNumber = extractIntFromStackAndVerify(commandStack);
    int r = extractIntFromStackAndVerify(commandStack);
    int g = extractIntFromStackAndVerify(commandStack);
    int b = extractIntFromStackAndVerify(commandStack);

    // Error handling
    if (LEDNumber == -1 || r == -1 || g == -1 || b == -1) return;
    byte peek = commandStack.pop();
    commandStack.push(peek);
    if (peek != 0xFE && peek != 0xFC) {
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

int extractIntFromStackAndVerify(SSVQueueStackArray <byte>& commandStack) {
    byte firstByte = commandStack.pop();
    int value = 0;
    byte peek = commandStack.pop();
    //commandStack.push(peek);

    if (peek != 0xFD) {
      byte number[4] = {};
      number[0] = firstByte;
      number[1] = peek;
      number[2] = commandStack.pop();
      number[3] = commandStack.pop();
      peek = commandStack.pop();
      for (int i = 0; i < 4; i++) {
      value = (value << 8) + (number[0] & 0xFF);
      }
    } else {
      value = (int) firstByte;
    }
    byte terminator = peek;
    byte debugMessage[5] = {69, firstByte, peek, terminator, 69};
    //Serial.write(debugMessage, 5);
    if (terminator != 0xFD) {
      wrongNumberOfArgsError();
      return -1;
    }
    return value;
}


void clear() {
  pixels.clear();
}

void allRED() {
  for (int i = 0; i < NUMPIXELS; i++) {
    pixels.setPixelColor(i, 150, 0, 0);
  }
  pixels.show();
}

void allGREEN() {
  for (int i = 0; i < NUMPIXELS; i++) {
    pixels.setPixelColor(i, 0, 150, 0);
  }
  pixels.show();
}

void allBLUE() {
  for (int i = 0; i < NUMPIXELS; i++) {
    pixels.setPixelColor(i, 0, 0, 150);
  }
  pixels.show();
}

void allWHITE() {
  for (int i = 0; i < NUMPIXELS; i++) {
    pixels.setPixelColor(i, 10, 10, 10);
  }
  pixels.show();
}

byte popTest(SSVQueueStackArray <byte>& commandStack) {
  return commandStack.pop();
}

void setALL(SSVQueueStackArray <byte>& commandStack) {

    int r = extractIntFromStackAndVerify(commandStack);
    int g = extractIntFromStackAndVerify(commandStack);
    int b = extractIntFromStackAndVerify(commandStack);

    if (r == -1  || g == -1 || b == -1) return;
    byte pop = commandStack.pop();
    if (pop != 0xFE && pop != 0xFC) {
        wrongNumberOfArgsError();
        return;
    }

    // TODO: This should not be NUMPIXELS but instead get it as a parameter in acknowledgement
    for (int i = 0; i < NUMPIXELS; i++) {
        pixels.setPixelColor(i, r, g, b);
        pixels.show();
    }
}

void ACK() {
  Serial.write(0xC8);
}