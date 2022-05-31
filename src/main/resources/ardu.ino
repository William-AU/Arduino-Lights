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
  //pixels.clear();
  if (Serial.available() > 0) {
    String command = Serial.readStringUntil('(');
    // No switching on strings so we do this 
    if (command == "setLED") {
      setLED();
    } else if (command == "setALL") {
      setALL();
    } else if (command == "clear") {
      readLast(); // Clear buffer
      clear();
    }
    ACK();
    pixels.show();
  }
}

int readInt() {
  return Serial.readStringUntil(',').toInt();
}

int readLastInt() {
  return Serial.readStringUntil(')').toInt();
}

String readLast() {
  return Serial.readStringUntil(')');
}

void setLED() {
  int LEDNumber = readInt();
  int r = readInt();
  int g = readInt();
  int b = readLastInt();
  pixels.setPixelColor(LEDNumber, r, g, b);
}

void clear() {
  pixels.clear();
}


void setALL() {
  int r = readInt();
  int g = readInt();
  int b = readLastInt();
  for (int i = 0; i < NUMPIXELS; i++) {
    pixels.setPixelColor(i, pixels.Color(r, g, b));
  }
}

void ACK() {
  Serial.write(200);
}