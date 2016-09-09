package net.pterodactylus.fcp.test;

import java.io.IOException;

/**
 * Abstract base class for tests of the “*PeerNotes” command family.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class AbstractPeerNotesCommandTest extends AbstractPeerCommandTest {

	protected void replyWithPeerNote(String nodeIdentifier, String noteText) throws IOException {
		answer(
				"PeerNote",
				"Identifier=" + identifier(),
				"NodeIdentifier=" + nodeIdentifier,
				"NoteText=" + noteText,
				"PeerNoteType=1",
				"EndMessage"
		);
	}

}
