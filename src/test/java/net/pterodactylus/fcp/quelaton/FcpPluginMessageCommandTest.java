package net.pterodactylus.fcp.quelaton;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.Future;

import net.pterodactylus.fcp.test.AbstractClientCommandTest;

import org.hamcrest.Matcher;
import org.junit.Test;

/**
 * Unit test for {@link FcpPluginMessageCommand}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class FcpPluginMessageCommandTest extends AbstractClientCommandTest {

	@Test
	public void defaultClientCanSendMessageToPlugin() throws Exception {
		Future<Void> executed = client().fcpPluginMessage().parameter("key", "value").forPlugin("foo.bar.Plugin").execute();
		connectAndAssert(this::matchesFcpPluginMessage);
		assertThat(executed.get(), nullValue());
	}

	@Test
	public void defaultClientCanSendMessageWithDataToPlugin() throws Exception {
		ByteArrayInputStream inputStream = new ByteArrayInputStream("Hello\n".getBytes());
		Future<Void> executed = client().fcpPluginMessage().parameter("key", "value").withData(inputStream, 6).forPlugin("foo.bar.Plugin").execute();
		connectAndAssert("Hello", this::matchesFcpPluginMessageWithData);
		assertThat(executed.get(), nullValue());
	}

	private Matcher<List<String>> matchesFcpPluginMessage() {
		return matchesFcpMessage("FCPPluginMessage", "Param.key=value");
	}

	private Matcher<List<String>> matchesFcpPluginMessageWithData() {
		return matchesFcpMessageWithTerminator("FCPPluginMessage", "Hello", "DataLength=6", "Data", "Param.key=value");
	}

}
