package net.pterodactylus.fcp.test;

import org.junit.Rule;

/**
 * Abstract base class for all client command tests.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class AbstractClientCommandTest {

	@Rule
	public final WithFcp fcp = new WithFcp();

}
