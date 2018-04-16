package net.pterodactylus.fcp.quelaton

import com.google.common.util.concurrent.*
import net.pterodactylus.fcp.*
import net.pterodactylus.fcp.FcpUtils.*
import java.io.*
import java.util.*
import java.util.concurrent.*
import java.util.function.*

/**
 * Implementation of the [ClientGetCommand].
 *
 * @author [David ‘Bombe’ Roden](mailto:bombe@pterodactylus.net)
 */
internal class ClientGetCommandImpl(threadPool: ExecutorService, private val connectionSupplier: ConnectionSupplier, private val identifierGenerator: Supplier<String>) : ClientGetCommand {

	private val threadPool: ListeningExecutorService
	private val onRedirects = ArrayList<Consumer<String>>()

	private var ignoreDataStore: Boolean = false
	private var dataStoreOnly: Boolean = false
	private var maxSize: Long? = null
	private var priority: Priority? = null
	private var realTime: Boolean = false
	private var global: Boolean = false

	init {
		this.threadPool = MoreExecutors.listeningDecorator(threadPool)
	}

	override fun onRedirect(onRedirect: Consumer<String>): ClientGetCommand {
		onRedirects.add(onRedirect)
		return this
	}

	override fun ignoreDataStore(): ClientGetCommand {
		ignoreDataStore = true
		return this
	}

	override fun dataStoreOnly(): ClientGetCommand {
		dataStoreOnly = true
		return this
	}

	override fun maxSize(maxSize: Long): ClientGetCommand {
		this.maxSize = maxSize
		return this
	}

	override fun priority(priority: Priority): ClientGetCommand {
		this.priority = priority
		return this
	}

	override fun realTime(): ClientGetCommand {
		realTime = true
		return this
	}

	override fun global(): ClientGetCommand {
		global = true
		return this
	}

	override fun uri(uri: String): Executable<Optional<ClientGetCommand.Data>> {
		return Executable { threadPool.submit<Optional<ClientGetCommand.Data>> { execute(uri) } }
	}

	@Throws(InterruptedException::class, ExecutionException::class, IOException::class)
	private fun execute(uri: String): Optional<ClientGetCommand.Data> {
		val clientGet = createClientGetCommand(identifierGenerator.get(), uri)
		ClientGetDialog().use { clientGetDialog -> return clientGetDialog.send(clientGet).get() }
	}

	private fun createClientGetCommand(identifier: String?, uri: String): ClientGet {
		val clientGet = ClientGet(uri, identifier, ReturnType.direct)
		if (ignoreDataStore) {
			clientGet.setIgnoreDataStore(true)
		}
		if (dataStoreOnly) {
			clientGet.setDataStoreOnly(true)
		}
		if (maxSize != null) {
			clientGet.setMaxSize(maxSize!!)
		}
		if (priority != null) {
			clientGet.setPriority(priority)
		}
		if (realTime) {
			clientGet.setRealTimeFlag(true)
		}
		if (global) {
			clientGet.setGlobal(true)
		}
		return clientGet
	}

	private inner class ClientGetDialog @Throws(IOException::class)
	constructor() : FcpDialog<Optional<ClientGetCommand.Data>>(this@ClientGetCommandImpl.threadPool, this@ClientGetCommandImpl.connectionSupplier.get(), Optional.empty()) {

		override fun consumeAllData(allData: AllData) {
			synchronized(this) {
				val contentType = allData.contentType
				val dataLength = allData.dataLength
				try {
					val payload = TempInputStream(allData.payloadInputStream, dataLength)
					result = Optional.of(createData(contentType, dataLength, payload))
				} catch (e: IOException) {
					// TODO – logging
					finish()
				}

			}
		}

		private fun createData(contentType: String, dataLength: Long, payload: InputStream): ClientGetCommand.Data {
			return object : ClientGetCommand.Data {
				override val mimeType: String
					get() = contentType

				override val inputStream: InputStream
					get() = payload

				override fun size(): Long {
					return dataLength
				}
			}
		}

		override fun consumeGetFailed(getFailed: GetFailed) {
			if (getFailed.code == 27) {
				onRedirects.forEach { onRedirect -> onRedirect.accept(getFailed.redirectURI) }
				sendMessage(createClientGetCommand(identifier, getFailed.redirectURI))
			} else {
				finish()
			}
		}

	}

}
