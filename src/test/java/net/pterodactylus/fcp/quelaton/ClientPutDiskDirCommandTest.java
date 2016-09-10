package net.pterodactylus.fcp.quelaton;

import static net.pterodactylus.fcp.quelaton.RequestProgressMatcher.isRequestProgress;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

import net.pterodactylus.fcp.Key;
import net.pterodactylus.fcp.RequestProgress;
import net.pterodactylus.fcp.test.AbstractClientPutCommandTest;

import org.hamcrest.Matcher;
import org.junit.Test;

/**
 * Unit test for {@link ClientPutDiskDirCommand}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class ClientPutDiskDirCommandTest extends AbstractClientPutCommandTest {

	@Test
	public void commandIsSentCorrectly() throws Exception {
		Future<Optional<Key>> key = client().clientPutDiskDir().fromDirectory(new File("")).uri("CHK@").execute();
		connectAndAssert(this::matchesClientPutDiskDir);
		answer("PutSuccessful", "Identifier=" + identifier(), "URI=CHK@abc", "EndMessage");
		assertThat(key.get().get().getKey(), is("CHK@abc"));
	}

	@Test
	public void protocolErrorAbortsCommand() throws Exception {
		Future<Optional<Key>> key = client().clientPutDiskDir().fromDirectory(new File("")).uri("CHK@").execute();
		connectAndAssert(this::matchesClientPutDiskDir);
		replyWithProtocolError();
		assertThat(key.get().isPresent(), is(false));
	}

	@Test
	public void progressIsSentToConsumerCorrectly() throws Exception {
		List<RequestProgress> requestProgress = new ArrayList<>();
		Future<Optional<Key>> key = client().clientPutDiskDir().onProgress(requestProgress::add)
				.fromDirectory(new File("")).uri("CHK@").execute();
		connectAndAssert(() -> matchesClientPutDiskDir("Verbosity=1"));
		replyWithSimpleProgress(1, 2, 3, 4, 5, 6, true, 8);
		replyWithSimpleProgress(11, 12, 13, 14, 15, 16, false, 18);
		replyWithPutSuccessful(identifier());
		assertThat(key.get().get().getKey(), is("KSK@foo.txt"));
		assertThat(requestProgress, contains(
				isRequestProgress(1, 2, 3, 4, 5, 6, true, 8),
				isRequestProgress(11, 12, 13, 14, 15, 16, false, 18)
		));
	}

	@Test
	public void generatedUriIsSentToConsumerCorrectly() throws Exception {
		List<String> generatedKeys = new ArrayList<>();
		Future<Optional<Key>> key = client().clientPutDiskDir().onKeyGenerated(generatedKeys::add)
				.fromDirectory(new File("")).uri("CHK@").execute();
		connectAndAssert(this::matchesClientPutDiskDir);
		replyWithGeneratedUri();
		replyWithPutSuccessful(identifier());
		assertThat(key.get().get().getKey(), is("KSK@foo.txt"));
		assertThat(generatedKeys, contains("KSK@foo.txt"));
	}

	private Matcher<List<String>> matchesClientPutDiskDir(String... additionalLines) {
		List<String> lines = new ArrayList<>(Arrays.asList("Identifier=" + identifier(), "URI=CHK@", "Filename=" + new File("").getPath()));
		Arrays.asList(additionalLines).forEach(lines::add);
		return matchesFcpMessage("ClientPutDiskDir", lines.toArray(new String[lines.size()]));
	}

}
