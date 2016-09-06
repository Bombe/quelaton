package net.pterodactylus.fcp.quelaton;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.pterodactylus.fcp.Peer;
import net.pterodactylus.fcp.test.AbstractClientCommandTest;

import org.hamcrest.Matcher;
import org.junit.Test;

/**
 * Unit test for {@link ListPeerCommandTest}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class ListPeerCommandTest extends AbstractClientCommandTest {

	@Test
	public void byIdentity() throws InterruptedException, ExecutionException, IOException {
		Future<Optional<Peer>> peer = fcp.client().listPeer().byIdentity("id1").execute();
		fcp.connectAndAssert(() -> matchesListPeer("id1"));
		replyWithPeer("id1");
		assertThat(peer.get().get().getIdentity(), is("id1"));
	}

	@Test
	public void byHostAndPort() throws InterruptedException, ExecutionException, IOException {
		Future<Optional<Peer>> peer = fcp.client().listPeer().byHostAndPort("host.free.net", 12345).execute();
		fcp.connectAndAssert(() -> matchesListPeer("host.free.net:12345"));
		replyWithPeer("id1");
		assertThat(peer.get().get().getIdentity(), is("id1"));
	}

	@Test
	public void byName() throws InterruptedException, ExecutionException, IOException {
		Future<Optional<Peer>> peer = fcp.client().listPeer().byName("FriendNode").execute();
		fcp.connectAndAssert(() -> matchesListPeer("FriendNode"));
		replyWithPeer("id1");
		assertThat(peer.get().get().getIdentity(), is("id1"));
	}

	@Test
	public void unknownNodeIdentifier() throws InterruptedException, ExecutionException, IOException {
		Future<Optional<Peer>> peer = fcp.client().listPeer().byIdentity("id2").execute();
		fcp.connectAndAssert(() -> matchesListPeer("id2"));
		replyWithUnknownNodeIdentifier();
		assertThat(peer.get().isPresent(), is(false));
	}

	private Matcher<List<String>> matchesListPeer(String nodeId) {
		return fcp.matchesFcpMessage(
				"ListPeer",
				"Identifier=" + fcp.identifier(),
				"NodeIdentifier=" + nodeId
		);
	}

	private void replyWithPeer(String peerId, String... additionalLines) throws IOException {
		fcp.answer(
				"Peer",
				"Identifier=" + fcp.identifier(),
				"identity=" + peerId,
				"opennet=false",
				"ark.pubURI=SSK@3YEf.../ark",
				"ark.number=78",
				"auth.negTypes=2",
				"version=Fred,0.7,1.0,1466",
				"lastGoodVersion=Fred,0.7,1.0,1466"
		);
		fcp.answer(additionalLines);
		fcp.answer("EndMessage");
	}

	private void replyWithUnknownNodeIdentifier() throws IOException {
		fcp.answer(
				"UnknownNodeIdentifier",
				"Identifier=" + fcp.identifier(),
				"NodeIdentifier=id2",
				"EndMessage"
		);
	}

}
