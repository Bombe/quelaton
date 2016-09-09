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
 * Unit test for {@link GetPluginInfoCommand}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class GetPluginInfoCommandTest extends AbstractPluginCommandTest {

	@Test
	public void gettingPluginInfoWorks() throws Exception {
		Future<Optional<PluginInfo>> pluginInfo = client().getPluginInfo().plugin(CLASS_NAME).execute();
		connectAndAssert(this::matchGetPluginInfoMessage);
		replyWithPluginInfo();
		verifyPluginInfo(pluginInfo);
	}

	@Test
	public void gettingPluginInfoWithDetailsWorks() throws Exception {
		Future<Optional<PluginInfo>> pluginInfo = client().getPluginInfo().detailed().plugin(CLASS_NAME).execute();
		connectAndAssert(() -> allOf(matchGetPluginInfoMessage(), hasItem("Detailed=true")));
		replyWithPluginInfo();
		verifyPluginInfo(pluginInfo);
	}

	@Test
	public void protocolErrorIsRecognizedAsFailure() throws Exception {
		Future<Optional<PluginInfo>> pluginInfo = client().getPluginInfo().detailed().plugin(CLASS_NAME).execute();
		connectAndAssert(() -> allOf(matchGetPluginInfoMessage(), hasItem("Detailed=true")));
		replyWithProtocolError();
		assertThat(pluginInfo.get(), is(Optional.empty()));
	}

	private Matcher<List<String>> matchGetPluginInfoMessage() {
		return matchesFcpMessage(
				"GetPluginInfo",
				"Identifier=" + identifier(),
				"PluginName=" + CLASS_NAME
		);
	}

}
