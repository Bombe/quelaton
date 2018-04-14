package net.pterodactylus.fcp.fake

import org.hamcrest.*
import java.io.*
import java.net.*

/**
 * Wrapper around a [Socket] that handles text.
 */
class TextSocket(private val socket: Socket) : Closeable {

	private val socketInput = socket.getInputStream()!!
	private val socketOutput = socket.getOutputStream()!!
	private val inputReader = socketInput.bufferedReader()
	private val outputWriter = socketOutput.writer()

	fun readLine(): String? =
			inputReader.readLine()

	fun writeLine(line: String) {
		outputWriter.write("$line\n")
		outputWriter.flush()
	}

	fun collectUntil(lineMatcher: Matcher<String>): List<String>? =
			mutableListOf<String>().also { lines ->
				do {
					val line = readLine() ?: return null
					lines += line
				} while (!lineMatcher.matches(line))
			}

	override fun close() {
		outputWriter.close()
		inputReader.close()
		socketOutput.close()
		socketInput.close()
		socket.close()
	}

}
