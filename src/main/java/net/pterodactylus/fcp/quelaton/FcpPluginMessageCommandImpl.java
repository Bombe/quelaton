package net.pterodactylus.fcp.quelaton;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import net.pterodactylus.fcp.FCPPluginMessage;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * Default {@link FcpPluginMessageCommand} implementation based on {@link FcpDialog}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class FcpPluginMessageCommandImpl implements FcpPluginMessageCommand {

	private final ListeningExecutorService threadPool;
	private final ConnectionSupplier connectionSupplier;
	private final Supplier<String> identifierGenerator;
	private final AtomicReference<String> pluginClass = new AtomicReference<>();
	private final Map<String, String> parameters = new HashMap<>();
	private final AtomicReference<InputStream> dataInputStream = new AtomicReference<>();
	private final AtomicLong dataLength = new AtomicLong();

	public FcpPluginMessageCommandImpl(ExecutorService threadPool, ConnectionSupplier connectionSupplier, Supplier<String> identifierGenerator) {
		this.threadPool = MoreExecutors.listeningDecorator(threadPool);
		this.connectionSupplier = connectionSupplier;
		this.identifierGenerator = identifierGenerator;
	}

	@Override
	public FcpPluginMessageCommand parameter(String name, String value) {
		parameters.put(name, value);
		return this;
	}

	@Override
	public ForPlugin withData(InputStream dataInputStream, long dataLength) {
		this.dataInputStream.set(Objects.requireNonNull(dataInputStream, "dataInputStream must not be null"));
		this.dataLength.set(dataLength);
		return this::forPlugin;
	}

	@Override
	public Executable<Void> forPlugin(String pluginClass) {
		this.pluginClass.set(Objects.requireNonNull(pluginClass, "pluginClass must not be null"));
		return this::execute;
	}

	private ListenableFuture<Void> execute() {
		return threadPool.submit(this::executeDialog);
	}

	private Void executeDialog() throws IOException, ExecutionException, InterruptedException {
		FCPPluginMessage fcpPluginMessage = new FCPPluginMessage(identifierGenerator.get(), pluginClass.get());
		parameters.forEach(fcpPluginMessage::setParameter);
		Optional.ofNullable(dataInputStream.get()).ifPresent(i -> fcpPluginMessage.setData(i, dataLength.get()));
		try (FcpPluginMessageDialog fcpPluginMessageDialog = new FcpPluginMessageDialog()) {
			return fcpPluginMessageDialog.send(fcpPluginMessage).get();
		}
	}

	private class FcpPluginMessageDialog extends FcpDialog<Void> {

		public FcpPluginMessageDialog() throws IOException {
			super(threadPool, connectionSupplier.get(), null);
			finish();
		}

	}

}
