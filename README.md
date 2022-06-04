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
* 0xFE: Command terminator - require acknowledgement
* 0xFD: Parameter terminator
* 0xFC: Command terminator - do not require acknowledgement
* 0xC8: ACK
* 0x00: Command with no parameters
* 0x01: Command with parameters


Each command has the following codes:
* 0x00: Clear
* 0x01: Set LED
* 0x02: Set all LEDs
* 0x03: Delay

## Opening a connection ##
When opening a new connection, it must be verified with a handshake to ensure both the sender and receiver have a clean buffer to work with, this is done by the sender first sending 0xAA, afterwards the receiver must respond with 0xBB, 0xCC, 0xDD in order.

## Error codes ##
Upon receiving an unparsable command, the client will flush the incomming serial buffer and return 0xFF along with an error code and potentially more information corresponding to:
* 0x00: Timeout
* 0x01: Unexpected command length - All bytes in the command could not be read
  * Returns: (Expected length of the command, Actual length of the command)
* 0x02: Missing command terminator
* 0x03: Unrecognized command name
  * Returns: Code of received command
* 0x04: Incorrect number of arguments
* 0xFF: Unexpected error

## Message structure ##
Each message sent will follow the following structure:
- 1 byte containing the size of the entire message (including this byte)
- 1 byte that is either 0x00 or 0x01 to show if a command has parameters or not
- 1 byte corresponding to the name of the command
- If the command has parameters, repeat this for each parameter:
  - 1 or 4 bytes with the value (a single byte is used for values <256, otherwise 4 bytes are used regardless of size (max 2^32 - 1)
  - 1 byte (0xFD) to show the end of the parameter
- 1 byte to show the end of the command, if this byte is 0xFE the receiver must acknowledge with 0xC8, if the command ends with 0xFC the sender does not expect an acknowledgement, which happens when the sender wishes to execute multiple commands that fit within the serial buffer
