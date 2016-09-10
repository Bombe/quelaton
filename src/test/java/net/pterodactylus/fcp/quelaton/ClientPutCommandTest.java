package net.pterodactylus.fcp.quelaton;

import static net.pterodactylus.fcp.quelaton.RequestProgressMatcher.isRequestProgress;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

import net.pterodactylus.fcp.Key;
import net.pterodactylus.fcp.RequestProgress;
import net.pterodactylus.fcp.test.AbstractClientPutCommandTest;

import org.hamcrest.Matcher;
import org.junit.Test;

/**
 * Unit test for {@link ClientPutCommand}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class ClientPutCommandTest extends AbstractClientPutCommandTest {

	@Test
	public void sendsCorrectCommand() throws Exception {
		client().clientPut()
				.from(new ByteArrayInputStream("Hello\n".getBytes()))
				.length(6)
				.uri("KSK@foo.txt")
				.execute();
		connectNode();
		readMessage("Hello", this::matchesDirectClientPut);
	}

	@Test
	public void succeedsOnCorrectIdentifier() throws Exception {
		Future<Optional<Key>> key = client().clientPut()
				.from(new ByteArrayInputStream("Hello\n".getBytes()))
				.length(6)
				.uri("KSK@foo.txt")
				.execute();
		connectNode();
		readMessage("Hello", this::matchesDirectClientPut);
		replyWithPutFailed("not-the-right-one");
		replyWithPutSuccessful(identifier());
		assertThat(key.get().get().getKey(), is("KSK@foo.txt"));
	}

	@Test
	public void failsOnCorrectIdentifier() throws Exception {
		Future<Optional<Key>> key = client().clientPut()
				.from(new ByteArrayInputStream("Hello\n".getBytes()))
				.length(6)
				.uri("KSK@foo.txt")
				.execute();
		connectNode();
		readMessage("Hello", this::matchesDirectClientPut);
		replyWithPutSuccessful("not-the-right-one");
		replyWithPutFailed(identifier());
		assertThat(key.get().isPresent(), is(false));
	}

	@Test
	public void renameIsSentCorrectly() throws Exception {
		client().clientPut()
				.named("otherName.txt")
				.from(new ByteArrayInputStream("Hello\n".getBytes()))
				.length(6)
				.uri("KSK@foo.txt")
				.execute();
		connectNode();
		readMessage("Hello", () -> allOf(
				hasHead("ClientPut"),
				hasParameters(1, 2, "TargetFilename=otherName.txt", "UploadFrom=direct", "DataLength=6",
						"URI=KSK@foo.txt"),
				hasTail("EndMessage", "Hello")
		));
	}

	@Test
	public void redirectIsSentCorrecly() throws Exception {
		client().clientPut().redirectTo("KSK@bar.txt").uri("KSK@foo.txt").execute();
		connectAndAssert(() -> matchesFcpMessage("ClientPut", "UploadFrom=redirect", "URI=KSK@foo.txt", "TargetURI=KSK@bar.txt"));
	}

	@Test
	public void withFileIsSentCorrectly() throws Exception {
		client().clientPut().from(new File("/tmp/data.txt")).uri("KSK@foo.txt").execute();
		connectAndAssert(() -> matchesFcpMessage("ClientPut", "UploadFrom=disk", "URI=KSK@foo.txt", "Filename=/tmp/data.txt"));
	}

	private void replyWithPutFailed(String identifier) throws Exception {
		answer(
				"PutFailed",
				"Identifier=" + identifier,
				"EndMessage"
		);
	}

	private Matcher<List<String>> matchesDirectClientPut(String... additionalLines) {
		List<String> lines = new ArrayList<>(Arrays.asList("UploadFrom=direct", "DataLength=6", "URI=KSK@foo.txt"));
		Arrays.asList(additionalLines).forEach(lines::add);
		return allOf(
				hasHead("ClientPut"),
				hasParameters(1, 2, lines.toArray(new String[lines.size()])),
				hasTail("EndMessage", "Hello")
		);
	}

	@Test
	public void clientPutDoesNotReactToProtocolErrorForDifferentIdentifier() throws Exception {
		Future<Optional<Key>> key = client().clientPut().from(new File("/tmp/data.txt")).uri("KSK@foo.txt").execute();
		connectNode();
		List<String> lines = collectUntil(is("EndMessage"));
		String identifier = extractIdentifier(lines);
		answer(
				"ProtocolError",
				"Identifier=not-the-right-one",
				"Code=25",
				"EndMessage"
		);
		answer(
				"PutSuccessful",
				"Identifier=" + identifier,
				"URI=KSK@foo.txt",
				"EndMessage"
		);
		assertThat(key.get().get().getKey(), is("KSK@foo.txt"));
	}

	@Test
	public void clientPutAbortsOnProtocolErrorOtherThan25() throws Exception {
		Future<Optional<Key>> key = client().clientPut().from(new File("/tmp/data.txt")).uri("KSK@foo.txt").execute();
		connectNode();
		List<String> lines = collectUntil(is("EndMessage"));
		String identifier = extractIdentifier(lines);
		answer(
				"ProtocolError",
				"Identifier=" + identifier,
				"Code=1",
				"EndMessage"
		);
		assertThat(key.get().isPresent(), is(false));
	}

	@Test
	public void clientPutSendsNotificationsForGeneratedKeys() throws Exception {
		List<String> generatedKeys = new CopyOnWriteArrayList<>();
		Future<Optional<Key>> key = client().clientPut()
				.onKeyGenerated(generatedKeys::add)
				.from(new ByteArrayInputStream("Hello\n".getBytes()))
				.length(6)
				.uri("KSK@foo.txt")
				.execute();
		connectNode();
		readMessage("Hello", this::matchesDirectClientPut);
		replyWithGeneratedUri();
		replyWithPutSuccessful(identifier());
		assertThat(key.get().get().getKey(), is("KSK@foo.txt"));
		assertThat(generatedKeys, contains("KSK@foo.txt"));
	}

	@Test
	public void clientPutSendsNotificationOnProgress() throws Exception {
		List<RequestProgress> requestProgress = new ArrayList<>();
		Future<Optional<Key>> key = client().clientPut()
				.onProgress(requestProgress::add)
				.from(new ByteArrayInputStream("Hello\n".getBytes()))
				.length(6)
				.uri("KSK@foo.txt")
				.execute();
		connectNode();
		readMessage("Hello", () -> matchesDirectClientPut("Verbosity=1"));
		replyWithSimpleProgress(1, 2, 3, 4, 5, 6, true, 8);
		replyWithSimpleProgress(11, 12, 13, 14, 15, 16, false, 18);
		replyWithPutSuccessful(identifier());
		assertThat(key.get().get().getKey(), is("KSK@foo.txt"));
		assertThat(requestProgress, contains(
				isRequestProgress(1, 2, 3, 4, 5, 6, true, 8),
				isRequestProgress(11, 12, 13, 14, 15, 16, false, 18)
		));
	}

}
