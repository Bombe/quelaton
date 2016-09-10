package net.pterodactylus.fcp.quelaton;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;
import java.util.concurrent.Future;

import net.pterodactylus.fcp.NodeData;
import net.pterodactylus.fcp.test.AbstractClientCommandTest;

import org.hamcrest.Matcher;
import org.junit.Test;

/**
 * Unit test for {@link GetNodeCommand}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class GetNodeCommandTest extends AbstractClientCommandTest {

	@Test
	public void defaultFcpClientCanGetNodeInformation() throws Exception {
		Future<NodeData> nodeData = client().getNode().execute();
		connectAndAssert(() -> matchesGetNode(false, false, false));
		replyWithNodeData();
		assertThat(nodeData.get(), notNullValue());
		assertThat(nodeData.get().getNodeRef().isOpennet(), is(false));
	}

	@Test
	public void defaultFcpClientCanGetNodeInformationWithOpennetRef() throws Exception {
		Future<NodeData> nodeData = client().getNode().opennetRef().execute();
		connectAndAssert(() -> matchesGetNode(true, false, false));
		replyWithNodeData("opennet=true");
		assertThat(nodeData.get().getVersion().toString(), is("Fred,0.7,1.0,1466"));
		assertThat(nodeData.get().getNodeRef().isOpennet(), is(true));
	}

	@Test
	public void defaultFcpClientCanGetNodeInformationWithPrivateData() throws Exception {
		Future<NodeData> nodeData = client().getNode().includePrivate().execute();
		connectAndAssert(() -> matchesGetNode(false, true, false));
		replyWithNodeData("ark.privURI=SSK@XdHMiRl");
		assertThat(nodeData.get().getARK().getPrivateURI(), is("SSK@XdHMiRl"));
	}

	@Test
	public void defaultFcpClientCanGetNodeInformationWithVolatileData() throws Exception {
		Future<NodeData> nodeData = client().getNode().includeVolatile().execute();
		connectAndAssert(() -> matchesGetNode(false, false, true));
		replyWithNodeData("volatile.freeJavaMemory=205706528");
		assertThat(nodeData.get().getVolatile("freeJavaMemory"), is("205706528"));
	}

	private Matcher<List<String>> matchesGetNode(boolean withOpennetRef, boolean withPrivate, boolean withVolatile) {
		return matchesFcpMessage(
				"GetNode",
				"Identifier=" + identifier(),
				"GiveOpennetRef=" + withOpennetRef,
				"WithPrivate=" + withPrivate,
				"WithVolatile=" + withVolatile
		);
	}

	private void replyWithNodeData(String... additionalLines) throws Exception {
		answer(
				"NodeData",
				"Identifier=" + identifier(),
				"ark.pubURI=SSK@3YEf.../ark",
				"ark.number=78",
				"auth.negTypes=2",
				"version=Fred,0.7,1.0,1466",
				"lastGoodVersion=Fred,0.7,1.0,1466"
		);
		answer(additionalLines);
		answer("EndMessage");
	}

}
