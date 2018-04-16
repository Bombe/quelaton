package net.pterodactylus.fcp.quelaton

import com.google.common.util.concurrent.*
import net.pterodactylus.fcp.*
import net.pterodactylus.fcp.FcpUtils.*
import net.pterodactylus.fcp.quelaton.ClientGetCommand.*
import net.pterodactylus.fcp.util.*
import java.io.*
import java.util.concurrent.*
import java.util.function.*

/**
 * Implementation of the [ClientGetCommand].
 */
internal class ClientGetCommandImpl(threadPool: ExecutorService, private val connectionSupplier: ConnectionSupplier, private val identifierGenerator: Supplier<String>) : ClientGetCommand {

	private val threadPool: ListeningExecutorService = MoreExecutors.listeningDecorator(threadPool)
	private val onRedirects = mutableListOf<(String) -> Unit>()

	private var ignoreDataStore: Boolean = false
	private var dataStoreOnly: Boolean = false
	private var maxSize: Long? = null
	private var priority: Priority? = null
	private var realTime: Boolean = false
	private var global: Boolean = false

	override fun onRedirect(newUri: (String) -> Unit) = apply {
		onRedirects.add(newUri)
	}

	override fun ignoreDataStore() = apply {
		ignoreDataStore = true
	}

	override fun dataStoreOnly() = apply {
		dataStoreOnly = true
	}

	override fun maxSize(maxSize: Long) = apply {
		this.maxSize = maxSize
	}

	override fun priority(priority: Priority) = apply {
		this.priority = priority
	}

	override fun realTime() = apply {
		realTime = true
	}

	override fun global() = apply {
		global = true
	}

	override fun uri(uri: String) =
			Executable { threadPool.submit<Data?> { execute(uri) } }

	private fun execute(uri: String) =
			ClientGetDialog().use { clientGetDialog ->
				clientGetDialog.send(createClientGetCommand(identifierGenerator.get(), uri)).get()
			}

	private fun createClientGetCommand(identifier: String?, uri: String) =
			ClientGet(uri, identifier, ReturnType.direct).apply {
				ignoreDataStore.ifTrue { setIgnoreDataStore(true) }
				dataStoreOnly.ifTrue { setDataStoreOnly(true) }
				maxSize?.also { setMaxSize(it) }
				priority?.also { setPriority(it) }
				realTime.ifTrue { setRealTimeFlag(true) }
				global.ifTrue { setGlobal(true) }
			}

	private inner class ClientGetDialog : FcpDialog<Data?>(threadPool, connectionSupplier.get()) {

		override fun consumeAllData(allData: AllData) {
			try {
				val payload = TempInputStream(allData.payloadInputStream, allData.dataLength)
				result = Data(allData.contentType, payload, allData.dataLength)
			} catch (e: IOException) {
				// TODO – logging
				finish()
			}
		}

		override fun consumeGetFailed(getFailed: GetFailed) {
			if (getFailed.code == 27) {
				onRedirects.forEach { newUri -> newUri(getFailed.redirectURI) }
				sendMessage(createClientGetCommand(identifier, getFailed.redirectURI))
			} else {
				finish()
			}
		}

	}

}
