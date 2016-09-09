package net.pterodactylus.fcp.quelaton;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.pterodactylus.fcp.PeerNote;
import net.pterodactylus.fcp.test.AbstractPeerCommandTest;

import org.hamcrest.Matcher;
import org.junit.Test;

/**
 * Unit test for {@link ListPeerNotesCommand}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class ListPeerNotesCommandTest extends AbstractPeerCommandTest {

	@Test
	public void onUnknownNodeIdentifier() throws InterruptedException, ExecutionException, IOException {
		Future<Optional<PeerNote>> peerNote = client().listPeerNotes().byName("Friend1").execute();
		connectAndAssert(() -> matchesListPeerNotes("Friend1"));
		replyWithUnknownNodeIdentifier();
		assertThat(peerNote.get().isPresent(), is(false));
	}

	@Test
	public void byNodeName() throws InterruptedException, ExecutionException, IOException {
		Future<Optional<PeerNote>> peerNote = client().listPeerNotes().byName("Friend1").execute();
		connectAndAssert(() -> matchesListPeerNotes("Friend1"));
		replyWithPeerNote();
		replyWithEndListPeerNotes();
		assertThat(peerNote.get().get().getNoteText(), is("Example Text."));
		assertThat(peerNote.get().get().getPeerNoteType(), is(1));
	}

	@Test
	public void byNodeIdentifier() throws InterruptedException, ExecutionException, IOException {
		Future<Optional<PeerNote>> peerNote = client().listPeerNotes().byIdentity("id1").execute();
		connectAndAssert(() -> matchesListPeerNotes("id1"));
		replyWithPeerNote();
		replyWithEndListPeerNotes();
		assertThat(peerNote.get().get().getNoteText(), is("Example Text."));
		assertThat(peerNote.get().get().getPeerNoteType(), is(1));
	}

	@Test
	public void byHostAndPort() throws InterruptedException, ExecutionException, IOException {
		Future<Optional<PeerNote>> peerNote = client().listPeerNotes().byHostAndPort("1.2.3.4", 5678).execute();
		connectAndAssert(() -> matchesListPeerNotes("1.2.3.4:5678"));
		replyWithPeerNote();
		replyWithEndListPeerNotes();
		assertThat(peerNote.get().get().getNoteText(), is("Example Text."));
		assertThat(peerNote.get().get().getPeerNoteType(), is(1));
	}

	private Matcher<List<String>> matchesListPeerNotes(String nodeIdentifier) {
		return matchesFcpMessage(
				"ListPeerNotes",
				"NodeIdentifier=" + nodeIdentifier
		);
	}

	private void replyWithEndListPeerNotes() throws IOException {
		answer(
				"EndListPeerNotes",
				"Identifier=" + identifier(),
				"EndMessage"
		);
	}

	private void replyWithPeerNote() throws IOException {
		answer(
				"PeerNote",
				"Identifier=" + identifier(),
				"NodeIdentifier=Friend1",
				"NoteText=RXhhbXBsZSBUZXh0Lg==",
				"PeerNoteType=1",
				"EndMessage"
		);
	}

}
