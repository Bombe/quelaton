package net.pterodactylus.fcp.quelaton;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.pterodactylus.fcp.Peer;
import net.pterodactylus.fcp.test.AbstractPeerCommandTest;

import org.hamcrest.Matcher;
import org.junit.Test;

/**
 * Unit test for {@link ListPeerCommand}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class ListPeerCommandTest extends AbstractPeerCommandTest {

	@Test
	public void byIdentity() throws InterruptedException, ExecutionException, IOException {
		Future<Optional<Peer>> peer = client().listPeer().byIdentity("id1").execute();
		connectAndAssert(() -> matchesListPeer("id1"));
		replyWithPeer("id1");
		assertThat(peer.get().get().getIdentity(), is("id1"));
	}

	@Test
	public void byHostAndPort() throws InterruptedException, ExecutionException, IOException {
		Future<Optional<Peer>> peer = client().listPeer().byHostAndPort("host.free.net", 12345).execute();
		connectAndAssert(() -> matchesListPeer("host.free.net:12345"));
		replyWithPeer("id1");
		assertThat(peer.get().get().getIdentity(), is("id1"));
	}

	@Test
	public void byName() throws InterruptedException, ExecutionException, IOException {
		Future<Optional<Peer>> peer = client().listPeer().byName("FriendNode").execute();
		connectAndAssert(() -> matchesListPeer("FriendNode"));
		replyWithPeer("id1");
		assertThat(peer.get().get().getIdentity(), is("id1"));
	}

	@Test
	public void unknownNodeIdentifier() throws InterruptedException, ExecutionException, IOException {
		Future<Optional<Peer>> peer = client().listPeer().byIdentity("id2").execute();
		connectAndAssert(() -> matchesListPeer("id2"));
		replyWithUnknownNodeIdentifier();
		assertThat(peer.get().isPresent(), is(false));
	}

	private Matcher<List<String>> matchesListPeer(String nodeId) {
		return matchesFcpMessage(
				"ListPeer",
				"Identifier=" + identifier(),
				"NodeIdentifier=" + nodeId
		);
	}

	private void replyWithUnknownNodeIdentifier() throws IOException {
		answer(
				"UnknownNodeIdentifier",
				"Identifier=" + identifier(),
				"NodeIdentifier=id2",
				"EndMessage"
		);
	}

}
