package net.pterodactylus.fcp.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import net.pterodactylus.fcp.fake.FakeTcpServer;
import net.pterodactylus.fcp.quelaton.DefaultFcpClient;
import net.pterodactylus.fcp.quelaton.FcpClient;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Rule;
import org.junit.rules.ExternalResource;

/**
 * JUnit {@link Rule} that manages a fake FCP server and the communication with it, as well as tools for matching messages.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class WithFcp implements AutoCloseable {

	private int threadCounter = 0;
	private final ExecutorService threadPool = Executors.newCachedThreadPool(r -> new Thread(r, "Test-Thread-" + threadCounter++));
	private final FakeTcpServer fcpServer;
	private final DefaultFcpClient fcpClient;

	private List<String> lines;
	private String identifier;

	public WithFcp() {
		try {
			fcpServer = new FakeTcpServer(threadPool);
		} catch (IOException ioe1) {
			throw new RuntimeException(ioe1);
		}
		fcpClient = new DefaultFcpClient(threadPool, "localhost", fcpServer.getPort(), () -> "Test");
	}

	public String identifier() {
		return identifier;
	}

	public List<String> lines() {
		return lines;
	}

	public FcpClient client() {
		return fcpClient;
	}

	public void answer(String... reply) throws IOException {
		fcpServer.writeLine(reply);
	}

	public void connectAndAssert(Supplier<Matcher<List<String>>> requestMatcher) throws InterruptedException, ExecutionException, IOException {
		connectNode();
		readMessage(requestMatcher);
	}

	public void connectNode() throws InterruptedException, ExecutionException, IOException {
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

	public List<String> collectUntil(Matcher<String> lineMatcher) throws IOException {
		return fcpServer.collectUntil(lineMatcher);
	}

	public void readMessage(Supplier<Matcher<List<String>>> requestMatcher) throws IOException {
		readMessage("EndMessage", requestMatcher);
	}

	public void readMessage(String terminator, Supplier<Matcher<List<String>>> requestMatcher) throws IOException {
		lines = fcpServer.collectUntil(is(terminator));
		identifier = extractIdentifier(lines);
		assertThat(lines, requestMatcher.get());
	}

	public String extractIdentifier(List<String> lines) {
		return lines.stream()
				.filter(s -> s.startsWith("Identifier="))
				.map(s -> s.substring(s.indexOf('=') + 1))
				.findFirst()
				.orElse("");
	}

	public Matcher<List<String>> matchesFcpMessage(String name, String... requiredLines) {
		return matchesFcpMessageWithTerminator(name, "EndMessage", requiredLines);
	}

	public Matcher<List<String>> matchesFcpMessageWithTerminator(String name, String terminator, String... requiredLines) {
		return allOf(hasHead(name), hasParameters(1, 1, requiredLines), hasTail(terminator));
	}

	public Matcher<Iterable<String>> hasHead(String firstElement) {
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

	public Matcher<List<String>> hasParameters(int ignoreStart, int ignoreEnd, String... lines) {
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

	public Matcher<List<String>> hasTail(String... lastElements) {
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

	@Override
	public void close() throws IOException {
		fcpServer.close();
		threadPool.shutdown();
	}

}
