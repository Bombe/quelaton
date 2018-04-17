package net.pterodactylus.fcp.quelaton

import net.pterodactylus.fcp.*
import java.io.*
import java.util.*
import java.util.function.*

/**
 * FCP command that inserts data into Freenet.
 */
interface ClientPutCommand {

	fun onProgress(requestProgressConsumer: Consumer<RequestProgress>): ClientPutCommand
	fun onKeyGenerated(keyGenerated: Consumer<String>): ClientPutCommand
	fun named(targetFilename: String): ClientPutCommand
	fun redirectTo(uri: String): WithUri
	fun from(file: File): WithUri
	fun from(inputStream: InputStream): WithLength

	interface WithLength {

		fun length(length: Long): WithUri

	}

	interface WithUri {

		fun uri(uri: String): Executable<Optional<Key>>

	}

}
