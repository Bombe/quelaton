package net.pterodactylus.fcp.quelaton

import java.io.File
import java.net.URL
import java.util.Optional

import net.pterodactylus.fcp.NodeRef
import net.pterodactylus.fcp.Peer

/**
 * Command that adds a peer to the node.
 */
interface AddPeerCommand {

	fun fromFile(file: File): Executable<Optional<Peer>>
	fun fromURL(url: URL): Executable<Optional<Peer>>
	fun fromNodeRef(nodeRef: NodeRef): Executable<Optional<Peer>>

}
