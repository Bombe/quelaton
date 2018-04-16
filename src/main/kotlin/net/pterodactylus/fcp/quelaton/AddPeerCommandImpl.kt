package net.pterodactylus.fcp.quelaton

import com.google.common.util.concurrent.*
import net.pterodactylus.fcp.*
import java.io.*
import java.net.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*
import java.util.function.*

/**
 * Default [AddPeerCommand] implementation based on [FcpDialog].
 *
 * @author [David ‘Bombe’ Roden](mailto:bombe@freenetproject.org)
 */
internal class AddPeerCommandImpl(threadPool: ExecutorService, private val connectionSupplier: ConnectionSupplier, private val identifierSupplier: Supplier<String>) : AddPeerCommand {

	private val threadPool: ListeningExecutorService = MoreExecutors.listeningDecorator(threadPool)
	private val file = AtomicReference<File>()
	private val url = AtomicReference<URL>()
	private val nodeRef = AtomicReference<NodeRef>()

	override fun fromFile(file: File) =
			this.file.set(file).let {
				Executable { this.execute() }
			}

	override fun fromURL(url: URL) =
			this.url.set(url).let {
				Executable { this.execute() }
			}

	override fun fromNodeRef(nodeRef: NodeRef) =
			this.nodeRef.set(nodeRef).let {
				Executable { this.execute() }
			}

	private fun execute() =
			threadPool.submit(this::executeDialog)!!

	private fun executeDialog() =
			when {
				file.get() != null -> AddPeer(identifierSupplier.get(), file.get().path)
				url.get() != null -> AddPeer(identifierSupplier.get(), url.get())
				else -> AddPeer(identifierSupplier.get(), nodeRef.get())
			}.let { addPeer ->
				AddPeerDialog().use { addPeerDialog ->
					addPeerDialog.send(addPeer).get()
				}
			}

	private inner class AddPeerDialog : FcpDialog<Peer?>(threadPool, connectionSupplier.get()) {

		override fun consumePeer(peer: Peer) {
			result = peer
		}

		override fun consumeProtocolError(protocolError: ProtocolError) = finish()

	}

}
