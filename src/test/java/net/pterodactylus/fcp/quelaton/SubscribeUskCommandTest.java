package net.pterodactylus.fcp.quelaton;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import net.pterodactylus.fcp.test.AbstractClientCommandTest;

import org.junit.Test;

/**
 * Unit test for {@link SubscribeUskCommand}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class SubscribeUskCommandTest extends AbstractClientCommandTest {

	private static final String URI = "USK@some,uri/file.txt";

	@Test
	public void subscriptionWorks() throws Exception {
		Future<Optional<UskSubscription>> uskSubscription = client().subscribeUsk().uri(URI).execute();
		connectAndAssert(() -> matchesFcpMessage("SubscribeUSK", "URI=" + URI));
		replyWithSubscribed();
		assertThat(uskSubscription.get().get().getUri(), is(URI));
		AtomicInteger edition = new AtomicInteger();
		CountDownLatch updated = new CountDownLatch(2);
		uskSubscription.get().get().onUpdate(e -> {
			edition.set(e);
			updated.countDown();
		});
		sendUpdateNotification(23);
		sendUpdateNotification(24);
		assertThat("updated in time", updated.await(5, TimeUnit.SECONDS), is(true));
		assertThat(edition.get(), is(24));
	}

	@Test
	public void subscriptionUpdatesMultipleTimes() throws Exception {
		Future<Optional<UskSubscription>> uskSubscription = client().subscribeUsk().uri(URI).execute();
		connectAndAssert(() -> matchesFcpMessage("SubscribeUSK", "URI=" + URI));
		replyWithSubscribed();
		assertThat(uskSubscription.get().get().getUri(), is(URI));
		AtomicInteger edition = new AtomicInteger();
		CountDownLatch updated = new CountDownLatch(2);
		uskSubscription.get().get().onUpdate(e -> {
			edition.set(e);
			updated.countDown();
		});
		uskSubscription.get().get().onUpdate(e -> updated.countDown());
		sendUpdateNotification(23);
		assertThat("updated in time", updated.await(5, TimeUnit.SECONDS), is(true));
		assertThat(edition.get(), is(23));
	}

	@Test
	public void subscriptionCanBeCancelled() throws Exception {
		Future<Optional<UskSubscription>> uskSubscription = client().subscribeUsk().uri(URI).execute();
		connectAndAssert(() -> matchesFcpMessage("SubscribeUSK", "URI=" + URI));
		String originalIdentifier = identifier();
		replyWithSubscribed();
		assertThat(uskSubscription.get().get().getUri(), is(URI));
		AtomicBoolean updated = new AtomicBoolean();
		uskSubscription.get().get().onUpdate(e -> updated.set(true));
		uskSubscription.get().get().cancel();
		readMessage(() -> matchesFcpMessage("UnsubscribeUSK", "Identifier=" + originalIdentifier));
		sendUpdateNotification(23);
		assertThat(updated.get(), is(false));
	}

	private void replyWithSubscribed() throws Exception {
		answer(
				"SubscribedUSK",
				"Identifier=" + identifier(),
				"URI=" + URI,
				"DontPoll=false",
				"EndMessage"
		);
	}

	private void sendUpdateNotification(int edition, String... additionalLines) throws Exception {
		answer(
				"SubscribedUSKUpdate",
				"Identifier=" + identifier(),
				"URI=" + URI,
				"Edition=" + edition
		);
		answer(additionalLines);
		answer("EndMessage");
	}


}
