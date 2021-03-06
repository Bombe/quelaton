package net.pterodactylus.fcp.test;

import java.io.IOException;

/**
 * Common methods for peer-related command tests.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class AbstractPeerCommandTest extends AbstractClientCommandTest {

	protected void replyWithPeer(String peerId, String... additionalLines) throws IOException {
		answer(
				"Peer",
				"Identifier=" + identifier(),
				"identity=" + peerId,
				"opennet=false",
				"ark.pubURI=SSK@3YEf.../ark",
				"ark.number=78",
				"auth.negTypes=2",
				"version=Fred,0.7,1.0,1466",
				"lastGoodVersion=Fred,0.7,1.0,1466"
		);
		answer(additionalLines);
		answer("EndMessage");
	}

	protected void replyWithUnknownNodeIdentifier() throws IOException {
		answer(
				"UnknownNodeIdentifier",
				"Identifier=" + identifier(),
				"NodeIdentifier=id2",
				"EndMessage"
		);
	}

}
