# Arduino-Lights

A simple API for controlling NeoPixel-like LEDs from Java.

To use, define the port and LED count in the constants (WIP), and flash the .ino file in main/resources. To ensure commands do not flood the serial buffer, 
all commands sent must be acknowledged by the Arduino, this results in a 1-10ms delay but ensures no commands are missed. 
By default the program uses a 57600 bit/s connection, this can be changed, but currently requires editing both the .ino file and main program. 

## Current list of commands: ##
* setLED
  * Sets the RGB value of a specific LED
* setALL
  * Sets the RGB value of all LEDs
* clear
  * Sets the RGB value of all LEDs to (0, 0, 0)
