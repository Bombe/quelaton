package net.pterodactylus.fcp.quelaton;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.pterodactylus.fcp.test.AbstractPeerCommandTest;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * Unit test for {@link RemovePeerCommand}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class RemovePeerCommandTest extends AbstractPeerCommandTest {

	@Test
	public void byName() throws InterruptedException, ExecutionException, IOException {
		Future<Boolean> peer = client().removePeer().byName("Friend1").execute();
		connectAndAssert(() -> matchesRemovePeer("Friend1"));
		replyWithPeerRemoved("Friend1");
		assertThat(peer.get(), is(true));
	}

	@Test
	public void invalidName() throws InterruptedException, ExecutionException, IOException {
		Future<Boolean> peer = client().removePeer().byName("NotFriend1").execute();
		connectAndAssert(() -> matchesRemovePeer("NotFriend1"));
		replyWithUnknownNodeIdentifier();
		assertThat(peer.get(), is(false));
	}

	@Test
	public void byIdentity() throws InterruptedException, ExecutionException, IOException {
		Future<Boolean> peer = client().removePeer().byIdentity("id1").execute();
		connectAndAssert(() -> matchesRemovePeer("id1"));
		replyWithPeerRemoved("id1");
		assertThat(peer.get(), is(true));
	}

	@Test
	public void byHostAndPort() throws InterruptedException, ExecutionException, IOException {
		Future<Boolean> peer = client().removePeer().byHostAndPort("1.2.3.4", 5678).execute();
		connectAndAssert(() -> matchesRemovePeer("1.2.3.4:5678"));
		replyWithPeerRemoved("Friend1");
		assertThat(peer.get(), is(true));
	}

	private Matcher<List<String>> matchesRemovePeer(String nodeIdentifier) {
		return matchesFcpMessage(
				"RemovePeer",
				"Identifier=" + identifier(),
				"NodeIdentifier=" + nodeIdentifier
		);
	}

	private void replyWithPeerRemoved(String nodeIdentifier) throws IOException {
		answer(
				"PeerRemoved",
				"Identifier=" + identifier(),
				"NodeIdentifier=" + nodeIdentifier,
				"EndMessage"
		);
	}

}
