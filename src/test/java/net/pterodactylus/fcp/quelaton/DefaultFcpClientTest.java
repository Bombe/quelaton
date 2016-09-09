package net.pterodactylus.fcp.quelaton;

import static net.pterodactylus.fcp.quelaton.RequestProgressMatcher.isRequestProgress;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import net.pterodactylus.fcp.ConfigData;
import net.pterodactylus.fcp.FcpKeyPair;
import net.pterodactylus.fcp.Key;
import net.pterodactylus.fcp.NodeData;
import net.pterodactylus.fcp.PluginInfo;
import net.pterodactylus.fcp.Priority;
import net.pterodactylus.fcp.RequestProgress;
import net.pterodactylus.fcp.fake.FakeTcpServer;
import net.pterodactylus.fcp.quelaton.ClientGetCommand.Data;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.nitorcreations.junit.runners.NestedRunner;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Unit test for {@link DefaultFcpClient}.
 *
 * @author <a href="bombe@freenetproject.org">David ‘Bombe’ Roden</a>
 */
@RunWith(NestedRunner.class)
public class DefaultFcpClientTest {

	private static final String INSERT_URI =
		"SSK@RVCHbJdkkyTCeNN9AYukEg76eyqmiosSaNKgE3U9zUw,7SHH53gletBVb9JD7nBsyClbLQsBubDPEIcwg908r7Y,AQECAAE/";
	private static final String REQUEST_URI =
		"SSK@wtbgd2loNcJCXvtQVOftl2tuWBomDQHfqS6ytpPRhfw,7SHH53gletBVb9JD7nBsyClbLQsBubDPEIcwg908r7Y,AQACAAE/";

	private int threadCounter = 0;
	private final ExecutorService threadPool =
		Executors.newCachedThreadPool(r -> new Thread(r, "Test-Thread-" + threadCounter++));
	private final FakeTcpServer fcpServer;
	private final DefaultFcpClient fcpClient;

	public DefaultFcpClientTest() throws IOException {
		fcpServer = new FakeTcpServer(threadPool);
		fcpClient = new DefaultFcpClient(threadPool, "localhost", fcpServer.getPort(), () -> "Test");
	}

	@After
	public void tearDown() throws IOException {
		fcpServer.close();
		threadPool.shutdown();
	}

	private void connectNode() throws InterruptedException, ExecutionException, IOException {
		fcpServer.connect().get();
		fcpServer.collectUntil(is("EndMessage"));
		fcpServer.writeLine("NodeHello",
			"CompressionCodecs=4 - GZIP(0), BZIP2(1), LZMA(2), LZMA_NEW(3)",
			"Revision=build01466",
			"Testnet=false",
			"Version=Fred,0.7,1.0,1466",
			"Build=1466",
			"ConnectionIdentifier=14318898267048452a81b36e7f13a3f0",
			"Node=Fred",
			"ExtBuild=29",
			"FCPVersion=2.0",
			"NodeLanguage=ENGLISH",
			"ExtRevision=v29",
			"EndMessage"
		);
	}

	private String extractIdentifier(List<String> lines) {
		return lines.stream()
			.filter(s -> s.startsWith("Identifier="))
			.map(s -> s.substring(s.indexOf('=') + 1))
			.findFirst()
			.orElse("");
	}

	private Matcher<List<String>> matchesFcpMessage(String name, String... requiredLines) {
		return matchesFcpMessageWithTerminator(name, "EndMessage", requiredLines);
	}

