package net.pterodactylus.fcp.quelaton;

import static com.google.common.io.ByteStreams.toByteArray;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.pterodactylus.fcp.Priority;
import net.pterodactylus.fcp.quelaton.ClientGetCommand.Data;
import net.pterodactylus.fcp.test.AbstractClientCommandTest;

import org.junit.Test;

/**
 * Unit test for {@link ClientGetCommand}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class ClientGetCommandTest extends AbstractClientCommandTest {

	@Test
	public void works() throws Exception {
		Future<Optional<Data>> dataFuture = client().clientGet().uri("KSK@foo.txt").execute();
		connectAndAssert(() -> matchesFcpMessage("ClientGet", "URI=KSK@foo.txt", "ReturnType=direct"));
		replyWithAllData("not-test", "Hello World", "text/plain;charset=latin-9");
		replyWithAllData(identifier(), "Hello", "text/plain;charset=utf-8");
		Optional<Data> data = dataFuture.get();
		verifyData(data);
	}

	@Test
	public void getFailedIsRecognized() throws Exception {
		Future<Optional<Data>> dataFuture = client().clientGet().uri("KSK@foo.txt").execute();
		connectAndAssert(() -> matchesFcpMessage("ClientGet", "URI=KSK@foo.txt"));
		replyWithGetFailed("not-test");
		replyWithGetFailed(identifier());
		Optional<Data> data = dataFuture.get();
		assertThat(data.isPresent(), is(false));
	}

	@Test
	public void getFailedForDifferentIdentifierIsIgnored() throws Exception {
		Future<Optional<Data>> dataFuture = client().clientGet().uri("KSK@foo.txt").execute();
		connectAndAssert(() -> matchesFcpMessage("ClientGet", "URI=KSK@foo.txt"));
		replyWithGetFailed("not-test");
		replyWithAllData(identifier(), "Hello", "text/plain;charset=utf-8");
		Optional<Data> data = dataFuture.get();
		verifyData(data);
	}

	@Test(expected = ExecutionException.class)
	public void connectionClosedIsRecognized() throws Exception {
		Future<Optional<Data>> dataFuture = client().clientGet().uri("KSK@foo.txt").execute();
		connectAndAssert(() -> matchesFcpMessage("ClientGet", "URI=KSK@foo.txt"));
		closeFcpServer();
		dataFuture.get();
	}

	@Test
	public void withIgnoreData() throws Exception {
		client().clientGet().ignoreDataStore().uri("KSK@foo.txt").execute();
		connectAndAssert(() -> matchesFcpMessage("ClientGet", "URI=KSK@foo.txt", "IgnoreDS=true"));
	}

	@Test
	public void withDataStoreOnly() throws Exception {
		client().clientGet().dataStoreOnly().uri("KSK@foo.txt").execute();
		connectAndAssert(() -> matchesFcpMessage("ClientGet", "URI=KSK@foo.txt", "DSonly=true"));
	}

	@Test
	public void clientGetWithMaxSizeSettingSendsCorrectCommands() throws Exception {
		client().clientGet().maxSize(1048576).uri("KSK@foo.txt").execute();
		connectAndAssert(() -> matchesFcpMessage("ClientGet", "URI=KSK@foo.txt", "MaxSize=1048576"));
	}

	@Test
	public void clientGetWithPrioritySettingSendsCorrectCommands() throws Exception {
		client().clientGet().priority(Priority.interactive).uri("KSK@foo.txt").execute();
		connectAndAssert(() -> matchesFcpMessage("ClientGet", "URI=KSK@foo.txt", "PriorityClass=1"));
	}

	@Test
	public void clientGetWithRealTimeSettingSendsCorrectCommands() throws Exception {
		client().clientGet().realTime().uri("KSK@foo.txt").execute();
		connectAndAssert(() -> matchesFcpMessage("ClientGet", "URI=KSK@foo.txt", "RealTimeFlag=true"));
	}

	@Test
	public void clientGetWithGlobalSettingSendsCorrectCommands() throws Exception {
		client().clientGet().global().uri("KSK@foo.txt").execute();
		connectAndAssert(() -> matchesFcpMessage("ClientGet", "URI=KSK@foo.txt", "Global=true"));
	}

	@Test
	public void clientGetFollowsRedirect() throws Exception {
		Future<Optional<Data>> data = client().clientGet().uri("USK@foo/bar").execute();
		connectAndAssert(() -> matchesFcpMessage("ClientGet", "URI=USK@foo/bar"));
		replyWithRedirect("USK@foo/baz");
		readMessage(() -> matchesFcpMessage("ClientGet", "URI=USK@foo/baz"));
		replyWithAllData(identifier(), "Hello", "text/plain;charset=utf-8");
		verifyData(data.get());
	}

	@Test
	public void clientGetNotifiesListenersOnRedirect() throws Exception {
		List<String> redirects = new ArrayList<>();
		Future<Optional<Data>> data = client().clientGet().onRedirect(redirects::add).uri("USK@foo/bar").execute();
		connectAndAssert(() -> matchesFcpMessage("ClientGet", "URI=USK@foo/bar"));
		replyWithRedirect("USK@foo/baz");
		readMessage(() -> matchesFcpMessage("ClientGet", "URI=USK@foo/baz"));
		replyWithRedirect("USK@foo/quux");
		readMessage(() -> matchesFcpMessage("ClientGet", "URI=USK@foo/quux"));
		replyWithAllData(identifier(), "Hello", "text/plain;charset=utf-8");
		verifyData(data.get());
		assertThat(redirects, contains("USK@foo/baz", "USK@foo/quux"));
	}

	private void replyWithGetFailed(String identifier) throws Exception {
		answer(
				"GetFailed",
				"Identifier=" + identifier,
				"Code=3",
				"EndMessage"
		);
	}

	private void replyWithRedirect(String newUri) throws Exception {
		answer(
				"GetFailed",
				"Identifier=" + identifier(),
				"Code=27",
				"RedirectURI=" + newUri,
				"EndMessage"
		);
	}

	private void replyWithAllData(String identifier, String text, String contentType) throws Exception {
		answer(
				"AllData",
				"Identifier=" + identifier,
				"DataLength=" + (text.length() + 1),
				"StartupTime=1435610539000",
				"CompletionTime=1435610540000",
				"Metadata.ContentType=" + contentType,
				"Data",
				text
		);
	}

	private void verifyData(Optional<Data> data) throws Exception {
		assertThat(data.get().getMimeType(), is("text/plain;charset=utf-8"));
		assertThat(data.get().size(), is(6L));
		assertThat(toByteArray(data.get().getInputStream()), is("Hello\n".getBytes(UTF_8)));
	}

}
