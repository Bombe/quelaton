package net.pterodactylus.fcp.fake

import org.hamcrest.Matchers.*
import org.junit.*
import org.junit.Assert.*
import java.io.*
import java.net.*
import java.util.Arrays.*
import java.util.concurrent.*

/**
 * Test for [FakeTcpServer].
 */
class FakeTcpServerTest {

	private val sameThread = Executors.newSingleThreadExecutor()!!
	private val tcpServer: FakeTcpServer = FakeTcpServer(sameThread)

	@Test
	fun testConnect() {
		ProxySelector.setDefault(object : ProxySelector() {
			override fun select(uri: URI): List<Proxy> {
				return asList(Proxy.NO_PROXY)
			}

			override fun connectFailed(uri: URI, sa: SocketAddress, ioe: IOException) {}
		})
		val connect = tcpServer.connect()
		TextSocket(Socket("127.0.0.1", tcpServer.port)).use { clientSocket ->
			connect.get()
			clientSocket.writeLine("Hello")
			clientSocket.writeLine("Bye")
			assertThat(tcpServer.collectUntil(equalTo("Bye")), contains("Hello", "Bye"))
			tcpServer.writeLine("Yes")
			tcpServer.writeLine("Quit")
			assertThat(clientSocket.collectUntil(equalTo("Quit")), contains("Yes", "Quit"))
		}
	}

}