	private Matcher<Iterable<String>> hasHead(String firstElement) {
		return new TypeSafeDiagnosingMatcher<Iterable<String>>() {
			@Override
			protected boolean matchesSafely(Iterable<String> iterable, Description mismatchDescription) {
				if (!iterable.iterator().hasNext()) {
					mismatchDescription.appendText("is empty");
					return false;
				}
				String element = iterable.iterator().next();
				if (!element.equals(firstElement)) {
					mismatchDescription.appendText("starts with ").appendValue(element);
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("starts with ").appendValue(firstElement);
			}
		};
	}

	private Matcher<List<String>> matchesFcpMessageWithTerminator(
		String name, String terminator, String... requiredLines) {
		return allOf(hasHead(name), hasParameters(1, 1, requiredLines), hasTail(terminator));
	}

	private Matcher<List<String>> hasParameters(int ignoreStart, int ignoreEnd, String... lines) {
		return new TypeSafeDiagnosingMatcher<List<String>>() {
			@Override
			protected boolean matchesSafely(List<String> item, Description mismatchDescription) {
				if (item.size() < (ignoreStart + ignoreEnd)) {
					mismatchDescription.appendText("has only ").appendValue(item.size()).appendText(" elements");
					return false;
				}
				for (String line : lines) {
					if ((item.indexOf(line) < ignoreStart) || (item.indexOf(line) >= (item.size() - ignoreEnd))) {
						mismatchDescription.appendText("does not contains ").appendValue(line);
						return false;
					}
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("contains ").appendValueList("(", ", ", ")", lines);
				description.appendText(", ignoring the first ").appendValue(ignoreStart);
				description.appendText(" and the last ").appendValue(ignoreEnd);
			}
		};
	}

	private Matcher<List<String>> hasTail(String... lastElements) {
		return new TypeSafeDiagnosingMatcher<List<String>>() {
			@Override
			protected boolean matchesSafely(List<String> list, Description mismatchDescription) {
				if (list.size() < lastElements.length) {
					mismatchDescription.appendText("is too small");
					return false;
				}
				List<String> tail = list.subList(list.size() - lastElements.length, list.size());
				if (!tail.equals(Arrays.asList(lastElements))) {
					mismatchDescription.appendText("ends with ").appendValueList("(", ", ", ")", tail);
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("ends with ").appendValueList("(", ", ", ")", lastElements);
			}
		};
	}

	private List<String> lines;
	private String identifier;

	private void connectAndAssert(Supplier<Matcher<List<String>>> requestMatcher)
	throws InterruptedException, ExecutionException, IOException {
		connectNode();
		readMessage(requestMatcher);
	}

	private void readMessage(Supplier<Matcher<List<String>>> requestMatcher) throws IOException {
		readMessage("EndMessage", requestMatcher);
	}

	private void readMessage(String terminator, Supplier<Matcher<List<String>>> requestMatcher) throws IOException {
		lines = fcpServer.collectUntil(is(terminator));
		identifier = extractIdentifier(lines);
		assertThat(lines, requestMatcher.get());
	}

	private void replyWithProtocolError() throws IOException {
		fcpServer.writeLine(
			"ProtocolError",
			"Identifier=" + identifier,
			"EndMessage"
		);
	}

	public class ConnectionsAndKeyPairs {

		public class Connections {

			@Test(expected = ExecutionException.class)
			public void throwsExceptionOnFailure() throws IOException, ExecutionException, InterruptedException {
				Future<FcpKeyPair> keyPairFuture = fcpClient.generateKeypair().execute();
				connectAndAssert(() -> matchesFcpMessage("GenerateSSK"));
				fcpServer.writeLine(
					"CloseConnectionDuplicateClientName",
					"EndMessage"
				);
				keyPairFuture.get();
			}

			@Test(expected = ExecutionException.class)
			public void throwsExceptionIfConnectionIsClosed() throws IOException, ExecutionException, InterruptedException {
				Future<FcpKeyPair> keyPairFuture = fcpClient.generateKeypair().execute();
				connectAndAssert(() -> matchesFcpMessage("GenerateSSK"));
				fcpServer.close();
				keyPairFuture.get();
			}

			@Test
			public void connectionIsReused() throws InterruptedException, ExecutionException, IOException {
				Future<FcpKeyPair> keyPair = fcpClient.generateKeypair().execute();
				connectAndAssert(() -> matchesFcpMessage("GenerateSSK"));
				replyWithKeyPair();
				keyPair.get();
				keyPair = fcpClient.generateKeypair().execute();
				readMessage(() -> matchesFcpMessage("GenerateSSK"));
				identifier = extractIdentifier(lines);
				replyWithKeyPair();
				keyPair.get();
			}

			@Test
			public void defaultFcpClientCanReconnectAfterConnectionHasBeenClosed()
			throws InterruptedException, ExecutionException, IOException {
				Future<FcpKeyPair> keyPair = fcpClient.generateKeypair().execute();
				connectAndAssert(() -> matchesFcpMessage("GenerateSSK"));
				fcpServer.close();
				try {
					keyPair.get();
					Assert.fail();
				} catch (ExecutionException e) {
					/* ignore. */
				}
				keyPair = fcpClient.generateKeypair().execute();
				connectAndAssert(() -> matchesFcpMessage("GenerateSSK"));
				replyWithKeyPair();
				keyPair.get();
			}

		}

		private void replyWithKeyPair() throws IOException {
			fcpServer.writeLine("SSKKeypair",
				"InsertURI=" + INSERT_URI + "",
				"RequestURI=" + REQUEST_URI + "",
				"Identifier=" + identifier,
				"EndMessage");
		}

	}

	public class PluginCommands {

		private static final String CLASS_NAME = "foo.plugin.Plugin";

		private void replyWithPluginInfo() throws IOException {
			fcpServer.writeLine(
				"PluginInfo",
				"Identifier=" + identifier,
				"PluginName=superPlugin",
				"IsTalkable=true",
				"LongVersion=1.2.3",
				"Version=42",
				"OriginUri=superPlugin",
				"Started=true",
				"EndMessage"
			);
		}

		private void verifyPluginInfo(Future<Optional<PluginInfo>> pluginInfo)
		throws InterruptedException, ExecutionException {
			assertThat(pluginInfo.get().get().getPluginName(), is("superPlugin"));
			assertThat(pluginInfo.get().get().getOriginalURI(), is("superPlugin"));
			assertThat(pluginInfo.get().get().isTalkable(), is(true));
			assertThat(pluginInfo.get().get().getVersion(), is("42"));
			assertThat(pluginInfo.get().get().getLongVersion(), is("1.2.3"));
			assertThat(pluginInfo.get().get().isStarted(), is(true));
		}

		public class GetPluginInfo {

			@Test
			public void gettingPluginInfoWorks() throws InterruptedException, ExecutionException, IOException {
				Future<Optional<PluginInfo>> pluginInfo = fcpClient.getPluginInfo().plugin(CLASS_NAME).execute();
				connectAndAssert(this::matchGetPluginInfoMessage);
				replyWithPluginInfo();
				verifyPluginInfo(pluginInfo);
			}

			@Test
			public void gettingPluginInfoWithDetailsWorks()
			throws InterruptedException, ExecutionException, IOException {
				Future<Optional<PluginInfo>> pluginInfo =
					fcpClient.getPluginInfo().detailed().plugin(CLASS_NAME).execute();
				connectAndAssert(() -> allOf(matchGetPluginInfoMessage(), hasItem("Detailed=true")));
				replyWithPluginInfo();
				verifyPluginInfo(pluginInfo);
			}

			@Test
			public void protocolErrorIsRecognizedAsFailure()
			throws InterruptedException, ExecutionException, IOException {
				Future<Optional<PluginInfo>> pluginInfo =
					fcpClient.getPluginInfo().detailed().plugin(CLASS_NAME).execute();
				connectAndAssert(() -> allOf(matchGetPluginInfoMessage(), hasItem("Detailed=true")));
				replyWithProtocolError();
				assertThat(pluginInfo.get(), is(Optional.empty()));
			}

			private Matcher<List<String>> matchGetPluginInfoMessage() {
				return matchesFcpMessage(
					"GetPluginInfo",
					"Identifier=" + identifier,
					"PluginName=" + CLASS_NAME
				);
			}

		}

	}

	public class UskSubscriptionCommands {

		private static final String URI = "USK@some,uri/file.txt";

		@Test
		public void subscriptionWorks() throws InterruptedException, ExecutionException, IOException {
			Future<Optional<UskSubscription>> uskSubscription = fcpClient.subscribeUsk().uri(URI).execute();
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
		public void subscriptionUpdatesMultipleTimes() throws InterruptedException, ExecutionException, IOException {
			Future<Optional<UskSubscription>> uskSubscription = fcpClient.subscribeUsk().uri(URI).execute();
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
		public void subscriptionCanBeCancelled() throws InterruptedException, ExecutionException, IOException {
			Future<Optional<UskSubscription>> uskSubscription = fcpClient.subscribeUsk().uri(URI).execute();
			connectAndAssert(() -> matchesFcpMessage("SubscribeUSK", "URI=" + URI));
			String originalIdentifier = identifier;
			replyWithSubscribed();
			assertThat(uskSubscription.get().get().getUri(), is(URI));
			AtomicBoolean updated = new AtomicBoolean();
			uskSubscription.get().get().onUpdate(e -> updated.set(true));
			uskSubscription.get().get().cancel();
			readMessage(() -> matchesFcpMessage("UnsubscribeUSK", "Identifier=" + originalIdentifier));
			sendUpdateNotification(23);
			assertThat(updated.get(), is(false));
		}

		private void replyWithSubscribed() throws IOException {
			fcpServer.writeLine(
				"SubscribedUSK",
				"Identifier=" + identifier,
				"URI=" + URI,
				"DontPoll=false",
				"EndMessage"
			);
		}

		private void sendUpdateNotification(int edition, String... additionalLines) throws IOException {
			fcpServer.writeLine(
				"SubscribedUSKUpdate",
				"Identifier=" + identifier,
				"URI=" + URI,
				"Edition=" + edition
			);
			fcpServer.writeLine(additionalLines);
			fcpServer.writeLine("EndMessage");
		}

	}

	public class ClientGet {

		@Test
		public void works() throws InterruptedException, ExecutionException, IOException {
			Future<Optional<Data>> dataFuture = fcpClient.clientGet().uri("KSK@foo.txt").execute();
			connectAndAssert(() -> matchesFcpMessage("ClientGet", "URI=KSK@foo.txt", "ReturnType=direct"));
			replyWithAllData("not-test", "Hello World", "text/plain;charset=latin-9");
			replyWithAllData(identifier, "Hello", "text/plain;charset=utf-8");
			Optional<Data> data = dataFuture.get();
			verifyData(data);
		}

		@Test
		public void getFailedIsRecognized() throws InterruptedException, ExecutionException, IOException {
			Future<Optional<Data>> dataFuture = fcpClient.clientGet().uri("KSK@foo.txt").execute();
			connectAndAssert(() -> matchesFcpMessage("ClientGet", "URI=KSK@foo.txt"));
			replyWithGetFailed("not-test");
			replyWithGetFailed(identifier);
			Optional<Data> data = dataFuture.get();
			assertThat(data.isPresent(), is(false));
		}

		@Test
		public void getFailedForDifferentIdentifierIsIgnored()
		throws InterruptedException, ExecutionException, IOException {
			Future<Optional<Data>> dataFuture = fcpClient.clientGet().uri("KSK@foo.txt").execute();
			connectAndAssert(() -> matchesFcpMessage("ClientGet", "URI=KSK@foo.txt"));
			replyWithGetFailed("not-test");
			replyWithAllData(identifier, "Hello", "text/plain;charset=utf-8");
			Optional<Data> data = dataFuture.get();
			verifyData(data);
		}

		@Test(expected = ExecutionException.class)
		public void connectionClosedIsRecognized() throws InterruptedException, ExecutionException, IOException {
			Future<Optional<Data>> dataFuture = fcpClient.clientGet().uri("KSK@foo.txt").execute();
			connectAndAssert(() -> matchesFcpMessage("ClientGet", "URI=KSK@foo.txt"));
			fcpServer.close();
			dataFuture.get();
		}

		@Test
		public void withIgnoreData() throws InterruptedException, ExecutionException, IOException {
			fcpClient.clientGet().ignoreDataStore().uri("KSK@foo.txt").execute();
			connectAndAssert(() -> matchesFcpMessage("ClientGet", "URI=KSK@foo.txt", "IgnoreDS=true"));
		}

		@Test
		public void withDataStoreOnly() throws InterruptedException, ExecutionException, IOException {
			fcpClient.clientGet().dataStoreOnly().uri("KSK@foo.txt").execute();
			connectAndAssert(() -> matchesFcpMessage("ClientGet", "URI=KSK@foo.txt", "DSonly=true"));
		}

		@Test
		public void clientGetWithMaxSizeSettingSendsCorrectCommands()
		throws InterruptedException, ExecutionException, IOException {
			fcpClient.clientGet().maxSize(1048576).uri("KSK@foo.txt").execute();
			connectAndAssert(() -> matchesFcpMessage("ClientGet", "URI=KSK@foo.txt", "MaxSize=1048576"));
		}

		@Test
		public void clientGetWithPrioritySettingSendsCorrectCommands()
		throws InterruptedException, ExecutionException, IOException {
			fcpClient.clientGet().priority(Priority.interactive).uri("KSK@foo.txt").execute();
			connectAndAssert(() -> matchesFcpMessage("ClientGet", "URI=KSK@foo.txt", "PriorityClass=1"));
		}

		@Test
		public void clientGetWithRealTimeSettingSendsCorrectCommands()
		throws InterruptedException, ExecutionException, IOException {
			fcpClient.clientGet().realTime().uri("KSK@foo.txt").execute();
			connectAndAssert(() -> matchesFcpMessage("ClientGet", "URI=KSK@foo.txt", "RealTimeFlag=true"));
		}

		@Test
		public void clientGetWithGlobalSettingSendsCorrectCommands()
		throws InterruptedException, ExecutionException, IOException {
			fcpClient.clientGet().global().uri("KSK@foo.txt").execute();
			connectAndAssert(() -> matchesFcpMessage("ClientGet", "URI=KSK@foo.txt", "Global=true"));
		}

		@Test
		public void clientGetFollowsRedirect() throws InterruptedException, ExecutionException, IOException {
		    Future<Optional<Data>> data = fcpClient.clientGet().uri("USK@foo/bar").execute();
			connectAndAssert(() -> matchesFcpMessage("ClientGet", "URI=USK@foo/bar"));
			replyWithRedirect("USK@foo/baz");
			readMessage(() -> matchesFcpMessage("ClientGet", "URI=USK@foo/baz"));
			replyWithAllData(identifier, "Hello", "text/plain;charset=utf-8");
			verifyData(data.get());
		}

		@Test
		public void clientGetNotifiesListenersOnRedirect() throws IOException, ExecutionException, InterruptedException {
			List<String> redirects = new ArrayList<>();
			Future<Optional<Data>> data = fcpClient.clientGet().onRedirect(redirects::add).uri("USK@foo/bar").execute();
			connectAndAssert(() -> matchesFcpMessage("ClientGet", "URI=USK@foo/bar"));
			replyWithRedirect("USK@foo/baz");
			readMessage(() -> matchesFcpMessage("ClientGet", "URI=USK@foo/baz"));
			replyWithRedirect("USK@foo/quux");
			readMessage(() -> matchesFcpMessage("ClientGet", "URI=USK@foo/quux"));
			replyWithAllData(identifier, "Hello", "text/plain;charset=utf-8");
			verifyData(data.get());
			assertThat(redirects, contains("USK@foo/baz", "USK@foo/quux"));
		}

		private void replyWithGetFailed(String identifier) throws IOException {
			fcpServer.writeLine(
				"GetFailed",
				"Identifier=" + identifier,
				"Code=3",
				"EndMessage"
			);
		}

		private void replyWithRedirect(String newUri) throws IOException {
			fcpServer.writeLine(
				"GetFailed",
				"Identifier=" + identifier,
				"Code=27",
				"RedirectURI=" + newUri,
				"EndMessage"
			);
		}

		private void replyWithAllData(String identifier, String text, String contentType) throws IOException {
			fcpServer.writeLine(
				"AllData",
				"Identifier=" + identifier,
				"DataLength=" + (text.length() + 1),
				"StartupTime=1435610539000",
				"CompletionTime=1435610540000",
				"Metadata.ContentType=" + contentType,
				"Data",
				text
			);
		}

		private void verifyData(Optional<Data> data) throws IOException {
			assertThat(data.get().getMimeType(), is("text/plain;charset=utf-8"));
			assertThat(data.get().size(), is(6L));
			assertThat(ByteStreams.toByteArray(data.get().getInputStream()),
				is("Hello\n".getBytes(StandardCharsets.UTF_8)));
		}

	}

	public class PutCommands {

		public class ClientPut {

			@Test
			public void sendsCorrectCommand() throws IOException, ExecutionException, InterruptedException {
				fcpClient.clientPut()
					.from(new ByteArrayInputStream("Hello\n".getBytes()))
					.length(6)
					.uri("KSK@foo.txt")
					.execute();
				connectNode();
				readMessage("Hello", this::matchesDirectClientPut);
			}

			@Test
			public void succeedsOnCorrectIdentifier() throws InterruptedException, ExecutionException, IOException {
				Future<Optional<Key>> key = fcpClient.clientPut()
					.from(new ByteArrayInputStream("Hello\n".getBytes()))
					.length(6)
					.uri("KSK@foo.txt")
					.execute();
				connectNode();
				readMessage("Hello", this::matchesDirectClientPut);
				replyWithPutFailed("not-the-right-one");
				replyWithPutSuccessful(identifier);
				assertThat(key.get().get().getKey(), is("KSK@foo.txt"));
			}

			@Test
			public void failsOnCorrectIdentifier() throws InterruptedException, ExecutionException, IOException {
				Future<Optional<Key>> key = fcpClient.clientPut()
					.from(new ByteArrayInputStream("Hello\n".getBytes()))
					.length(6)
					.uri("KSK@foo.txt")
					.execute();
				connectNode();
				readMessage("Hello", this::matchesDirectClientPut);
				replyWithPutSuccessful("not-the-right-one");
				replyWithPutFailed(identifier);
				assertThat(key.get().isPresent(), is(false));
			}

			@Test
			public void renameIsSentCorrectly() throws InterruptedException, ExecutionException, IOException {
				fcpClient.clientPut()
					.named("otherName.txt")
					.from(new ByteArrayInputStream("Hello\n".getBytes()))
					.length(6)
					.uri("KSK@foo.txt")
					.execute();
				connectNode();
				readMessage("Hello", () -> allOf(
					hasHead("ClientPut"),
					hasParameters(1, 2, "TargetFilename=otherName.txt", "UploadFrom=direct", "DataLength=6",
						"URI=KSK@foo.txt"),
					hasTail("EndMessage", "Hello")
				));
			}

			@Test
			public void redirectIsSentCorrecly() throws IOException, ExecutionException, InterruptedException {
				fcpClient.clientPut().redirectTo("KSK@bar.txt").uri("KSK@foo.txt").execute();
				connectAndAssert(() ->
					matchesFcpMessage("ClientPut", "UploadFrom=redirect", "URI=KSK@foo.txt", "TargetURI=KSK@bar.txt"));
			}

			@Test
			public void withFileIsSentCorrectly() throws InterruptedException, ExecutionException, IOException {
				fcpClient.clientPut().from(new File("/tmp/data.txt")).uri("KSK@foo.txt").execute();
				connectAndAssert(() ->
					matchesFcpMessage("ClientPut", "UploadFrom=disk", "URI=KSK@foo.txt", "Filename=/tmp/data.txt"));
			}

			public class DDA {

				private final File ddaFile;
				private final File fileToUpload;

				public DDA() throws IOException {
					ddaFile = createDdaFile();
					fileToUpload = new File(ddaFile.getParent(), "test.dat");
				}

				private Matcher<List<String>> matchesFileClientPut(File file) {
					return matchesFcpMessage("ClientPut", "UploadFrom=disk", "URI=KSK@foo.txt", "Filename=" + file);
				}

				@Test
				public void completeDda() throws IOException, ExecutionException, InterruptedException {
					fcpClient.clientPut().from(fileToUpload).uri("KSK@foo.txt").execute();
					connectAndAssert(() -> matchesFileClientPut(fileToUpload));
					sendDdaRequired(identifier);
					readMessage(() -> matchesTestDDARequest(ddaFile));
					sendTestDDAReply(ddaFile.getParent(), ddaFile);
					readMessage(() -> matchesTestDDAResponse(ddaFile));
					writeTestDDAComplete(ddaFile);
					readMessage(() -> matchesFileClientPut(fileToUpload));
				}

				@Test
				public void ignoreOtherDda() throws IOException, ExecutionException, InterruptedException {
					fcpClient.clientPut().from(fileToUpload).uri("KSK@foo.txt").execute();
					connectAndAssert(() -> matchesFileClientPut(fileToUpload));
					sendDdaRequired(identifier);
					readMessage(() -> matchesTestDDARequest(ddaFile));
					sendTestDDAReply("/some-other-directory", ddaFile);
					sendTestDDAReply(ddaFile.getParent(), ddaFile);
					readMessage(() -> matchesTestDDAResponse(ddaFile));
				}

				@Test
				public void sendResponseIfFileUnreadable()
				throws IOException, ExecutionException, InterruptedException {
					fcpClient.clientPut().from(fileToUpload).uri("KSK@foo.txt").execute();
					connectAndAssert(() -> matchesFileClientPut(fileToUpload));
					sendDdaRequired(identifier);
					readMessage(() -> matchesTestDDARequest(ddaFile));
					sendTestDDAReply(ddaFile.getParent(), new File(ddaFile + ".foo"));
					readMessage(this::matchesFailedToReadResponse);
				}

				@Test
				public void clientPutDoesNotResendOriginalClientPutOnTestDDACompleteWithWrongDirectory()
				throws IOException, ExecutionException, InterruptedException {
					fcpClient.clientPut().from(fileToUpload).uri("KSK@foo.txt").execute();
					connectNode();
					List<String> lines = fcpServer.collectUntil(is("EndMessage"));
					String identifier = extractIdentifier(lines);
					fcpServer.writeLine(
						"TestDDAComplete",
						"Directory=/some-other-directory",
						"EndMessage"
					);
					sendDdaRequired(identifier);
					lines = fcpServer.collectUntil(is("EndMessage"));
					assertThat(lines, matchesFcpMessage(
						"TestDDARequest",
						"Directory=" + ddaFile.getParent(),
						"WantReadDirectory=true",
						"WantWriteDirectory=false"
					));
				}

				private Matcher<List<String>> matchesFailedToReadResponse() {
					return matchesFcpMessage(
						"TestDDAResponse",
						"Directory=" + ddaFile.getParent(),
						"ReadContent=failed-to-read"
					);
				}

				private void writeTestDDAComplete(File tempFile) throws IOException {
					fcpServer.writeLine(
						"TestDDAComplete",
						"Directory=" + tempFile.getParent(),
						"ReadDirectoryAllowed=true",
						"EndMessage"
					);
				}

				private Matcher<List<String>> matchesTestDDAResponse(File tempFile) {
					return matchesFcpMessage(
						"TestDDAResponse",
						"Directory=" + tempFile.getParent(),
						"ReadContent=test-content"
					);
				}

				private void sendTestDDAReply(String directory, File tempFile) throws IOException {
					fcpServer.writeLine(
						"TestDDAReply",
						"Directory=" + directory,
						"ReadFilename=" + tempFile,
						"EndMessage"
					);
				}

				private Matcher<List<String>> matchesTestDDARequest(File tempFile) {
					return matchesFcpMessage(
						"TestDDARequest",
						"Directory=" + tempFile.getParent(),
						"WantReadDirectory=true",
						"WantWriteDirectory=false"
					);
				}

				private void sendDdaRequired(String identifier) throws IOException {
					fcpServer.writeLine(
						"ProtocolError",
						"Identifier=" + identifier,
						"Code=25",
						"EndMessage"
					);
				}

			}

			private void replyWithPutFailed(String identifier) throws IOException {
				fcpServer.writeLine(
					"PutFailed",
					"Identifier=" + identifier,
					"EndMessage"
				);
			}

			private Matcher<List<String>> matchesDirectClientPut(String... additionalLines) {
				List<String> lines =
					new ArrayList<>(Arrays.asList("UploadFrom=direct", "DataLength=6", "URI=KSK@foo.txt"));
				Arrays.asList(additionalLines).forEach(lines::add);
				return allOf(
					hasHead("ClientPut"),
					hasParameters(1, 2, lines.toArray(new String[lines.size()])),
					hasTail("EndMessage", "Hello")
				);
			}

			private File createDdaFile() throws IOException {
				File tempFile = File.createTempFile("test-dda-", ".dat");
				tempFile.deleteOnExit();
				Files.write("test-content", tempFile, StandardCharsets.UTF_8);
				return tempFile;
			}

			@Test
			public void clientPutDoesNotReactToProtocolErrorForDifferentIdentifier()
			throws InterruptedException, ExecutionException, IOException {
				Future<Optional<Key>> key =
					fcpClient.clientPut().from(new File("/tmp/data.txt")).uri("KSK@foo.txt").execute();
				connectNode();
				List<String> lines = fcpServer.collectUntil(is("EndMessage"));
				String identifier = extractIdentifier(lines);
				fcpServer.writeLine(
					"ProtocolError",
					"Identifier=not-the-right-one",
					"Code=25",
					"EndMessage"
				);
				fcpServer.writeLine(
					"PutSuccessful",
					"Identifier=" + identifier,
					"URI=KSK@foo.txt",
					"EndMessage"
				);
				assertThat(key.get().get().getKey(), is("KSK@foo.txt"));
			}

			@Test
			public void clientPutAbortsOnProtocolErrorOtherThan25()
			throws InterruptedException, ExecutionException, IOException {
				Future<Optional<Key>> key =
					fcpClient.clientPut().from(new File("/tmp/data.txt")).uri("KSK@foo.txt").execute();
				connectNode();
				List<String> lines = fcpServer.collectUntil(is("EndMessage"));
				String identifier = extractIdentifier(lines);
				fcpServer.writeLine(
					"ProtocolError",
					"Identifier=" + identifier,
					"Code=1",
					"EndMessage"
				);
				assertThat(key.get().isPresent(), is(false));
			}

			@Test
			public void clientPutSendsNotificationsForGeneratedKeys()
			throws InterruptedException, ExecutionException, IOException {
				List<String> generatedKeys = new CopyOnWriteArrayList<>();
				Future<Optional<Key>> key = fcpClient.clientPut()
					.onKeyGenerated(generatedKeys::add)
					.from(new ByteArrayInputStream("Hello\n".getBytes()))
					.length(6)
					.uri("KSK@foo.txt")
					.execute();
				connectNode();
				readMessage("Hello", this::matchesDirectClientPut);
				replyWithGeneratedUri();
				replyWithPutSuccessful(identifier);
				assertThat(key.get().get().getKey(), is("KSK@foo.txt"));
				assertThat(generatedKeys, contains("KSK@foo.txt"));
			}

			@Test
			public void clientPutSendsNotificationOnProgress()
			throws InterruptedException, ExecutionException, IOException {
				List<RequestProgress> requestProgress = new ArrayList<>();
				Future<Optional<Key>> key = fcpClient.clientPut()
					.onProgress(requestProgress::add)
					.from(new ByteArrayInputStream("Hello\n".getBytes()))
					.length(6)
					.uri("KSK@foo.txt")
					.execute();
				connectNode();
				readMessage("Hello", () -> matchesDirectClientPut("Verbosity=1"));
				replyWithSimpleProgress(1, 2, 3, 4, 5, 6, true, 8);
				replyWithSimpleProgress(11, 12, 13, 14, 15, 16, false, 18);
				replyWithPutSuccessful(identifier);
				assertThat(key.get().get().getKey(), is("KSK@foo.txt"));
				assertThat(requestProgress, contains(
					isRequestProgress(1, 2, 3, 4, 5, 6, true, 8),
					isRequestProgress(11, 12, 13, 14, 15, 16, false, 18)
				));
			}

		}

		private void replyWithPutSuccessful(String identifier) throws IOException {
			fcpServer.writeLine(
				"PutSuccessful",
				"URI=KSK@foo.txt",
				"Identifier=" + identifier,
				"EndMessage"
			);
		}

		private void replyWithGeneratedUri() throws IOException {
			fcpServer.writeLine(
				"URIGenerated",
				"Identifier=" + identifier,
				"URI=KSK@foo.txt",
				"EndMessage"
			);
		}

		private void replyWithSimpleProgress(
			int total, int required, int failed, int fatallyFailed, int succeeded, int lastProgress,
			boolean finalizedTotal, int minSuccessFetchBlocks) throws IOException {
			fcpServer.writeLine(
				"SimpleProgress",
				"Identifier=" + identifier,
				"Total=" + total,
				"Required=" + required,
				"Failed=" + failed,
				"FatallyFailed=" + fatallyFailed,
				"Succeeded=" + succeeded,
				"LastProgress=" + lastProgress,
				"FinalizedTotal=" + finalizedTotal,
				"MinSuccessFetchBlocks=" + minSuccessFetchBlocks,
				"EndMessage"
			);
		}

		public class ClientPutDiskDir {

			@Test
			public void commandIsSentCorrectly() throws InterruptedException, ExecutionException, IOException {
				Future<Optional<Key>> key = fcpClient.clientPutDiskDir().fromDirectory(new File("")).uri("CHK@").execute();
				connectAndAssert(this::matchesClientPutDiskDir);
				fcpServer.writeLine("PutSuccessful", "Identifier=" + identifier, "URI=CHK@abc", "EndMessage");
				assertThat(key.get().get().getKey(), is("CHK@abc"));
			}

			@Test
			public void protocolErrorAbortsCommand() throws InterruptedException, ExecutionException, IOException {
				Future<Optional<Key>> key = fcpClient.clientPutDiskDir().fromDirectory(new File("")).uri("CHK@").execute();
				connectAndAssert(this::matchesClientPutDiskDir);
				replyWithProtocolError();
				assertThat(key.get().isPresent(), is(false));
			}

			@Test
			public void progressIsSentToConsumerCorrectly() throws InterruptedException, ExecutionException, IOException {
				List<RequestProgress> requestProgress = new ArrayList<>();
				Future<Optional<Key>> key = fcpClient.clientPutDiskDir().onProgress(requestProgress::add)
					.fromDirectory(new File("")).uri("CHK@").execute();
				connectAndAssert(() -> matchesClientPutDiskDir("Verbosity=1"));
				replyWithSimpleProgress(1, 2, 3, 4, 5, 6, true, 8);
				replyWithSimpleProgress(11, 12, 13, 14, 15, 16, false, 18);
				replyWithPutSuccessful(identifier);
				assertThat(key.get().get().getKey(), is("KSK@foo.txt"));
				assertThat(requestProgress, contains(
					isRequestProgress(1, 2, 3, 4, 5, 6, true, 8),
					isRequestProgress(11, 12, 13, 14, 15, 16, false, 18)
				));
			}

			@Test
			public void generatedUriIsSentToConsumerCorrectly() throws InterruptedException, ExecutionException, IOException {
				List<String> generatedKeys = new ArrayList<>();
				Future<Optional<Key>> key = fcpClient.clientPutDiskDir().onKeyGenerated(generatedKeys::add)
					.fromDirectory(new File("")).uri("CHK@").execute();
				connectAndAssert(this::matchesClientPutDiskDir);
				replyWithGeneratedUri();
				replyWithPutSuccessful(identifier);
				assertThat(key.get().get().getKey(), is("KSK@foo.txt"));
				assertThat(generatedKeys, contains("KSK@foo.txt"));
			}

			private Matcher<List<String>> matchesClientPutDiskDir(String... additionalLines) {
				List<String> lines = new ArrayList<>(Arrays.asList("Identifier=" + identifier, "URI=CHK@", "Filename=" + new File("").getPath()));
				Arrays.asList(additionalLines).forEach(lines::add);
				return matchesFcpMessage("ClientPutDiskDir", lines.toArray(new String[lines.size()]));
			}

		}

	}

	public class ConfigCommand {

		public class GetConfig {

			@Test
			public void defaultFcpClientCanGetConfigWithoutDetails()
			throws InterruptedException, ExecutionException, IOException {
				Future<ConfigData> configData = fcpClient.getConfig().execute();
				connectAndAssert(() -> matchesFcpMessage("GetConfig", "Identifier=" + identifier));
				replyWithConfigData();
				assertThat(configData.get(), notNullValue());
			}

			@Test
			public void defaultFcpClientCanGetConfigWithCurrent()
			throws InterruptedException, ExecutionException, IOException {
				Future<ConfigData> configData = fcpClient.getConfig().withCurrent().execute();
				connectAndAssert(() -> matchesGetConfigWithAdditionalParameter("WithCurrent"));
				replyWithConfigData("current.foo=bar");
				assertThat(configData.get().getCurrent("foo"), is("bar"));
			}

			@Test
			public void defaultFcpClientCanGetConfigWithDefaults()
			throws InterruptedException, ExecutionException, IOException {
				Future<ConfigData> configData = fcpClient.getConfig().withDefaults().execute();
				connectAndAssert(() -> matchesGetConfigWithAdditionalParameter("WithDefaults"));
				replyWithConfigData("default.foo=bar");
				assertThat(configData.get().getDefault("foo"), is("bar"));
			}

			@Test
			public void defaultFcpClientCanGetConfigWithSortOrder()
			throws InterruptedException, ExecutionException, IOException {
				Future<ConfigData> configData = fcpClient.getConfig().withSortOrder().execute();
				connectAndAssert(() -> matchesGetConfigWithAdditionalParameter("WithSortOrder"));
				replyWithConfigData("sortOrder.foo=17");
				assertThat(configData.get().getSortOrder("foo"), is(17));
			}

			@Test
			public void defaultFcpClientCanGetConfigWithExpertFlag()
			throws InterruptedException, ExecutionException, IOException {
				Future<ConfigData> configData = fcpClient.getConfig().withExpertFlag().execute();
				connectAndAssert(() -> matchesGetConfigWithAdditionalParameter("WithExpertFlag"));
				replyWithConfigData("expertFlag.foo=true");
				assertThat(configData.get().getExpertFlag("foo"), is(true));
			}

			@Test
			public void defaultFcpClientCanGetConfigWithForceWriteFlag()
			throws InterruptedException, ExecutionException, IOException {
				Future<ConfigData> configData = fcpClient.getConfig().withForceWriteFlag().execute();
				connectAndAssert(() -> matchesGetConfigWithAdditionalParameter("WithForceWriteFlag"));
				replyWithConfigData("forceWriteFlag.foo=true");
				assertThat(configData.get().getForceWriteFlag("foo"), is(true));
			}

			@Test
			public void defaultFcpClientCanGetConfigWithShortDescription()
			throws InterruptedException, ExecutionException, IOException {
				Future<ConfigData> configData = fcpClient.getConfig().withShortDescription().execute();
				connectAndAssert(() -> matchesGetConfigWithAdditionalParameter("WithShortDescription"));
				replyWithConfigData("shortDescription.foo=bar");
				assertThat(configData.get().getShortDescription("foo"), is("bar"));
			}

			@Test
			public void defaultFcpClientCanGetConfigWithLongDescription()
			throws InterruptedException, ExecutionException, IOException {
				Future<ConfigData> configData = fcpClient.getConfig().withLongDescription().execute();
				connectAndAssert(() -> matchesGetConfigWithAdditionalParameter("WithLongDescription"));
				replyWithConfigData("longDescription.foo=bar");
				assertThat(configData.get().getLongDescription("foo"), is("bar"));
			}

			@Test
			public void defaultFcpClientCanGetConfigWithDataTypes()
			throws InterruptedException, ExecutionException, IOException {
				Future<ConfigData> configData = fcpClient.getConfig().withDataTypes().execute();
				connectAndAssert(() -> matchesGetConfigWithAdditionalParameter("WithDataTypes"));
				replyWithConfigData("dataType.foo=number");
				assertThat(configData.get().getDataType("foo"), is("number"));
			}

			private Matcher<List<String>> matchesGetConfigWithAdditionalParameter(String additionalParameter) {
				return matchesFcpMessage(
					"GetConfig",
					"Identifier=" + identifier,
					additionalParameter + "=true"
				);
			}

		}

		public class ModifyConfig {

			@Test
			public void defaultFcpClientCanModifyConfigData()
			throws InterruptedException, ExecutionException, IOException {
				Future<ConfigData> newConfigData = fcpClient.modifyConfig().set("foo.bar").to("baz").execute();
				connectAndAssert(() -> matchesFcpMessage(
					"ModifyConfig",
					"Identifier=" + identifier,
					"foo.bar=baz"
				));
				replyWithConfigData("current.foo.bar=baz");
				assertThat(newConfigData.get().getCurrent("foo.bar"), is("baz"));
			}

		}

		private void replyWithConfigData(String... additionalLines) throws IOException {
			fcpServer.writeLine("ConfigData", "Identifier=" + identifier);
			fcpServer.writeLine(additionalLines);
			fcpServer.writeLine("EndMessage");
		}

	}

	public class NodeInformation {

		@Test
		public void defaultFcpClientCanGetNodeInformation() throws InterruptedException, ExecutionException, IOException {
			Future<NodeData> nodeData = fcpClient.getNode().execute();
			connectAndAssert(() -> matchesGetNode(false, false, false));
			replyWithNodeData();
			assertThat(nodeData.get(), notNullValue());
			assertThat(nodeData.get().getNodeRef().isOpennet(), is(false));
		}

		@Test
		public void defaultFcpClientCanGetNodeInformationWithOpennetRef()
		throws InterruptedException, ExecutionException, IOException {
			Future<NodeData> nodeData = fcpClient.getNode().opennetRef().execute();
			connectAndAssert(() -> matchesGetNode(true, false, false));
			replyWithNodeData("opennet=true");
			assertThat(nodeData.get().getVersion().toString(), is("Fred,0.7,1.0,1466"));
			assertThat(nodeData.get().getNodeRef().isOpennet(), is(true));
		}

		@Test
		public void defaultFcpClientCanGetNodeInformationWithPrivateData()
		throws InterruptedException, ExecutionException, IOException {
			Future<NodeData> nodeData = fcpClient.getNode().includePrivate().execute();
			connectAndAssert(() -> matchesGetNode(false, true, false));
			replyWithNodeData("ark.privURI=SSK@XdHMiRl");
			assertThat(nodeData.get().getARK().getPrivateURI(), is("SSK@XdHMiRl"));
		}

		@Test
		public void defaultFcpClientCanGetNodeInformationWithVolatileData()
		throws InterruptedException, ExecutionException, IOException {
			Future<NodeData> nodeData = fcpClient.getNode().includeVolatile().execute();
			connectAndAssert(() -> matchesGetNode(false, false, true));
			replyWithNodeData("volatile.freeJavaMemory=205706528");
			assertThat(nodeData.get().getVolatile("freeJavaMemory"), is("205706528"));
		}

		private Matcher<List<String>> matchesGetNode(boolean withOpennetRef, boolean withPrivate, boolean withVolatile) {
			return matchesFcpMessage(
				"GetNode",
				"Identifier=" + identifier,
				"GiveOpennetRef=" + withOpennetRef,
				"WithPrivate=" + withPrivate,
				"WithVolatile=" + withVolatile
			);
		}

		private void replyWithNodeData(String... additionalLines) throws IOException {
			fcpServer.writeLine(
				"NodeData",
				"Identifier=" + identifier,
				"ark.pubURI=SSK@3YEf.../ark",
				"ark.number=78",
				"auth.negTypes=2",
				"version=Fred,0.7,1.0,1466",
				"lastGoodVersion=Fred,0.7,1.0,1466"
			);
			fcpServer.writeLine(additionalLines);
			fcpServer.writeLine("EndMessage");
		}

	}

}
