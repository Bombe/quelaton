package net.pterodactylus.fcp.test;

/**
 * Base test for all “ClientPut”-related command tests.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class AbstractClientPutCommandTest extends AbstractClientCommandTest {

	protected void replyWithPutSuccessful(String identifier) throws Exception {
		answer(
				"PutSuccessful",
				"URI=KSK@foo.txt",
				"Identifier=" + identifier,
				"EndMessage"
		);
	}

	protected void replyWithGeneratedUri() throws Exception {
		answer(
				"URIGenerated",
				"Identifier=" + identifier(),
				"URI=KSK@foo.txt",
				"EndMessage"
		);
	}

	protected void replyWithSimpleProgress(
			int total, int required, int failed, int fatallyFailed, int succeeded, int lastProgress,
			boolean finalizedTotal, int minSuccessFetchBlocks) throws Exception {
		answer(
				"SimpleProgress",
				"Identifier=" + identifier(),
				"Total=" + total,
				"Required=" + required,
				"Failed=" + failed,
				"FatallyFailed=" + fatallyFailed,
				"Succeeded=" + succeeded,
				"LastProgress=" + lastProgress,
				"FinalizedTotal=" + finalizedTotal,
				"MinSuccessFetchBlocks=" + minSuccessFetchBlocks,
				"EndMessage"
		);
	}

}
