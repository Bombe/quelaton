package net.pterodactylus.fcp.quelaton;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import net.pterodactylus.fcp.WatchFeeds;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * Default {@link WatchFeedsCommand} implementation based on {@link FcpDialog}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class WatchFeedsCommandImpl implements WatchFeedsCommand {

	private final ListeningExecutorService threadPool;
	private final ConnectionSupplier connectionSupplier;

	public WatchFeedsCommandImpl(ExecutorService threadPool, ConnectionSupplier connectionSupplier) {
		this.threadPool = MoreExecutors.listeningDecorator(threadPool);
		this.connectionSupplier = connectionSupplier;
	}

	@Override
	public Executable<Void> enable() {
		return () -> threadPool.submit(() -> execute(true));
	}

	@Override
	public Executable<Void> disable() {
		return () -> threadPool.submit(() -> execute(false));
	}

	private Void execute(boolean enabled) throws IOException, ExecutionException, InterruptedException {
		WatchFeeds watchFeeds = new WatchFeeds(enabled);
		try (WatchFeedsDialog watchFeedsDialog = new WatchFeedsDialog()) {
			return watchFeedsDialog.send(watchFeeds).get();
		}
	}

	private class WatchFeedsDialog extends FcpDialog<Void> {

		public WatchFeedsDialog() throws IOException {
			super(threadPool, connectionSupplier.get(), null);
			finish();
		}

	}

}
