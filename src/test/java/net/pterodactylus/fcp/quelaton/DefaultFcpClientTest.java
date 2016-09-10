package net.pterodactylus.fcp.quelaton;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.pterodactylus.fcp.FcpKeyPair;
import net.pterodactylus.fcp.test.AbstractClientCommandTest;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link DefaultFcpClient}.
 *
 * @author <a href="bombe@freenetproject.org">David ‘Bombe’ Roden</a>
 */
public class DefaultFcpClientTest extends AbstractClientCommandTest {

	private static final String INSERT_URI =
			"SSK@RVCHbJdkkyTCeNN9AYukEg76eyqmiosSaNKgE3U9zUw,7SHH53gletBVb9JD7nBsyClbLQsBubDPEIcwg908r7Y,AQECAAE/";
	private static final String REQUEST_URI =
			"SSK@wtbgd2loNcJCXvtQVOftl2tuWBomDQHfqS6ytpPRhfw,7SHH53gletBVb9JD7nBsyClbLQsBubDPEIcwg908r7Y,AQACAAE/";

	@Test(expected = ExecutionException.class)
	public void throwsExceptionOnFailure() throws Exception {
		Future<FcpKeyPair> keyPairFuture = client().generateKeypair().execute();
		connectAndAssert(() -> matchesFcpMessage("GenerateSSK"));
		answer(
				"CloseConnectionDuplicateClientName",
				"EndMessage"
		);
		keyPairFuture.get();
	}

	@Test(expected = ExecutionException.class)
	public void throwsExceptionIfConnectionIsClosed() throws Exception {
		Future<FcpKeyPair> keyPairFuture = client().generateKeypair().execute();
		connectAndAssert(() -> matchesFcpMessage("GenerateSSK"));
		closeFcpServer();
		keyPairFuture.get();
	}

	@Test
	public void connectionIsReused() throws Exception {
		Future<FcpKeyPair> keyPair = client().generateKeypair().execute();
		connectAndAssert(() -> matchesFcpMessage("GenerateSSK"));
		replyWithKeyPair();
		keyPair.get();
		keyPair = client().generateKeypair().execute();
		readMessage(() -> matchesFcpMessage("GenerateSSK"));
		replyWithKeyPair();
		keyPair.get();
	}

	@Test
	public void defaultFcpClientCanReconnectAfterConnectionHasBeenClosed() throws Exception {
		Future<FcpKeyPair> keyPair = client().generateKeypair().execute();
		connectAndAssert(() -> matchesFcpMessage("GenerateSSK"));
		closeFcpServer();
		try {
			keyPair.get();
			Assert.fail();
		} catch (ExecutionException e) {
			/* ignore. */
		}
		keyPair = client().generateKeypair().execute();
		connectAndAssert(() -> matchesFcpMessage("GenerateSSK"));
		replyWithKeyPair();
		keyPair.get();
	}

	private void replyWithKeyPair() throws IOException {
		answer("SSKKeypair",
				"InsertURI=" + INSERT_URI + "",
				"RequestURI=" + REQUEST_URI + "",
				"Identifier=" + identifier(),
				"EndMessage");
	}

}
