package net.pterodactylus.fcp.quelaton;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.concurrent.Future;

import net.pterodactylus.fcp.FcpKeyPair;
import net.pterodactylus.fcp.test.AbstractClientCommandTest;

import org.junit.Test;

/**
 * Unit test for {@link GenerateKeypairCommand}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class GenerateKeypairCommandTest extends AbstractClientCommandTest {

	private static final String INSERT_URI = "SSK@RVCHbJdkkyTCeNN9AYukEg76eyqmiosSaNKgE3U9zUw,7SHH53gletBVb9JD7nBsyClbLQsBubDPEIcwg908r7Y,AQECAAE/";
	private static final String REQUEST_URI = "SSK@wtbgd2loNcJCXvtQVOftl2tuWBomDQHfqS6ytpPRhfw,7SHH53gletBVb9JD7nBsyClbLQsBubDPEIcwg908r7Y,AQACAAE/";

	@Test
	public void defaultFcpClientCanGenerateKeypair() throws Exception {
		Future<FcpKeyPair> keyPairFuture = fcp.client().generateKeypair().execute();
		fcp.connectAndAssert(() -> fcp.matchesFcpMessage("GenerateSSK"));
		replyWithKeyPair();
		FcpKeyPair keyPair = keyPairFuture.get();
		assertThat(keyPair.getPublicKey(), is(REQUEST_URI));
		assertThat(keyPair.getPrivateKey(), is(INSERT_URI));
	}

	private void replyWithKeyPair() throws IOException {
		fcp.answer("SSKKeypair",
				"InsertURI=" + INSERT_URI + "",
				"RequestURI=" + REQUEST_URI + "",
				"Identifier=" + fcp.identifier(),
				"EndMessage");
	}

}
