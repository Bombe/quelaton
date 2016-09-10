package net.pterodactylus.fcp.quelaton;

import static net.pterodactylus.fcp.quelaton.RequestProgressMatcher.isRequestProgress;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import net.pterodactylus.fcp.ConfigData;
import net.pterodactylus.fcp.FcpKeyPair;
import net.pterodactylus.fcp.Key;
import net.pterodactylus.fcp.NodeData;
import net.pterodactylus.fcp.RequestProgress;
import net.pterodactylus.fcp.fake.FakeTcpServer;

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

	public class PutCommands {

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
