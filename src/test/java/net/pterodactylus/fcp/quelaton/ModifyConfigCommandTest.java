package net.pterodactylus.fcp.quelaton;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.concurrent.Future;

import net.pterodactylus.fcp.ConfigData;
import net.pterodactylus.fcp.test.AbstractConfigCommandTest;

import org.junit.Test;

/**
 * Unit test for {@link ModifyConfigCommand}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class ModifyConfigCommandTest extends AbstractConfigCommandTest {

	@Test
	public void defaultFcpClientCanModifyConfigData() throws Exception {
		Future<ConfigData> newConfigData = client().modifyConfig().set("foo.bar").to("baz").execute();
		connectAndAssert(() -> matchesFcpMessage(
				"ModifyConfig",
				"Identifier=" + identifier(),
				"foo.bar=baz"
		));
		replyWithConfigData("current.foo.bar=baz");
		assertThat(newConfigData.get().getCurrent("foo.bar"), is("baz"));
	}

}
