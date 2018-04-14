package net.pterodactylus.fcp.fake

import org.hamcrest.*
import java.io.*
import java.net.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*

/*
 * A fake TCP server.
 */
class FakeTcpServer(private val executorService: ExecutorService) : Closeable {

	private val serverSocket = ServerSocket(0)
	val port get() = serverSocket.localPort
	private val clientSocket = AtomicReference<TextSocket>()

	fun connect(): Future<*> =
			executorService.submit {
				clientSocket.set(TextSocket(serverSocket.accept()))
			}

	fun collectUntil(lineMatcher: Matcher<String>): List<String> =
			clientSocket.get().collectUntil(lineMatcher)

	fun writeLine(vararg lines: String) =
			lines.forEach(clientSocket.get()::writeLine)

	fun readLine(): String? =
			clientSocket.get().readLine()

	override fun close(): Unit =
			clientSocket.get()?.close() ?: Unit

}
