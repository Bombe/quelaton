package net.pterodactylus.fcp.quelaton

import net.pterodactylus.fcp.*
import net.pterodactylus.fcp.quelaton.ClientGetCommand.*
import net.pterodactylus.fcp.test.*
import org.hamcrest.MatcherAssert.*
import org.hamcrest.Matchers.*
import org.junit.*
import java.nio.charset.StandardCharsets.*
import java.util.*
import java.util.concurrent.*

/**
 * Unit test for [ClientGetCommand].
 */
class ClientGetCommandTest : AbstractClientCommandTest() {

	@Test
	fun works() {
		val dataFuture = client().clientGet().uri("KSK@foo.txt").execute()
		connectAndAssert { matchesFcpMessage("ClientGet", "URI=KSK@foo.txt", "ReturnType=direct") }
		replyWithAllData("not-test", "Hello World", "text/plain;charset=latin-9")
		replyWithAllData(identifier(), "Hello", "text/plain;charset=utf-8")
		val data = dataFuture.get()
		verifyData(data)
	}

	@Test
	fun getFailedIsRecognized() {
		val dataFuture = client().clientGet().uri("KSK@foo.txt").execute()
		connectAndAssert { matchesFcpMessage("ClientGet", "URI=KSK@foo.txt") }
		replyWithGetFailed("not-test")
		replyWithGetFailed(identifier())
		val data = dataFuture.get()
		assertThat(data, nullValue())
	}

	@Test
	fun getFailedForDifferentIdentifierIsIgnored() {
		val dataFuture = client().clientGet().uri("KSK@foo.txt").execute()
		connectAndAssert { matchesFcpMessage("ClientGet", "URI=KSK@foo.txt") }
		replyWithGetFailed("not-test")
		replyWithAllData(identifier(), "Hello", "text/plain;charset=utf-8")
		val data = dataFuture.get()
		verifyData(data)
	}

	@Test(expected = ExecutionException::class)
	fun connectionClosedIsRecognized() {
		val dataFuture = client().clientGet().uri("KSK@foo.txt").execute()
		connectAndAssert { matchesFcpMessage("ClientGet", "URI=KSK@foo.txt") }
		closeFcpServer()
		dataFuture.get()
	}

	@Test
	fun withIgnoreData() {
		client().clientGet().ignoreDataStore().uri("KSK@foo.txt").execute()
		connectAndAssert { matchesFcpMessage("ClientGet", "URI=KSK@foo.txt", "IgnoreDS=true") }
	}

	@Test
	fun withDataStoreOnly() {
		client().clientGet().dataStoreOnly().uri("KSK@foo.txt").execute()
		connectAndAssert { matchesFcpMessage("ClientGet", "URI=KSK@foo.txt", "DSonly=true") }
	}

	@Test
	fun clientGetWithMaxSizeSettingSendsCorrectCommands() {
		client().clientGet().maxSize(1048576).uri("KSK@foo.txt").execute()
		connectAndAssert { matchesFcpMessage("ClientGet", "URI=KSK@foo.txt", "MaxSize=1048576") }
	}

	@Test
	fun clientGetWithPrioritySettingSendsCorrectCommands() {
		client().clientGet().priority(Priority.interactive).uri("KSK@foo.txt").execute()
		connectAndAssert { matchesFcpMessage("ClientGet", "URI=KSK@foo.txt", "PriorityClass=1") }
	}

	@Test
	fun clientGetWithRealTimeSettingSendsCorrectCommands() {
		client().clientGet().realTime().uri("KSK@foo.txt").execute()
		connectAndAssert { matchesFcpMessage("ClientGet", "URI=KSK@foo.txt", "RealTimeFlag=true") }
	}

	@Test
	fun clientGetWithGlobalSettingSendsCorrectCommands() {
		client().clientGet().global().uri("KSK@foo.txt").execute()
		connectAndAssert { matchesFcpMessage("ClientGet", "URI=KSK@foo.txt", "Global=true") }
	}

	@Test
	fun clientGetFollowsRedirect() {
		val data = client().clientGet().uri("USK@foo/bar").execute()
		connectAndAssert { matchesFcpMessage("ClientGet", "URI=USK@foo/bar") }
		replyWithRedirect("USK@foo/baz")
		readMessage { matchesFcpMessage("ClientGet", "URI=USK@foo/baz") }
		replyWithAllData(identifier(), "Hello", "text/plain;charset=utf-8")
		verifyData(data.get())
	}

	@Test
	fun clientGetNotifiesListenersOnRedirect() {
		val redirects = ArrayList<String>()
		val data = client().clientGet().onRedirect { redirects.add(it) }.uri("USK@foo/bar").execute()
		connectAndAssert { matchesFcpMessage("ClientGet", "URI=USK@foo/bar") }
		replyWithRedirect("USK@foo/baz")
		readMessage { matchesFcpMessage("ClientGet", "URI=USK@foo/baz") }
		replyWithRedirect("USK@foo/quux")
		readMessage { matchesFcpMessage("ClientGet", "URI=USK@foo/quux") }
		replyWithAllData(identifier(), "Hello", "text/plain;charset=utf-8")
		verifyData(data.get())
		assertThat<List<String>>(redirects, contains("USK@foo/baz", "USK@foo/quux"))
	}

	private fun replyWithGetFailed(identifier: String) {
		answer(
				"GetFailed",
				"Identifier=$identifier",
				"Code=3",
				"EndMessage"
		)
	}

	private fun replyWithRedirect(newUri: String) {
		answer(
				"GetFailed",
				"Identifier=" + identifier(),
				"Code=27",
				"RedirectURI=$newUri",
				"EndMessage"
		)
	}

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

	private fun verifyData(data: Data?) {
		assertThat(data?.mimeType, equalTo("text/plain;charset=utf-8"))
		assertThat(data?.size, equalTo(6L))
		assertThat(data?.inputStream?.readBytes(), equalTo("Hello\n".toByteArray(UTF_8)))
	}

}
