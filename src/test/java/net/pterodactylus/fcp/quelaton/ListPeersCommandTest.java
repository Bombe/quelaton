package net.pterodactylus.fcp.quelaton;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.pterodactylus.fcp.Peer;
import net.pterodactylus.fcp.test.AbstractPeerCommandTest;

import org.hamcrest.Matcher;
import org.junit.Test;

/**
 * Unit test for {@link ListPeersCommand}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class ListPeersCommandTest extends AbstractPeerCommandTest {

	@Test
	public void withoutMetadataOrVolatile() throws IOException, ExecutionException, InterruptedException {
		Future<Collection<Peer>> peers = fcp.client().listPeers().execute();
		fcp.connectAndAssert(() -> matchesListPeers(false, false));
		replyWithPeer("id1");
		replyWithPeer("id2");
		sendEndOfPeerList();
		assertThat(peers.get(), hasSize(2));
		assertThat(peers.get().stream().map(Peer::getIdentity).collect(toList()), containsInAnyOrder("id1", "id2"));
	}

	@Test
	public void withMetadata() throws IOException, ExecutionException, InterruptedException {
		Future<Collection<Peer>> peers = fcp.client().listPeers().includeMetadata().execute();
		fcp.connectAndAssert(() -> matchesListPeers(false, true));
		replyWithPeer("id1", "metadata.foo=bar1");
		replyWithPeer("id2", "metadata.foo=bar2");
		sendEndOfPeerList();
		assertThat(peers.get(), hasSize(2));
		assertThat(peers.get().stream().map(peer -> peer.getMetadata("foo")).collect(toList()), containsInAnyOrder("bar1", "bar2"));
	}

	@Test
	public void withVolatile() throws IOException, ExecutionException, InterruptedException {
		Future<Collection<Peer>> peers = fcp.client().listPeers().includeVolatile().execute();
		fcp.connectAndAssert(() -> matchesListPeers(true, false));
		replyWithPeer("id1", "volatile.foo=bar1");
		replyWithPeer("id2", "volatile.foo=bar2");
		sendEndOfPeerList();
		assertThat(peers.get(), hasSize(2));
		assertThat(peers.get().stream().map(peer -> peer.getVolatile("foo")).collect(toList()), containsInAnyOrder("bar1", "bar2"));
	}

	private Matcher<List<String>> matchesListPeers(boolean withVolatile, boolean withMetadata) {
		return fcp.matchesFcpMessage(
				"ListPeers",
				"WithVolatile=" + withVolatile,
				"WithMetadata=" + withMetadata
		);
	}

	private void sendEndOfPeerList() throws IOException {
		fcp.answer(
				"EndListPeers",
				"Identifier=" + fcp.identifier(),
				"EndMessage"
		);
	}

}
