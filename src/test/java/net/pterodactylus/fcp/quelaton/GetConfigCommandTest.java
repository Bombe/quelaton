package net.pterodactylus.fcp.quelaton;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;
import java.util.concurrent.Future;

import net.pterodactylus.fcp.ConfigData;
import net.pterodactylus.fcp.test.AbstractConfigCommandTest;

import org.hamcrest.Matcher;
import org.junit.Test;

/**
 * Unit test for {@link GetConfigCommand}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class GetConfigCommandTest extends AbstractConfigCommandTest {

	@Test
	public void defaultFcpClientCanGetConfigWithoutDetails() throws Exception {
		Future<ConfigData> configData = client().getConfig().execute();
		connectAndAssert(() -> matchesFcpMessage("GetConfig", "Identifier=" + identifier()));
		replyWithConfigData();
		assertThat(configData.get(), notNullValue());
	}

	@Test
	public void defaultFcpClientCanGetConfigWithCurrent() throws Exception {
		Future<ConfigData> configData = client().getConfig().withCurrent().execute();
		connectAndAssert(() -> matchesGetConfigWithAdditionalParameter("WithCurrent"));
		replyWithConfigData("current.foo=bar");
		assertThat(configData.get().getCurrent("foo"), is("bar"));
	}

	@Test
	public void defaultFcpClientCanGetConfigWithDefaults() throws Exception {
		Future<ConfigData> configData = client().getConfig().withDefaults().execute();
		connectAndAssert(() -> matchesGetConfigWithAdditionalParameter("WithDefaults"));
		replyWithConfigData("default.foo=bar");
		assertThat(configData.get().getDefault("foo"), is("bar"));
	}

	@Test
	public void defaultFcpClientCanGetConfigWithSortOrder() throws Exception {
		Future<ConfigData> configData = client().getConfig().withSortOrder().execute();
		connectAndAssert(() -> matchesGetConfigWithAdditionalParameter("WithSortOrder"));
		replyWithConfigData("sortOrder.foo=17");
		assertThat(configData.get().getSortOrder("foo"), is(17));
	}

	@Test
	public void defaultFcpClientCanGetConfigWithExpertFlag() throws Exception {
		Future<ConfigData> configData = client().getConfig().withExpertFlag().execute();
		connectAndAssert(() -> matchesGetConfigWithAdditionalParameter("WithExpertFlag"));
		replyWithConfigData("expertFlag.foo=true");
		assertThat(configData.get().getExpertFlag("foo"), is(true));
	}

	@Test
	public void defaultFcpClientCanGetConfigWithForceWriteFlag() throws Exception {
		Future<ConfigData> configData = client().getConfig().withForceWriteFlag().execute();
		connectAndAssert(() -> matchesGetConfigWithAdditionalParameter("WithForceWriteFlag"));
		replyWithConfigData("forceWriteFlag.foo=true");
		assertThat(configData.get().getForceWriteFlag("foo"), is(true));
	}

	@Test
	public void defaultFcpClientCanGetConfigWithShortDescription() throws Exception {
		Future<ConfigData> configData = client().getConfig().withShortDescription().execute();
		connectAndAssert(() -> matchesGetConfigWithAdditionalParameter("WithShortDescription"));
		replyWithConfigData("shortDescription.foo=bar");
		assertThat(configData.get().getShortDescription("foo"), is("bar"));
	}

	@Test
	public void defaultFcpClientCanGetConfigWithLongDescription() throws Exception {
		Future<ConfigData> configData = client().getConfig().withLongDescription().execute();
		connectAndAssert(() -> matchesGetConfigWithAdditionalParameter("WithLongDescription"));
		replyWithConfigData("longDescription.foo=bar");
		assertThat(configData.get().getLongDescription("foo"), is("bar"));
	}

	@Test
	public void defaultFcpClientCanGetConfigWithDataTypes() throws Exception {
		Future<ConfigData> configData = client().getConfig().withDataTypes().execute();
		connectAndAssert(() -> matchesGetConfigWithAdditionalParameter("WithDataTypes"));
		replyWithConfigData("dataType.foo=number");
		assertThat(configData.get().getDataType("foo"), is("number"));
	}

	private Matcher<List<String>> matchesGetConfigWithAdditionalParameter(String additionalParameter) {
		return matchesFcpMessage(
				"GetConfig",
				"Identifier=" + identifier(),
				additionalParameter + "=true"
		);
	}

}
