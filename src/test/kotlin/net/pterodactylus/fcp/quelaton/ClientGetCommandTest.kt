package net.pterodactylus.fcp.quelaton

import com.google.common.io.ByteStreams.*
import net.pterodactylus.fcp.*
import net.pterodactylus.fcp.quelaton.ClientGetCommand.*
import net.pterodactylus.fcp.test.*
import org.hamcrest.MatcherAssert.*
import org.hamcrest.Matchers.*
import org.junit.*
import java.nio.charset.StandardCharsets.*
import java.util.*
import java.util.concurrent.*
import java.util.function.*

/**
 * Unit test for [ClientGetCommand].
 *
 * @author [David ‘Bombe’ Roden](mailto:bombe@pterodactylus.net)
 */
class ClientGetCommandTest : AbstractClientCommandTest() {

	@Test
	@Throws(Exception::class)
	fun works() {
		val dataFuture = client().clientGet().uri("KSK@foo.txt").execute()
		connectAndAssert { matchesFcpMessage("ClientGet", "URI=KSK@foo.txt", "ReturnType=direct") }
		replyWithAllData("not-test", "Hello World", "text/plain;charset=latin-9")
		replyWithAllData(identifier(), "Hello", "text/plain;charset=utf-8")
		val data = dataFuture.get()
		verifyData(data)
	}

	@Test
	@Throws(Exception::class)
	fun getFailedIsRecognized() {
		val dataFuture = client().clientGet().uri("KSK@foo.txt").execute()
		connectAndAssert { matchesFcpMessage("ClientGet", "URI=KSK@foo.txt") }
		replyWithGetFailed("not-test")
		replyWithGetFailed(identifier())
		val data = dataFuture.get()
		assertThat(data.isPresent, `is`(false))
	}

	@Test
	@Throws(Exception::class)
	fun getFailedForDifferentIdentifierIsIgnored() {
		val dataFuture = client().clientGet().uri("KSK@foo.txt").execute()
		connectAndAssert { matchesFcpMessage("ClientGet", "URI=KSK@foo.txt") }
		replyWithGetFailed("not-test")
		replyWithAllData(identifier(), "Hello", "text/plain;charset=utf-8")
		val data = dataFuture.get()
		verifyData(data)
	}

	@Test(expected = ExecutionException::class)
	@Throws(Exception::class)
	fun connectionClosedIsRecognized() {
		val dataFuture = client().clientGet().uri("KSK@foo.txt").execute()
		connectAndAssert { matchesFcpMessage("ClientGet", "URI=KSK@foo.txt") }
		closeFcpServer()
		dataFuture.get()
	}

	@Test
	@Throws(Exception::class)
	fun withIgnoreData() {
		client().clientGet().ignoreDataStore().uri("KSK@foo.txt").execute()
		connectAndAssert { matchesFcpMessage("ClientGet", "URI=KSK@foo.txt", "IgnoreDS=true") }
	}

	@Test
	@Throws(Exception::class)
	fun withDataStoreOnly() {
		client().clientGet().dataStoreOnly().uri("KSK@foo.txt").execute()
		connectAndAssert { matchesFcpMessage("ClientGet", "URI=KSK@foo.txt", "DSonly=true") }
	}

	@Test
	@Throws(Exception::class)
	fun clientGetWithMaxSizeSettingSendsCorrectCommands() {
		client().clientGet().maxSize(1048576).uri("KSK@foo.txt").execute()
		connectAndAssert { matchesFcpMessage("ClientGet", "URI=KSK@foo.txt", "MaxSize=1048576") }
	}

	@Test
	@Throws(Exception::class)
	fun clientGetWithPrioritySettingSendsCorrectCommands() {
		client().clientGet().priority(Priority.interactive).uri("KSK@foo.txt").execute()
		connectAndAssert { matchesFcpMessage("ClientGet", "URI=KSK@foo.txt", "PriorityClass=1") }
	}

	@Test
	@Throws(Exception::class)
	fun clientGetWithRealTimeSettingSendsCorrectCommands() {
		client().clientGet().realTime().uri("KSK@foo.txt").execute()
		connectAndAssert { matchesFcpMessage("ClientGet", "URI=KSK@foo.txt", "RealTimeFlag=true") }
	}

	@Test
	@Throws(Exception::class)
	fun clientGetWithGlobalSettingSendsCorrectCommands() {
		client().clientGet().global().uri("KSK@foo.txt").execute()
		connectAndAssert { matchesFcpMessage("ClientGet", "URI=KSK@foo.txt", "Global=true") }
	}

	@Test
	@Throws(Exception::class)
	fun clientGetFollowsRedirect() {
		val data = client().clientGet().uri("USK@foo/bar").execute()
		connectAndAssert { matchesFcpMessage("ClientGet", "URI=USK@foo/bar") }
		replyWithRedirect("USK@foo/baz")
		readMessage { matchesFcpMessage("ClientGet", "URI=USK@foo/baz") }
		replyWithAllData(identifier(), "Hello", "text/plain;charset=utf-8")
		verifyData(data.get())
	}

	@Test
	@Throws(Exception::class)
	fun clientGetNotifiesListenersOnRedirect() {
		val redirects = ArrayList<String>()
		val data = client().clientGet().onRedirect(Consumer<String> { redirects.add(it) }).uri("USK@foo/bar").execute()
		connectAndAssert { matchesFcpMessage("ClientGet", "URI=USK@foo/bar") }
		replyWithRedirect("USK@foo/baz")
		readMessage { matchesFcpMessage("ClientGet", "URI=USK@foo/baz") }
		replyWithRedirect("USK@foo/quux")
		readMessage { matchesFcpMessage("ClientGet", "URI=USK@foo/quux") }
		replyWithAllData(identifier(), "Hello", "text/plain;charset=utf-8")
		verifyData(data.get())
		assertThat<List<String>>(redirects, contains("USK@foo/baz", "USK@foo/quux"))
	}

	@Throws(Exception::class)
	private fun replyWithGetFailed(identifier: String) {
		answer(
				"GetFailed",
				"Identifier=$identifier",
				"Code=3",
				"EndMessage"
		)
	}

	@Throws(Exception::class)
	private fun replyWithRedirect(newUri: String) {
		answer(
				"GetFailed",
				"Identifier=" + identifier(),
				"Code=27",
				"RedirectURI=$newUri",
				"EndMessage"
		)
	}

	@Throws(Exception::class)
	private fun replyWithAllData(identifier: String, text: String, contentType: String) {
		answer(
				"AllData",
				"Identifier=$identifier",
				"DataLength=" + (text.length + 1),
				"StartupTime=1435610539000",
				"CompletionTime=1435610540000",
				"Metadata.ContentType=$contentType",
				"Data",
				text
		)
	}

	@Throws(Exception::class)
	private fun verifyData(data: Optional<Data>) {
		assertThat(data.get().mimeType, `is`("text/plain;charset=utf-8"))
		assertThat(data.get().size(), `is`(6L))
		assertThat(toByteArray(data.get().inputStream), `is`("Hello\n".toByteArray(UTF_8)))
	}

}
