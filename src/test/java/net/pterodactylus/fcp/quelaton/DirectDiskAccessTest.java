package net.pterodactylus.fcp.quelaton;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import net.pterodactylus.fcp.test.AbstractClientCommandTest;

import com.google.common.io.Files;
import org.hamcrest.Matcher;
import org.junit.Test;

/**
 * Unit test for DDA-related commands.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class DirectDiskAccessTest extends AbstractClientCommandTest {

	private final File ddaFile;
	private final File fileToUpload;

	public DirectDiskAccessTest() throws Exception {
		ddaFile = createDdaFile();
		fileToUpload = new File(ddaFile.getParent(), "test.dat");
	}

	private File createDdaFile() throws Exception {
		File tempFile = File.createTempFile("test-dda-", ".dat");
		tempFile.deleteOnExit();
		Files.write("test-content", tempFile, StandardCharsets.UTF_8);
		return tempFile;
	}

	private Matcher<List<String>> matchesFileClientPut(File file) {
		return matchesFcpMessage("ClientPut", "UploadFrom=disk", "URI=KSK@foo.txt", "Filename=" + file);
	}

	@Test
	public void completeDda() throws Exception {
		client().clientPut().from(fileToUpload).uri("KSK@foo.txt").execute();
		connectAndAssert(() -> matchesFileClientPut(fileToUpload));
		sendDdaRequired(identifier());
		readMessage(() -> matchesTestDDARequest(ddaFile));
		sendTestDDAReply(ddaFile.getParent(), ddaFile);
		readMessage(() -> matchesTestDDAResponse(ddaFile));
		writeTestDDAComplete(ddaFile);
		readMessage(() -> matchesFileClientPut(fileToUpload));
	}

	@Test
	public void ignoreOtherDda() throws Exception {
		client().clientPut().from(fileToUpload).uri("KSK@foo.txt").execute();
		connectAndAssert(() -> matchesFileClientPut(fileToUpload));
		sendDdaRequired(identifier());
		readMessage(() -> matchesTestDDARequest(ddaFile));
		sendTestDDAReply("/some-other-directory", ddaFile);
		sendTestDDAReply(ddaFile.getParent(), ddaFile);
		readMessage(() -> matchesTestDDAResponse(ddaFile));
	}

	@Test
	public void sendResponseIfFileUnreadable() throws Exception {
		client().clientPut().from(fileToUpload).uri("KSK@foo.txt").execute();
		connectAndAssert(() -> matchesFileClientPut(fileToUpload));
		sendDdaRequired(identifier());
		readMessage(() -> matchesTestDDARequest(ddaFile));
		sendTestDDAReply(ddaFile.getParent(), new File(ddaFile + ".foo"));
		readMessage(this::matchesFailedToReadResponse);
	}

	@Test
	public void clientPutDoesNotResendOriginalClientPutOnTestDDACompleteWithWrongDirectory() throws Exception {
		client().clientPut().from(fileToUpload).uri("KSK@foo.txt").execute();
		connectNode();
		List<String> lines = collectUntil(is("EndMessage"));
		String identifier = extractIdentifier(lines);
		answer(
				"TestDDAComplete",
				"Directory=/some-other-directory",
				"EndMessage"
		);
		sendDdaRequired(identifier);
		lines = collectUntil(is("EndMessage"));
		assertThat(lines, matchesFcpMessage(
				"TestDDARequest",
				"Directory=" + ddaFile.getParent(),
				"WantReadDirectory=true",
				"WantWriteDirectory=false"
		));
	}

	private Matcher<List<String>> matchesFailedToReadResponse() {
		return matchesFcpMessage(
				"TestDDAResponse",
				"Directory=" + ddaFile.getParent(),
				"ReadContent=failed-to-read"
		);
	}

	private void writeTestDDAComplete(File tempFile) throws Exception {
		answer(
				"TestDDAComplete",
				"Directory=" + tempFile.getParent(),
				"ReadDirectoryAllowed=true",
				"EndMessage"
		);
	}

	private Matcher<List<String>> matchesTestDDAResponse(File tempFile) {
		return matchesFcpMessage(
				"TestDDAResponse",
				"Directory=" + tempFile.getParent(),
				"ReadContent=test-content"
		);
	}

	private void sendTestDDAReply(String directory, File tempFile) throws Exception {
		answer(
				"TestDDAReply",
				"Directory=" + directory,
				"ReadFilename=" + tempFile,
				"EndMessage"
		);
	}

	private Matcher<List<String>> matchesTestDDARequest(File tempFile) {
		return matchesFcpMessage(
				"TestDDARequest",
				"Directory=" + tempFile.getParent(),
				"WantReadDirectory=true",
				"WantWriteDirectory=false"
		);
	}

	private void sendDdaRequired(String identifier) throws IOException {
		answer(
				"ProtocolError",
				"Identifier=" + identifier,
				"Code=25",
				"EndMessage"
		);
	}

}
