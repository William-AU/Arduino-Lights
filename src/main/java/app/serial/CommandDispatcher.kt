package app.serial

import app.common.SerialConstants
import com.fazecast.jSerialComm.SerialPort
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.*

class CommandDispatcher(emptyQueue: Queue<String>, private val serialPort: SerialPort) {
    private val commandQueue: Queue<String> = emptyQueue;
    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)
    private var alive: Boolean = true;
    private val dispatchThread: Thread = Thread {
        logger.debug("Dispatching thread started")
        while (alive) {
            // Not sure why this is needed, but apparently it is
            Thread.sleep(1)
            if (!commandQueue.isEmpty()) {
                val command: StringBuilder = StringBuilder(commandQueue.remove())
                if (command.toString().lowercase().contains("setled") && !commandQueue.isEmpty() && commandQueue.peek()
                        .lowercase().contains("setled")
                ) {
                    while (commandQueue.peek() != null && commandQueue.peek().lowercase().contains("setled")) {
                        command.append(";").append(commandQueue.remove())
                    }
                    for (bytes in clumpCommands(command.toString())!!) {
                        val ack: Int = send(bytes)
                        if (ack != 0xC8) break
                    }
                } else {
                    send(BytecodeConverter.convertToBytes(command.toString()))
                }
            }
        }
    }

    init {
        dispatchThread.start()
    }


    // This method is pretty dumb and slow, but this will basically never be the bottleneck, so it doesn't actually matter
    private fun clumpCommands(command: String): List<ByteArray>? {
        // The idea here is to create chunks of commands that are as close to the serial buffer size as possible
        val commandArr = command.split(";").toTypedArray()
        var currentGuess = StringBuilder()
        val res: MutableList<ByteArray> = ArrayList()
        var i = 0
        while (i < commandArr.size) {
            var newLength: Int
            newLength = if (currentGuess.isEmpty()) {
                BytecodeConverter.convertToBytes(commandArr[i]).size
            } else {
                BytecodeConverter.convertToBytes(currentGuess.toString() + ";" + commandArr[i]).size
            }
            // Safety range of 10
            if (newLength > SerialConstants.BUFFER_SIZE - 10) {
                res.add(BytecodeConverter.convertToBytes(currentGuess.toString()))
                i--
                currentGuess = StringBuilder()
            } else {
                if (currentGuess.isEmpty()) {
                    currentGuess.append(commandArr[i])
                } else {
                    currentGuess.append(";").append(commandArr[i])
                }
            }
            i++
        }
        res.add(BytecodeConverter.convertToBytes(currentGuess.toString()))
        return res
    }

    // TODO: add a timeout somewhere here
    private fun send(command: ByteArray): Int {
        logger.debug("SENDING: " + Arrays.toString(command) + " size: " + command.size)
        try {
            serialPort.getOutputStream().write(command)
            serialPort.getOutputStream().flush()
            //listen();   // THREAD TERMINATING DEBUG ONLY
            val ack: Int = serialPort.getInputStream().read()
            //int ack = 200;
            if (ack != 0xC8) {
                printError()
            }
            logger.debug("Commmand acknowledged")
            return ack
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return -1
    }

    private fun printError() {
        val errorCode: Int = serialPort.inputStream.read()
        when(errorCode) {
            0x00 -> logger.error("Command timed out")
            0x01 -> {
                val expectedLength = serialPort.inputStream.read()
                val actualLength = serialPort.inputStream.read()
                logger.error(
                    "Unexpected command size, received length: " + actualLength
                            + " expected length: " + expectedLength
                )
            }
            0x02 -> logger.error("Missing command terminator (" + 0xFE.toByte() + ")")
            0x03 -> {
                val code = serialPort.inputStream.read()
                logger.error("Unexpected command opcode, received: $code")
            }
            0x04 -> logger.error("Incorrect number of arguments")
            0xFF -> logger.error("An unexpected error occurred")
            else -> logger.error("Unexpected error message signature received")
        }
    }

    fun queueEmpty(): Boolean {
        return commandQueue.isEmpty()
    }

    fun dispatch(command: String) {
        commandQueue.add(command)
    }

    fun kill() {
        alive = false
    }

}