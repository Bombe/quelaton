package net.pterodactylus.fcp.quelaton;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

import net.pterodactylus.fcp.PluginInfo;
import net.pterodactylus.fcp.test.AbstractPluginCommandTest;

import org.hamcrest.Matcher;
import org.junit.Test;

/**
 * Unit test for {@link LoadPluginCommand}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class LoadPluginCommandTest extends AbstractPluginCommandTest {

	private static final String FILE_PATH = "/path/to/plugin.jar";
	private static final String URL = "http://server.com/plugin.jar";
	private static final String KEY = "KSK@plugin.jar";

	@Test
	public void loadOfficialPluginFromFreenet() throws Exception {
		Future<Optional<PluginInfo>> pluginInfo = client().loadPlugin().officialFromFreenet("superPlugin").execute();
		connectAndAssert(() -> createMatcherForOfficialSource("freenet"));
		assertThat(lines(), not(contains(startsWith("Store="))));
		replyWithPluginInfo();
		verifyPluginInfo(pluginInfo);
	}

	@Test
	public void persistentFromFreenet() throws Exception {
		Future<Optional<PluginInfo>> pluginInfo = client().loadPlugin().addToConfig().officialFromFreenet("superPlugin").execute();
		connectAndAssert(() -> createMatcherForOfficialSource("freenet"));
		assertThat(lines(), hasItem("Store=true"));
		replyWithPluginInfo();
		verifyPluginInfo(pluginInfo);
	}

	@Test
	public void fromHttps() throws Exception {
		Future<Optional<PluginInfo>> pluginInfo = client().loadPlugin().officialFromHttps("superPlugin").execute();
		connectAndAssert(() -> createMatcherForOfficialSource("https"));
		replyWithPluginInfo();
		verifyPluginInfo(pluginInfo);
	}

	@Test
	public void fromFile() throws Exception {
		Future<Optional<PluginInfo>> pluginInfo = client().loadPlugin().fromFile(FILE_PATH).execute();
		connectAndAssert(() -> createMatcher("file", FILE_PATH));
		replyWithPluginInfo();
		verifyPluginInfo(pluginInfo);
	}

	@Test
	public void fromUrl() throws Exception {
		Future<Optional<PluginInfo>> pluginInfo = client().loadPlugin().fromUrl(URL).execute();
		connectAndAssert(() -> createMatcher("url", URL));
		replyWithPluginInfo();
		verifyPluginInfo(pluginInfo);
	}

	@Test
	public void loadCustomPluginFromFreenet() throws Exception {
		Future<Optional<PluginInfo>> pluginInfo = client().loadPlugin().fromFreenet(KEY).execute();
		connectAndAssert(() -> createMatcher("freenet", KEY));
		replyWithPluginInfo();
		verifyPluginInfo(pluginInfo);
	}

	@Test
	public void failedLoad() throws Exception {
		Future<Optional<PluginInfo>> pluginInfo = client().loadPlugin().officialFromFreenet("superPlugin").execute();
		connectAndAssert(() -> matchesFcpMessage("LoadPlugin"));
		replyWithProtocolError();
		assertThat(pluginInfo.get().isPresent(), is(false));
	}

	private Matcher<List<String>> createMatcher(String urlType, String url) {
		return matchesFcpMessage(
				"LoadPlugin",
				"Identifier=" + identifier(),
				"PluginURL=" + url,
				"URLType=" + urlType
		);
	}

	private Matcher<List<String>> createMatcherForOfficialSource(String officialSource) {
		return matchesFcpMessage(
				"LoadPlugin",
				"Identifier=" + identifier(),
				"PluginURL=superPlugin",
				"URLType=official",
				"OfficialSource=" + officialSource
		);
	}

}
