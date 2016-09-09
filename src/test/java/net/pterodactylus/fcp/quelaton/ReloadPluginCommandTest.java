package net.pterodactylus.fcp.quelaton;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

import net.pterodactylus.fcp.PluginInfo;
import net.pterodactylus.fcp.test.AbstractPluginCommandTest;

import org.hamcrest.Matcher;
import org.junit.Test;

/**
 * Unit test for {@link ReloadPluginCommand}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class ReloadPluginCommandTest extends AbstractPluginCommandTest {

	private static final String CLASS_NAME = "foo.plugin.Plugin";

	@Test
	public void reloadingPluginWorks() throws Exception {
		Future<Optional<PluginInfo>> pluginInfo = client().reloadPlugin().plugin(CLASS_NAME).execute();
		connectAndAssert(this::matchReloadPluginMessage);
		replyWithPluginInfo();
		verifyPluginInfo(pluginInfo);
	}

	@Test
	public void reloadingPluginWithMaxWaitTimeWorks() throws Exception {
		Future<Optional<PluginInfo>> pluginInfo = client().reloadPlugin().waitFor(1234).plugin(CLASS_NAME).execute();
		connectAndAssert(() -> allOf(matchReloadPluginMessage(), hasItem("MaxWaitTime=1234")));
		replyWithPluginInfo();
		verifyPluginInfo(pluginInfo);
	}

	@Test
	public void reloadingPluginWithPurgeWorks() throws Exception {
		Future<Optional<PluginInfo>> pluginInfo = client().reloadPlugin().purge().plugin(CLASS_NAME).execute();
		connectAndAssert(() -> allOf(matchReloadPluginMessage(), hasItem("Purge=true")));
		replyWithPluginInfo();
		verifyPluginInfo(pluginInfo);
	}

	@Test
	public void reloadingPluginWithStoreWorks() throws Exception {
		Future<Optional<PluginInfo>> pluginInfo = client().reloadPlugin().addToConfig().plugin(CLASS_NAME).execute();
		connectAndAssert(() -> allOf(matchReloadPluginMessage(), hasItem("Store=true")));
		replyWithPluginInfo();
		verifyPluginInfo(pluginInfo);
	}

	@Test
	public void protocolErrorIsRecognizedAsFailure() throws Exception {
		Future<Optional<PluginInfo>> pluginInfo = client().reloadPlugin().plugin(CLASS_NAME).execute();
		connectAndAssert(this::matchReloadPluginMessage);
		replyWithProtocolError();
		assertThat(pluginInfo.get().isPresent(), is(false));
	}

	private Matcher<List<String>> matchReloadPluginMessage() {
		return matchesFcpMessage(
				"ReloadPlugin",
				"Identifier=" + identifier(),
				"PluginName=" + CLASS_NAME
		);
	}

}
