package net.pterodactylus.fcp.quelaton;

import java.io.InputStream;

/**
 * Command to send messages to other plugins.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public interface FcpPluginMessageCommand {

	FcpPluginMessageCommand parameter(String name, String value);
	ForPlugin withData(InputStream dataInputStream, long dataLength);
	Executable<Void> forPlugin(String pluginClass);

	interface ForPlugin {

		Executable<Void> forPlugin(String pluginClass);

	}

}
