package net.pterodactylus.fcp.quelaton

import net.pterodactylus.fcp.*
import net.pterodactylus.fcp.test.*
import org.hamcrest.MatcherAssert.*
import org.hamcrest.Matchers.*
import org.junit.*
import java.io.*
import java.net.*

/**
 * Unit test for [AddPeerCommand].
 */
class AddPeerCommandTest : AbstractPeerCommandTest() {

	private fun matchesAddPeer() =
			matchesFcpMessage(
					"AddPeer",
					"Identifier=" + identifier()
			)

	@Test
	fun `peer can be added from file`() {
		val peer = client().addPeer().fromFile(File("/tmp/ref.txt")).execute()
		connectAndAssert { allOf(matchesAddPeer(), hasItem("File=/tmp/ref.txt")) }
		replyWithPeer("id1")
		assertThat(peer.get().get().identity, equalTo("id1"))
	}

	@Test
	fun `peer can be added from url`() {
		val peer = client().addPeer().fromURL(URL("http://node.ref/")).execute()
		connectAndAssert { allOf(matchesAddPeer(), hasItem("URL=http://node.ref/")) }
		replyWithPeer("id1")
		assertThat(peer.get().get().identity, equalTo("id1"))
	}

	@Test
	fun `peer can be added from noderef`() {
		val noderef = createNodeRef()
		val peer = client().addPeer().fromNodeRef(noderef).execute()
		connectAndAssert {
			allOf(matchesAddPeer(), hasItems(
					"myName=name",
					"ark.pubURI=public",
					"ark.number=1",
					"dsaGroup.g=base",
					"dsaGroup.p=prime",
					"dsaGroup.q=subprime",
					"dsaPubKey.y=dsa-public",
					"physical.udp=1.2.3.4:5678",
					"auth.negTypes=3;5",
					"sig=sig"
			))
		}
		replyWithPeer("id1")
		assertThat(peer.get().get().identity, equalTo("id1"))
	}

	private fun createNodeRef() =
			NodeRef().apply {
				identity = "id1"
				setName("name")
				ark = ARK("public", "1")
				dsaGroup = DSAGroup("base", "prime", "subprime")
				negotiationTypes = intArrayOf(3, 5)
				physicalUDP = "1.2.3.4:5678"
				dsaPublicKey = "dsa-public"
				signature = "sig"
			}

	@Test
	fun `protocol error ends command`() {
		val peer = client().addPeer().fromFile(File("/tmp/ref.txt")).execute()
		connectAndAssert { allOf(matchesAddPeer(), hasItem("File=/tmp/ref.txt")) }
		replyWithProtocolError()
		assertThat(peer.get().isPresent, equalTo(false))
	}

}
