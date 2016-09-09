package net.pterodactylus.fcp.quelaton;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Future;

import net.pterodactylus.fcp.test.AbstractPluginCommandTest;

import org.hamcrest.Matcher;
import org.junit.Test;

/**
 * Unit test for {@link RemovePluginCommand}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class RemovePluginCommandTest extends AbstractPluginCommandTest {

	@Test
	public void removingPluginWorks() throws Exception {
		Future<Boolean> pluginRemoved = client().removePlugin().plugin(CLASS_NAME).execute();
		connectAndAssert(this::matchPluginRemovedMessage);
		replyWithPluginRemoved();
		assertThat(pluginRemoved.get(), is(true));
	}

	@Test
	public void removingPluginWithMaxWaitTimeWorks() throws Exception {
		Future<Boolean> pluginRemoved = client().removePlugin().waitFor(1234).plugin(CLASS_NAME).execute();
		connectAndAssert(() -> allOf(matchPluginRemovedMessage(), hasItem("MaxWaitTime=1234")));
		replyWithPluginRemoved();
		assertThat(pluginRemoved.get(), is(true));
	}

	@Test
	public void removingPluginWithPurgeWorks() throws Exception {
		Future<Boolean> pluginRemoved = client().removePlugin().purge().plugin(CLASS_NAME).execute();
		connectAndAssert(() -> allOf(matchPluginRemovedMessage(), hasItem("Purge=true")));
		replyWithPluginRemoved();
		assertThat(pluginRemoved.get(), is(true));
	}

	private void replyWithPluginRemoved() throws IOException {
		answer(
				"PluginRemoved",
				"Identifier=" + identifier(),
				"PluginName=" + CLASS_NAME,
				"EndMessage"
		);
	}

	private Matcher<List<String>> matchPluginRemovedMessage() {
		return matchesFcpMessage(
				"RemovePlugin",
				"Identifier=" + identifier(),
				"PluginName=" + CLASS_NAME
		);
	}

}
