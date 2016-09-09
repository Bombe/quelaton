package net.pterodactylus.fcp.quelaton;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.pterodactylus.fcp.test.AbstractPeerNotesCommandTest;

import org.hamcrest.Matcher;
import org.junit.Test;

/**
 * Unit test for {@link ModifyPeerNoteCommand}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class ModifyPeerNoteCommandTest extends AbstractPeerNotesCommandTest {

	@Test
	public void byName() throws InterruptedException, ExecutionException, IOException {
		Future<Boolean> noteUpdated = client().modifyPeerNote().darknetComment("foo").byName("Friend1").execute();
		connectAndAssert(() -> matchesModifyPeerNote("Friend1"));
		replyWithPeerNote("Friend1", "Zm9v");
		assertThat(noteUpdated.get(), is(true));
	}

	@Test
	public void onUnknownNodeIdentifier() throws InterruptedException, ExecutionException, IOException {
		Future<Boolean> noteUpdated = client().modifyPeerNote().darknetComment("foo").byName("Friend1").execute();
		connectAndAssert(() -> matchesModifyPeerNote("Friend1"));
		replyWithUnknownNodeIdentifier();
		assertThat(noteUpdated.get(), is(false));
	}

	@Test
	public void defaultFcpClientFailsToModifyPeerNoteWithoutPeerNote() throws Exception {
		Future<Boolean> noteUpdated = client().modifyPeerNote().byName("Friend1").execute();
		assertThat(noteUpdated.get(), is(false));
	}

	@Test
	public void byIdentifier() throws InterruptedException, ExecutionException, IOException {
		Future<Boolean> noteUpdated = client().modifyPeerNote().darknetComment("foo").byIdentifier("id1").execute();
		connectAndAssert(() -> matchesModifyPeerNote("id1"));
		replyWithPeerNote("Friend1", "Zm9v");
		assertThat(noteUpdated.get(), is(true));
	}

	@Test
	public void byHostAndPort() throws InterruptedException, ExecutionException, IOException {
		Future<Boolean> noteUpdated = client().modifyPeerNote().darknetComment("foo").byHostAndPort("1.2.3.4", 5678).execute();
		connectAndAssert(() -> matchesModifyPeerNote("1.2.3.4:5678"));
		replyWithPeerNote("Friend1", "Zm9v");
		assertThat(noteUpdated.get(), is(true));
	}

	private Matcher<List<String>> matchesModifyPeerNote(String nodeIdentifier) {
		return matchesFcpMessage(
				"ModifyPeerNote",
				"Identifier=" + identifier(),
				"NodeIdentifier=" + nodeIdentifier,
				"PeerNoteType=1",
				"NoteText=Zm9v"
		);
	}

}
