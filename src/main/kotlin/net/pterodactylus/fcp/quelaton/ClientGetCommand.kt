package net.pterodactylus.fcp.quelaton

import net.pterodactylus.fcp.*
import java.io.*
import java.util.*
import java.util.function.*

/**
 * Command that retrieves data from Freenet.
 *
 * @author [David ‘Bombe’ Roden](mailto:bombe@pterodactylus.net)
 */
interface ClientGetCommand {

	fun onRedirect(onRedirect: Consumer<String>): ClientGetCommand
	fun ignoreDataStore(): ClientGetCommand
	fun dataStoreOnly(): ClientGetCommand
	fun maxSize(maxSize: Long): ClientGetCommand
	fun priority(priority: Priority): ClientGetCommand
	fun realTime(): ClientGetCommand
	fun global(): ClientGetCommand

	fun uri(uri: String): Executable<Optional<Data>>

	interface Data {

		val mimeType: String
		val inputStream: InputStream
		fun size(): Long

	}

}
