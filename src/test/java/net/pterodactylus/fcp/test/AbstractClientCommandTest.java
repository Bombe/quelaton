package net.pterodactylus.fcp.test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import net.pterodactylus.fcp.quelaton.FcpClient;

import org.hamcrest.Matcher;
import org.junit.After;

/**
 * Abstract base class for all client command tests.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class AbstractClientCommandTest {

	private final WithFcp fcp = new WithFcp();

	public String identifier() {
		return fcp.identifier();
	}

	public List<String> lines() {
		return fcp.lines();
	}

	public FcpClient client() {
		return fcp.client();
	}

	public void answer(String... reply) throws IOException {
		fcp.answer(reply);
	}

	public void connectNode() throws InterruptedException, ExecutionException, IOException {
		fcp.connectNode();
	}

	public void connectAndAssert(Supplier<Matcher<List<String>>> requestMatcher) throws InterruptedException, ExecutionException, IOException {
		fcp.connectAndAssert(requestMatcher);
	}

	public void connectAndAssert(String terminator, Supplier<Matcher<List<String>>> requestMatcher) throws InterruptedException, ExecutionException, IOException {
		fcp.connectAndAssert(terminator, requestMatcher);
	}

	public String extractIdentifier(List<String> lines) {
		return fcp.extractIdentifier(lines);
	}

	public List<String> collectUntil(Matcher<String> lineMatcher) throws IOException {
		return fcp.collectUntil(lineMatcher);
	}

	public void readMessage(Supplier<Matcher<List<String>>> requestMatcher) throws IOException {
		fcp.readMessage(requestMatcher);
	}

	public void readMessage(String terminator, Supplier<Matcher<List<String>>> requestMatcher) throws IOException {
		fcp.readMessage(terminator, requestMatcher);
	}

	public Matcher<List<String>> matchesFcpMessage(String name, String... requiredLines) {
		return fcp.matchesFcpMessage(name, requiredLines);
	}

	public Matcher<List<String>> matchesFcpMessageWithTerminator(String name, String terminator, String... requiredLines) {
		return fcp.matchesFcpMessageWithTerminator(name, terminator, requiredLines);
	}

	public Matcher<Iterable<String>> hasHead(String firstElement) {
		return fcp.hasHead(firstElement);
	}

	public Matcher<List<String>> hasParameters(int ignoreStart, int ignoreEnd, String... lines) {
		return fcp.hasParameters(ignoreStart, ignoreEnd, lines);
	}

	public Matcher<List<String>> hasTail(String... lastElements) {
		return fcp.hasTail(lastElements);
	}

	public void replyWithProtocolError() throws IOException {
		answer(
				"ProtocolError",
				"Identifier=" + identifier(),
				"EndMessage"
		);
	}

	public void closeFcpServer() throws IOException {
		fcp.closeFcpServer();
	}

	@After
	public void tearDownFcp() throws IOException {
		fcp.closeFcpServer();
		fcp.closeThreadpool();
	}

}
