package net.pterodactylus.fcp.quelaton

import net.pterodactylus.fcp.*
import java.io.*
import java.util.function.*

/**
 * Command that retrieves data from Freenet.
 */
interface ClientGetCommand {

	fun onRedirect(newUri: (String) -> Unit): ClientGetCommand
	fun ignoreDataStore(): ClientGetCommand
	fun dataStoreOnly(): ClientGetCommand
	fun maxSize(maxSize: Long): ClientGetCommand
	fun priority(priority: Priority): ClientGetCommand
	fun realTime(): ClientGetCommand
	fun global(): ClientGetCommand

	fun uri(uri: String): Executable<Data?>

	data class Data(val mimeType: String, val inputStream: InputStream, val size: Long)

}
