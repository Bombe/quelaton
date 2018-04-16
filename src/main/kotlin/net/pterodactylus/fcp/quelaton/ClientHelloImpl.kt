package net.pterodactylus.fcp.quelaton

import com.google.common.util.concurrent.*
import net.pterodactylus.fcp.*
import net.pterodactylus.fcp.util.*
import java.io.*
import java.util.concurrent.*

/**
 * Internal `ClientHello` implementation based on [FcpDialog].
 *
 * @author [David ‘Bombe’ Roden](mailto:bombe@pterodactylus.net)
 */
internal class ClientHelloImpl(threadPool: ExecutorService, private val hostname: String, private val port: Int) {

	private val threadPool: ListeningExecutorService = MoreExecutors.listeningDecorator(threadPool)
	private var clientName by atomic<String?>(null)

	fun withName(name: String): Executable<FcpConnection> {
		clientName = name
		return Executable { this.execute() }
	}

	private fun execute(): ListenableFuture<FcpConnection> =
			threadPool.submit<FcpConnection>(this::establishConnection)

	private fun establishConnection(): FcpConnection {
		val connection = FcpConnection(hostname, port)
		connection.connect()
		val clientHello = ClientHello(clientName, "2.0")
		var exception: Exception? = null
		try {
			ClientHelloDialog(connection).use { clientHelloDialog ->
				if (clientHelloDialog.send(clientHello).get()) {
					return connection
				}
			}
		} catch (e: Exception) {
			exception = e
		}
		connection.close()
		throw IOException(String.format("Could not connect to %s:%d.", hostname, port), exception)
	}

	private inner class ClientHelloDialog(connection: FcpConnection) : FcpDialog<Boolean>(threadPool, connection, false) {

		override fun consumeNodeHello(nodeHello: NodeHello) {
			result = true
		}

	}

}
