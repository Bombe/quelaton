package net.pterodactylus.fcp.test;

/**
 * Base test for config-related command tests.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class AbstractConfigCommandTest extends AbstractClientCommandTest {

	protected void replyWithConfigData(String... additionalLines) throws Exception {
		answer("ConfigData", "Identifier=" + identifier());
		answer(additionalLines);
		answer("EndMessage");
	}

}
