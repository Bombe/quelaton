package net.pterodactylus.fcp.quelaton;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.pterodactylus.fcp.ARK;
import net.pterodactylus.fcp.DSAGroup;
import net.pterodactylus.fcp.NodeRef;
import net.pterodactylus.fcp.Peer;
import net.pterodactylus.fcp.test.AbstractPeerCommandTest;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * Unit test for {@link AddPeerCommand}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class AddPeerCommandTest extends AbstractPeerCommandTest {

	@Test
	public void fromFile() throws InterruptedException, ExecutionException, IOException {
		Future<Optional<Peer>> peer = client().addPeer().fromFile(new File("/tmp/ref.txt")).execute();
		connectAndAssert(() -> allOf(matchesAddPeer(), hasItem("File=/tmp/ref.txt")));
		replyWithPeer("id1");
		assertThat(peer.get().get().getIdentity(), is("id1"));
	}

	@Test
	public void fromUrl() throws InterruptedException, ExecutionException, IOException {
		Future<Optional<Peer>> peer = client().addPeer().fromURL(new URL("http://node.ref/")).execute();
		connectAndAssert(() -> allOf(matchesAddPeer(), hasItem("URL=http://node.ref/")));
		replyWithPeer("id1");
		assertThat(peer.get().get().getIdentity(), is("id1"));
	}

	@Test
	public void fromNodeRef() throws InterruptedException, ExecutionException, IOException {
		NodeRef nodeRef = createNodeRef();
		Future<Optional<Peer>> peer = client().addPeer().fromNodeRef(nodeRef).execute();
		connectAndAssert(() -> allOf(matchesAddPeer(), Matchers.<String>hasItems(
				"myName=name",
				"ark.pubURI=public",
				"ark.number=1",
				"dsaGroup.g=base",
				"dsaGroup.p=prime",
				"dsaGroup.q=subprime",
				"dsaPubKey.y=dsa-public",
				"physical.udp=1.2.3.4:5678",
				"auth.negTypes=3;5",
				"sig=sig"
		)));
		replyWithPeer("id1");
		assertThat(peer.get().get().getIdentity(), is("id1"));
	}

	@Test
	public void protocolErrorEndsCommand() throws InterruptedException, ExecutionException, IOException {
		Future<Optional<Peer>> peer = client().addPeer().fromFile(new File("/tmp/ref.txt")).execute();
		connectAndAssert(() -> allOf(matchesAddPeer(), hasItem("File=/tmp/ref.txt")));
		replyWithProtocolError();
		assertThat(peer.get().isPresent(), is(false));
	}

	private NodeRef createNodeRef() {
		NodeRef nodeRef = new NodeRef();
		nodeRef.setIdentity("id1");
		nodeRef.setName("name");
		nodeRef.setARK(new ARK("public", "1"));
		nodeRef.setDSAGroup(new DSAGroup("base", "prime", "subprime"));
		nodeRef.setNegotiationTypes(new int[] { 3, 5 });
		nodeRef.setPhysicalUDP("1.2.3.4:5678");
		nodeRef.setDSAPublicKey("dsa-public");
		nodeRef.setSignature("sig");
		return nodeRef;
	}

	private Matcher<List<String>> matchesAddPeer() {
		return matchesFcpMessage(
				"AddPeer",
				"Identifier=" + identifier()
		);
	}

}
