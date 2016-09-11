package net.pterodactylus.fcp.quelaton;

/**
 * Enables or disables the sending of “Feed” commands alterting the user about alert notifications.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public interface WatchFeedsCommand {

	Executable<Void> enable();
	Executable<Void> disable();

}
