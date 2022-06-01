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

## Protocol ##
To preserve space and allow for command optimisation, all commands are converted to bytecode before being sent over serial. The bytecode opcodes are as follows:
* 0xFF: Error
* 0xFE: Command terminator
* 0xFD: Parameter terminator
* 0x00: Command with no parameters
* 0x01: Command with parameters


Each command has the following codes:
* 0x00: Clear
* 0x01: Set LED
* 0x02: Set all LEDs
* 0x03: Delay

Each command (0x00 and 0x01) must be followed by single byte command code, any parameters if applicable, and a terminator. For commands with parameters, the name is followed by a single byte denoting the number of parameters, then each parameter value can be any number of bytes representing the value, lastly each parameter must be terminated with parameter terminator byte. The full command must further be terminated by a command terminator.
