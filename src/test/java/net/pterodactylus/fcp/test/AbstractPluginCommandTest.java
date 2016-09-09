package net.pterodactylus.fcp.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Optional;
import java.util.concurrent.Future;

import net.pterodactylus.fcp.PluginInfo;

/**
 * Base test for all plugin-related command tests.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class AbstractPluginCommandTest extends AbstractClientCommandTest {

	protected static final String CLASS_NAME = "foo.plugin.Plugin";

	protected void replyWithPluginInfo() throws Exception {
		answer(
				"PluginInfo",
				"Identifier=" + identifier(),
				"PluginName=superPlugin",
				"IsTalkable=true",
				"LongVersion=1.2.3",
				"Version=42",
				"OriginUri=superPlugin",
				"Started=true",
				"EndMessage"
		);
	}

	protected void verifyPluginInfo(Future<Optional<PluginInfo>> pluginInfo) throws Exception {
		assertThat(pluginInfo.get().get().getPluginName(), is("superPlugin"));
		assertThat(pluginInfo.get().get().getOriginalURI(), is("superPlugin"));
		assertThat(pluginInfo.get().get().isTalkable(), is(true));
		assertThat(pluginInfo.get().get().getVersion(), is("42"));
		assertThat(pluginInfo.get().get().getLongVersion(), is("1.2.3"));
		assertThat(pluginInfo.get().get().isStarted(), is(true));
	}

}
