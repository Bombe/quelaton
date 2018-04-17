package net.pterodactylus.fcp.quelaton

import com.google.common.util.concurrent.*
import net.pterodactylus.fcp.*
import net.pterodactylus.fcp.quelaton.ClientPutCommand.*
import java.io.*
import java.nio.file.*
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*
import java.util.function.*

/**
 * Default [ClientPutCommand] implemented based on [FcpDialog].
 */
internal class ClientPutCommandImpl(threadPool: ExecutorService, private val connectionSupplier: ConnectionSupplier, private val identifierGenerator: Supplier<String>) : ClientPutCommand {

	private val threadPool: ListeningExecutorService = MoreExecutors.listeningDecorator(threadPool)
	private val redirectUri = AtomicReference<String>()
	private val file = AtomicReference<File>()
	private val payload = AtomicReference<InputStream>()
	private val length = AtomicLong()
	private val targetFilename = AtomicReference<String>()
	private val requestProgressConsumers = CopyOnWriteArrayList<Consumer<RequestProgress>>()
	private val keyGenerateds = CopyOnWriteArrayList<Consumer<String>>()

	override fun onProgress(requestProgressConsumer: Consumer<RequestProgress>): ClientPutCommand {
		requestProgressConsumers.add(Objects.requireNonNull(requestProgressConsumer))
		return this
	}

	override fun onKeyGenerated(keyGenerated: Consumer<String>): ClientPutCommand {
		keyGenerateds.add(keyGenerated)
		return this
	}

	override fun named(targetFilename: String): ClientPutCommand {
		this.targetFilename.set(targetFilename)
		return this
	}

	override fun redirectTo(uri: String): WithUri {
		this.redirectUri.set(Objects.requireNonNull(uri, "uri must not be null"))
		return object : WithUri {
			override fun uri(uri: String) =
					key(uri)
		}
	}

	override fun from(file: File): WithUri {
		this.file.set(Objects.requireNonNull(file, "file must not be null"))
		return object : WithUri {
			override fun uri(uri: String) =
					key(uri)
		}
	}

	override fun from(inputStream: InputStream): WithLength {
		payload.set(Objects.requireNonNull(inputStream, "inputStream must not be null"))
		return object : WithLength {
			override fun length(length: Long) =
					this@ClientPutCommandImpl.length(length)
		}
	}

	private fun length(length: Long): WithUri {
		this.length.set(length)
		return object : WithUri {
			override fun uri(uri: String) = key(uri)
		}
	}

	internal fun key(uri: String): Executable<Optional<Key>> {
		return Executable { threadPool.submit<Optional<Key>> { execute(uri) } }
	}

	private fun execute(uri: String): Optional<Key> {
		val clientPut = createClientPutCommand(uri, identifierGenerator.get())
		ClientPutDialog().use { clientPutDialog -> return clientPutDialog.send(clientPut).get() }
	}

	private fun createClientPutCommand(uri: String, identifier: String): ClientPut {
		val clientPut: ClientPut
		if (file.get() != null) {
			clientPut = createClientPutFromDisk(uri, identifier, file.get())
		} else if (redirectUri.get() != null) {
			clientPut = createClientPutRedirect(uri, identifier, redirectUri.get())
		} else {
			clientPut = createClientPutDirect(uri, identifier, length.get(), payload.get())
		}
		if (targetFilename.get() != null) {
			clientPut.setTargetFilename(targetFilename.get())
		}
		if (!requestProgressConsumers.isEmpty()) {
			clientPut.setVerbosity(Verbosity.PROGRESS)
		}
		return clientPut
	}

	private fun createClientPutFromDisk(uri: String, identifier: String, file: File): ClientPut {
		val clientPut = ClientPut(uri, identifier, UploadFrom.disk)
		clientPut.setFilename(file.absolutePath)
		return clientPut
	}

	private fun createClientPutRedirect(uri: String, identifier: String, redirectUri: String): ClientPut {
		val clientPut = ClientPut(uri, identifier, UploadFrom.redirect)
		clientPut.setTargetURI(redirectUri)
		return clientPut
	}

	private fun createClientPutDirect(uri: String, identifier: String, length: Long, payload: InputStream): ClientPut {
		val clientPut = ClientPut(uri, identifier, UploadFrom.direct)
		clientPut.setDataLength(length)
		clientPut.setPayloadInputStream(payload)
		return clientPut
	}

	private inner class ClientPutDialog : FcpDialog<Optional<Key>>(threadPool, connectionSupplier.get(), Optional.empty()) {

		private val originalClientPut = AtomicReference<FcpMessage>()
		private val directory = AtomicReference<String>()

		override fun send(fcpMessage: FcpMessage): ListenableFuture<Optional<Key>> {
			originalClientPut.set(fcpMessage)
			val filename = fcpMessage.getField("Filename")
			if (filename != null) {
				directory.set(File(filename).parent)
			}
			return super.send(fcpMessage)
		}

		override fun consumeSimpleProgress(simpleProgress: SimpleProgress) {
			val requestProgress = RequestProgress(
					simpleProgress.total,
					simpleProgress.required,
					simpleProgress.failed,
					simpleProgress.fatallyFailed,
					simpleProgress.lastProgress,
					simpleProgress.succeeded,
					simpleProgress.isFinalizedTotal,
					simpleProgress.minSuccessFetchBlocks
			)
			requestProgressConsumers.forEach { consumer -> consumer.accept(requestProgress) }
		}

		override fun consumeURIGenerated(uriGenerated: URIGenerated) {
			for (keyGenerated in keyGenerateds) {
				keyGenerated.accept(uriGenerated.uri)
			}
		}

		override fun consumePutSuccessful(putSuccessful: PutSuccessful) {
			result = Optional.of(Key(putSuccessful.uri))
		}

		override fun consumePutFailed(putFailed: PutFailed) {
			finish()
		}

		override fun consumeProtocolError(protocolError: ProtocolError) {
			if (protocolError.code == 25) {
				identifier = directory.get()
				sendMessage(TestDDARequest(directory.get(), true, false))
			} else {
				finish()
			}
		}

		override fun consumeTestDDAReply(testDDAReply: TestDDAReply) {
			try {
				val readContent = Files.readAllLines(File(testDDAReply.readFilename).toPath())[0]
				sendMessage(TestDDAResponse(directory.get(), readContent))
			} catch (e: IOException) {
				sendMessage(TestDDAResponse(directory.get(), "failed-to-read"))
			}

		}

		override fun consumeTestDDAComplete(testDDAComplete: TestDDAComplete) {
			identifier = originalClientPut.get().getField("Identifier")
			sendMessage(originalClientPut.get())
		}

	}

}
