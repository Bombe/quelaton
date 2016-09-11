package net.pterodactylus.fcp.quelaton;

import java.util.List;

import net.pterodactylus.fcp.test.AbstractClientCommandTest;

import org.hamcrest.Matcher;
import org.junit.Test;

/**
 * Unit test for {@link WatchFeedsCommand}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class WatchFeedsCommandTest extends AbstractClientCommandTest {

	@Test
	public void enablingFeedWatchingSendsCorrectCommand() throws Exception {
		client().watchFeeds().enable().execute();
		connectAndAssert(this::matchesWatchFeedsEnableMessage);
	}

	@Test
	public void disablingFeedWatchingSendsCorrectCommand() throws Exception {
		client().watchFeeds().disable().execute();
		connectAndAssert(this::matchesWatchFeedsDisableMessage);
	}

	private Matcher<List<String>> matchesWatchFeedsEnableMessage() {
		return matchesFcpMessage("WatchFeeds", "Enabled=true");
	}

	private Matcher<List<String>> matchesWatchFeedsDisableMessage() {
		return matchesFcpMessage("WatchFeeds", "Enabled=false");
	}

}
