package net.pterodactylus.fcp.quelaton;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

import net.pterodactylus.fcp.Peer;
import net.pterodactylus.fcp.test.AbstractPeerCommandTest;

import org.hamcrest.Matcher;
import org.junit.Test;

/**
 * Unit test for {@link ModifyPeerCommand}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class ModifyPeerCommandTest extends AbstractPeerCommandTest {

	@Test
	public void defaultFcpClientCanEnablePeerByName() throws Exception {
		Future<Optional<Peer>> peer = client().modifyPeer().enable().byName("id1").execute();
		connectAndAssert(() -> matchesModifyPeer("id1", "IsDisabled", false));
		replyWithPeer("id1");
		assertThat(peer.get().get().getIdentity(), is("id1"));
	}

	@Test
	public void defaultFcpClientCanDisablePeerByName() throws Exception {
		Future<Optional<Peer>> peer = client().modifyPeer().disable().byName("id1").execute();
		connectAndAssert(() -> matchesModifyPeer("id1", "IsDisabled", true));
		replyWithPeer("id1");
		assertThat(peer.get().get().getIdentity(), is("id1"));
	}

	@Test
	public void defaultFcpClientCanEnablePeerByIdentity() throws Exception {
		Future<Optional<Peer>> peer = client().modifyPeer().enable().byIdentity("id1").execute();
		connectAndAssert(() -> matchesModifyPeer("id1", "IsDisabled", false));
		replyWithPeer("id1");
		assertThat(peer.get().get().getIdentity(), is("id1"));
	}

	@Test
	public void defaultFcpClientCanEnablePeerByHostAndPort() throws Exception {
		Future<Optional<Peer>> peer = client().modifyPeer().enable().byHostAndPort("1.2.3.4", 5678).execute();
		connectAndAssert(() -> matchesModifyPeer("1.2.3.4:5678", "IsDisabled", false));
		replyWithPeer("id1");
		assertThat(peer.get().get().getIdentity(), is("id1"));
	}

	@Test
	public void allowLocalAddressesOfPeer() throws Exception {
		Future<Optional<Peer>> peer = client().modifyPeer().allowLocalAddresses().byIdentity("id1").execute();
		connectAndAssert(() -> allOf(
				matchesModifyPeer("id1", "AllowLocalAddresses", true),
				not(contains(startsWith("IsDisabled=")))
		));
		replyWithPeer("id1");
		assertThat(peer.get().get().getIdentity(), is("id1"));
	}

	@Test
	public void disallowLocalAddressesOfPeer() throws Exception {
		Future<Optional<Peer>> peer = client().modifyPeer().disallowLocalAddresses().byIdentity("id1").execute();
		connectAndAssert(() -> allOf(
				matchesModifyPeer("id1", "AllowLocalAddresses", false),
				not(contains(startsWith("IsDisabled=")))
		));
		replyWithPeer("id1");
		assertThat(peer.get().get().getIdentity(), is("id1"));
	}

	@Test
	public void setBurstOnlyForPeer() throws Exception {
		Future<Optional<Peer>> peer = client().modifyPeer().setBurstOnly().byIdentity("id1").execute();
		connectAndAssert(() -> allOf(
				matchesModifyPeer("id1", "IsBurstOnly", true),
				not(contains(startsWith("AllowLocalAddresses="))),
				not(contains(startsWith("IsDisabled=")))
		));
		replyWithPeer("id1");
		assertThat(peer.get().get().getIdentity(), is("id1"));
	}

	@Test
	public void clearBurstOnlyForPeer() throws Exception {
		Future<Optional<Peer>> peer = client().modifyPeer().clearBurstOnly().byIdentity("id1").execute();
		connectAndAssert(() -> allOf(
				matchesModifyPeer("id1", "IsBurstOnly", false),
				not(contains(startsWith("AllowLocalAddresses="))),
				not(contains(startsWith("IsDisabled=")))
		));
		replyWithPeer("id1");
		assertThat(peer.get().get().getIdentity(), is("id1"));
	}

	@Test
	public void defaultFcpClientCanSetListenOnlyForPeer() throws Exception {
		Future<Optional<Peer>> peer = client().modifyPeer().setListenOnly().byIdentity("id1").execute();
		connectAndAssert(() -> allOf(
				matchesModifyPeer("id1", "IsListenOnly", true),
				not(contains(startsWith("AllowLocalAddresses="))),
				not(contains(startsWith("IsDisabled="))),
				not(contains(startsWith("IsBurstOnly=")))
		));
		replyWithPeer("id1");
		assertThat(peer.get().get().getIdentity(), is("id1"));
	}

	@Test
	public void clearListenOnlyForPeer() throws Exception {
		Future<Optional<Peer>> peer = client().modifyPeer().clearListenOnly().byIdentity("id1").execute();
		connectAndAssert(() -> allOf(
				matchesModifyPeer("id1", "IsListenOnly", false),
				not(contains(startsWith("AllowLocalAddresses="))),
				not(contains(startsWith("IsDisabled="))),
				not(contains(startsWith("IsBurstOnly=")))
		));
		replyWithPeer("id1");
		assertThat(peer.get().get().getIdentity(), is("id1"));
	}

	@Test
	public void ignoreSourceForPeer() throws Exception {
		Future<Optional<Peer>> peer = client().modifyPeer().ignoreSource().byIdentity("id1").execute();
		connectAndAssert(() -> allOf(
				matchesModifyPeer("id1", "IgnoreSourcePort", true),
				not(contains(startsWith("AllowLocalAddresses="))),
				not(contains(startsWith("IsDisabled="))),
				not(contains(startsWith("IsBurstOnly="))),
				not(contains(startsWith("IsListenOnly=")))
		));
		replyWithPeer("id1");
		assertThat(peer.get().get().getIdentity(), is("id1"));
	}

	@Test
	public void useSourceForPeer() throws Exception {
		Future<Optional<Peer>> peer = client().modifyPeer().useSource().byIdentity("id1").execute();
		connectAndAssert(() -> allOf(
				matchesModifyPeer("id1", "IgnoreSourcePort", false),
				not(contains(startsWith("AllowLocalAddresses="))),
				not(contains(startsWith("IsDisabled="))),
				not(contains(startsWith("IsBurstOnly="))),
				not(contains(startsWith("IsListenOnly=")))
		));
		replyWithPeer("id1");
		assertThat(peer.get().get().getIdentity(), is("id1"));
	}

	@Test
	public void unknownNode() throws Exception {
		Future<Optional<Peer>> peer = client().modifyPeer().enable().byIdentity("id1").execute();
		connectAndAssert(() -> matchesModifyPeer("id1", "IsDisabled", false));
		replyWithUnknownNodeIdentifier();
		assertThat(peer.get().isPresent(), is(false));
	}

	private Matcher<List<String>> matchesModifyPeer(String nodeIdentifier, String setting, boolean value) {
		return matchesFcpMessage(
				"ModifyPeer",
				"Identifier=" + identifier(),
				"NodeIdentifier=" + nodeIdentifier,
				setting + "=" + value
		);
	}

}
