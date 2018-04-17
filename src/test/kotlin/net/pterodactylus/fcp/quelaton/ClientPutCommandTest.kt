package net.pterodactylus.fcp.quelaton

import net.pterodactylus.fcp.*
import net.pterodactylus.fcp.quelaton.RequestProgressMatcher.*
import net.pterodactylus.fcp.test.*
import org.hamcrest.*
import org.hamcrest.MatcherAssert.*
import org.hamcrest.Matchers.*
import org.junit.*
import java.io.*
import java.util.*
import java.util.concurrent.*
import java.util.function.*

/**
 * Unit test for [ClientPutCommand].
 */
class ClientPutCommandTest : AbstractClientPutCommandTest() {

	@Test
	fun sendsCorrectCommand() {
		client().clientPut()
				.from(ByteArrayInputStream("Hello\n".toByteArray()))
				.length(6)
				.uri("KSK@foo.txt")
				.execute()
		connectNode()
		readMessage("Hello", { this.matchesDirectClientPut() })
	}

	@Test
	fun succeedsOnCorrectIdentifier() {
		val key = client().clientPut()
				.from(ByteArrayInputStream("Hello\n".toByteArray()))
				.length(6)
				.uri("KSK@foo.txt")
				.execute()
		connectNode()
		readMessage("Hello", { this.matchesDirectClientPut() })
		replyWithPutFailed("not-the-right-one")
		replyWithPutSuccessful(identifier())
		assertThat(key.get().get().key, `is`("KSK@foo.txt"))
	}

	@Test
	fun failsOnCorrectIdentifier() {
		val key = client().clientPut()
				.from(ByteArrayInputStream("Hello\n".toByteArray()))
				.length(6)
				.uri("KSK@foo.txt")
				.execute()
		connectNode()
		readMessage("Hello", { this.matchesDirectClientPut() })
		replyWithPutSuccessful("not-the-right-one")
		replyWithPutFailed(identifier())
		assertThat(key.get().isPresent, `is`(false))
	}

	@Test
	fun renameIsSentCorrectly() {
		client().clientPut()
				.named("otherName.txt")
				.from(ByteArrayInputStream("Hello\n".toByteArray()))
				.length(6)
				.uri("KSK@foo.txt")
				.execute()
		connectNode()
		readMessage("Hello") {
			allOf(
					hasHead("ClientPut"),
					hasParameters(1, 2,
							"TargetFilename=otherName.txt",
							"UploadFrom=direct",
							"DataLength=6",
							"URI=KSK@foo.txt"),
					hasTail("Data", "Hello")
			)
		}
	}

	@Test
	fun redirectIsSentCorrecly() {
		client().clientPut().redirectTo("KSK@bar.txt").uri("KSK@foo.txt").execute()
		connectAndAssert { matchesFcpMessage("ClientPut", "UploadFrom=redirect", "URI=KSK@foo.txt", "TargetURI=KSK@bar.txt") }
	}

	@Test
	fun withFileIsSentCorrectly() {
		client().clientPut().from(File("/tmp/data.txt")).uri("KSK@foo.txt").execute()
		connectAndAssert { matchesFcpMessage("ClientPut", "UploadFrom=disk", "URI=KSK@foo.txt", "Filename=/tmp/data.txt") }
	}

	private fun replyWithPutFailed(identifier: String) {
		answer(
				"PutFailed",
				"Identifier=$identifier",
				"EndMessage"
		)
	}

	private fun matchesDirectClientPut(vararg additionalLines: String): Matcher<List<String>> {
		val lines = ArrayList(Arrays.asList("UploadFrom=direct", "DataLength=6", "URI=KSK@foo.txt"))
		Arrays.asList(*additionalLines).forEach(Consumer<String> { lines.add(it) })
		return allOf(
				hasHead("ClientPut"),
				hasParameters(1, 2, *lines.toTypedArray()),
				hasTail("Data", "Hello")
		)
	}

	@Test
	fun clientPutDoesNotReactToProtocolErrorForDifferentIdentifier() {
		val key = client().clientPut().from(File("/tmp/data.txt")).uri("KSK@foo.txt").execute()
		connectNode()
		val lines = collectUntil(`is`("EndMessage"))
		val identifier = extractIdentifier(lines)
		answer(
				"ProtocolError",
				"Identifier=not-the-right-one",
				"Code=25",
				"EndMessage"
		)
		answer(
				"PutSuccessful",
				"Identifier=$identifier",
				"URI=KSK@foo.txt",
				"EndMessage"
		)
		assertThat(key.get().get().key, `is`("KSK@foo.txt"))
	}

	@Test
	fun clientPutAbortsOnProtocolErrorOtherThan25() {
		val key = client().clientPut().from(File("/tmp/data.txt")).uri("KSK@foo.txt").execute()
		connectNode()
		val lines = collectUntil(`is`("EndMessage"))
		val identifier = extractIdentifier(lines)
		answer(
				"ProtocolError",
				"Identifier=$identifier",
				"Code=1",
				"EndMessage"
		)
		assertThat(key.get().isPresent, `is`(false))
	}

	@Test
	fun clientPutSendsNotificationsForGeneratedKeys() {
		val generatedKeys = CopyOnWriteArrayList<String>()
		val key = client().clientPut()
				.onKeyGenerated(Consumer<String> { generatedKeys.add(it) })
				.from(ByteArrayInputStream("Hello\n".toByteArray()))
				.length(6)
				.uri("KSK@foo.txt")
				.execute()
		connectNode()
		readMessage("Hello", { this.matchesDirectClientPut() })
		replyWithGeneratedUri()
		replyWithPutSuccessful(identifier())
		assertThat(key.get().get().key, `is`("KSK@foo.txt"))
		assertThat<List<String>>(generatedKeys, contains("KSK@foo.txt"))
	}

	@Test
	fun clientPutSendsNotificationOnProgress() {
		val requestProgress = ArrayList<RequestProgress>()
		val key = client().clientPut()
				.onProgress(Consumer<RequestProgress> { requestProgress.add(it) })
				.from(ByteArrayInputStream("Hello\n".toByteArray()))
				.length(6)
				.uri("KSK@foo.txt")
				.execute()
		connectNode()
		readMessage("Hello") { matchesDirectClientPut("Verbosity=1") }
		replyWithSimpleProgress(1, 2, 3, 4, 5, 6, true, 8)
		replyWithSimpleProgress(11, 12, 13, 14, 15, 16, false, 18)
		replyWithPutSuccessful(identifier())
		assertThat(key.get().get().key, `is`("KSK@foo.txt"))
		assertThat<List<RequestProgress>>(requestProgress, contains(
				isRequestProgress(1, 2, 3, 4, 5, 6, true, 8),
				isRequestProgress(11, 12, 13, 14, 15, 16, false, 18)
		))
	}

}
