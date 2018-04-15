package net.pterodactylus.fcp.quelaton

import com.google.common.util.concurrent.*
import net.pterodactylus.fcp.*
import net.pterodactylus.fcp.util.*
import java.io.*
import java.util.concurrent.*
import java.util.concurrent.locks.*
import kotlin.concurrent.*

/**
 * An FCP dialog enables you to conveniently wait for a specific set of FCP replies.
 */
internal abstract class FcpDialog<R>(executorService: ExecutorService, private val fcpConnection: FcpConnection, initialResult: R? = null) : Closeable, FcpListener {

	private val lock = ReentrantLock()
	private val newMessageOrFinished: Condition = lock.newCondition()
	private val executorService: ListeningExecutorService = MoreExecutors.listeningDecorator(executorService)
	private val messages = ConcurrentLinkedQueue<FcpMessage>()
	protected var identifier by atomic<String?>(null)
	private var connectionClosed by atomic(false)
	private var connectionFailureReason by atomic<Throwable?>(null)
	private var finished by atomic(false)
	protected var result by atomicObservable(initialResult) { finish() }

	protected fun finish() {
		finished = true
		notifySyncObject()
	}

	@Throws(IOException::class)
	open fun send(fcpMessage: FcpMessage): ListenableFuture<R> {
		identifier = fcpMessage.getField("Identifier")
		fcpConnection.addFcpListener(this)
		messages.add(fcpMessage)
		return executorService.submit<R> {
			lock.withLock {
				while (!connectionClosed && (!finished || !messages.isEmpty())) {
					while (messages.peek() != null) {
						val message: FcpMessage = messages.poll()
						fcpConnection.sendMessage(message)
					}
					if (finished || connectionClosed) {
						continue
					}
					newMessageOrFinished.await()
				}
			}
			connectionFailureReason?.let { throw ExecutionException(it) } ?: result
		}
	}

	protected fun sendMessage(fcpMessage: FcpMessage) {
		messages.add(fcpMessage)
		notifySyncObject()
	}

	private fun notifySyncObject() {
		lock.withLock {
			newMessageOrFinished.signalAll()
		}
	}

	override fun close() {
		fcpConnection.removeFcpListener(this)
	}

	private fun <M : BaseMessage> consume(message: M, identifier: String = "Identifier", consumer: (M) -> Unit) {
		if (message.getField(identifier) == this.identifier) {
			consumeAlways(message, consumer)
		}
	}

	private fun <M : BaseMessage> consumeAlways(message: M, consumer: (M) -> Unit) {
		consumer(message)
		notifySyncObject()
	}

	private fun consumeUnknown(fcpMessage: FcpMessage) {
		consumeUnknownMessage(fcpMessage)
		notifySyncObject()
	}

	private fun consumeClose(throwable: Throwable) {
		connectionFailureReason = throwable
		connectionClosed = true
		notifySyncObject()
	}

	override fun receivedNodeHello(fcpConnection: FcpConnection, nodeHello: NodeHello) {
		consume(nodeHello, consumer = this::consumeNodeHello)
	}

	protected open fun consumeNodeHello(nodeHello: NodeHello) {}

	override fun receivedCloseConnectionDuplicateClientName(fcpConnection: FcpConnection,
			closeConnectionDuplicateClientName: CloseConnectionDuplicateClientName) {
		connectionFailureReason = IOException("duplicate client name")
		connectionClosed = true
		notifySyncObject()
	}

	override fun receivedSSKKeypair(fcpConnection: FcpConnection, sskKeypair: SSKKeypair) {
		consume(sskKeypair, consumer = this::consumeSSKKeypair)
	}

	protected open fun consumeSSKKeypair(sskKeypair: SSKKeypair) {}

	override fun receivedPeer(fcpConnection: FcpConnection, peer: Peer) {
		consume(peer, consumer = this::consumePeer)
	}

	protected open fun consumePeer(peer: Peer) {}

	override fun receivedEndListPeers(fcpConnection: FcpConnection, endListPeers: EndListPeers) {
		consume(endListPeers, consumer = this::consumeEndListPeers)
	}

	protected open fun consumeEndListPeers(endListPeers: EndListPeers) {}

	override fun receivedPeerNote(fcpConnection: FcpConnection, peerNote: PeerNote) {
		consume(peerNote, consumer = this::consumePeerNote)
	}

	protected open fun consumePeerNote(peerNote: PeerNote) {}

	override fun receivedEndListPeerNotes(fcpConnection: FcpConnection, endListPeerNotes: EndListPeerNotes) {
		consume(endListPeerNotes, consumer = this::consumeEndListPeerNotes)
	}

	protected open fun consumeEndListPeerNotes(endListPeerNotes: EndListPeerNotes) {}

	override fun receivedPeerRemoved(fcpConnection: FcpConnection, peerRemoved: PeerRemoved) {
		consume(peerRemoved, consumer = this::consumePeerRemoved)
	}

	protected open fun consumePeerRemoved(peerRemoved: PeerRemoved) {}

	override fun receivedNodeData(fcpConnection: FcpConnection, nodeData: NodeData) {
		consume(nodeData, consumer = this::consumeNodeData)
	}

	protected open fun consumeNodeData(nodeData: NodeData) {}

	override fun receivedTestDDAReply(fcpConnection: FcpConnection, testDDAReply: TestDDAReply) {
		consume(testDDAReply, "Directory", this::consumeTestDDAReply)
	}

	protected open fun consumeTestDDAReply(testDDAReply: TestDDAReply) {}

	override fun receivedTestDDAComplete(fcpConnection: FcpConnection, testDDAComplete: TestDDAComplete) {
		consume(testDDAComplete, "Directory", this::consumeTestDDAComplete)
	}

	protected open fun consumeTestDDAComplete(testDDAComplete: TestDDAComplete) {}

	override fun receivedPersistentGet(fcpConnection: FcpConnection, persistentGet: PersistentGet) {
		consume(persistentGet, consumer = this::consumePersistentGet)
	}

	protected open fun consumePersistentGet(persistentGet: PersistentGet) {}

	override fun receivedPersistentPut(fcpConnection: FcpConnection, persistentPut: PersistentPut) {
		consume(persistentPut, consumer = this::consumePersistentPut)
	}

	protected open fun consumePersistentPut(persistentPut: PersistentPut) {}

	override fun receivedEndListPersistentRequests(fcpConnection: FcpConnection,
			endListPersistentRequests: EndListPersistentRequests) {
		consume(endListPersistentRequests, consumer = this::consumeEndListPersistentRequests)
	}

	protected open fun consumeEndListPersistentRequests(endListPersistentRequests: EndListPersistentRequests) {}

	override fun receivedURIGenerated(fcpConnection: FcpConnection, uriGenerated: URIGenerated) {
		consume(uriGenerated, consumer = this::consumeURIGenerated)
	}

	protected open fun consumeURIGenerated(uriGenerated: URIGenerated) {}

	override fun receivedDataFound(fcpConnection: FcpConnection, dataFound: DataFound) {
		consume(dataFound, consumer = this::consumeDataFound)
	}

	protected open fun consumeDataFound(dataFound: DataFound) {}

	override fun receivedAllData(fcpConnection: FcpConnection, allData: AllData) {
		consume(allData, consumer = this::consumeAllData)
	}

	protected open fun consumeAllData(allData: AllData) {}

	override fun receivedSimpleProgress(fcpConnection: FcpConnection, simpleProgress: SimpleProgress) {
		consume(simpleProgress, consumer = this::consumeSimpleProgress)
	}

	protected open fun consumeSimpleProgress(simpleProgress: SimpleProgress) {}

	override fun receivedStartedCompression(fcpConnection: FcpConnection, startedCompression: StartedCompression) {
		consume(startedCompression, consumer = this::consumeStartedCompression)
	}

	protected open fun consumeStartedCompression(startedCompression: StartedCompression) {}

	override fun receivedFinishedCompression(fcpConnection: FcpConnection, finishedCompression: FinishedCompression) {
		consume(finishedCompression, consumer = this::consumeFinishedCompression)
	}

	protected open fun consumeFinishedCompression(finishedCompression: FinishedCompression) {}

	override fun receivedUnknownPeerNoteType(fcpConnection: FcpConnection, unknownPeerNoteType: UnknownPeerNoteType) {
		consume(unknownPeerNoteType, consumer = this::consumeUnknownPeerNoteType)
	}

	protected open fun consumeUnknownPeerNoteType(unknownPeerNoteType: UnknownPeerNoteType) {}

	override fun receivedUnknownNodeIdentifier(fcpConnection: FcpConnection,
			unknownNodeIdentifier: UnknownNodeIdentifier) {
		consume(unknownNodeIdentifier, consumer = this::consumeUnknownNodeIdentifier)
	}

	protected open fun consumeUnknownNodeIdentifier(unknownNodeIdentifier: UnknownNodeIdentifier) {}

	override fun receivedConfigData(fcpConnection: FcpConnection, configData: ConfigData) {
		consume(configData, consumer = this::consumeConfigData)
	}

	protected open fun consumeConfigData(configData: ConfigData) {}

	override fun receivedGetFailed(fcpConnection: FcpConnection, getFailed: GetFailed) {
		consume(getFailed, consumer = this::consumeGetFailed)
	}

	protected open fun consumeGetFailed(getFailed: GetFailed) {}

	override fun receivedPutFailed(fcpConnection: FcpConnection, putFailed: PutFailed) {
		consume(putFailed, consumer = this::consumePutFailed)
	}

	protected open fun consumePutFailed(putFailed: PutFailed) {}

	override fun receivedIdentifierCollision(fcpConnection: FcpConnection, identifierCollision: IdentifierCollision) {
		consume(identifierCollision, consumer = this::consumeIdentifierCollision)
	}

	protected open fun consumeIdentifierCollision(identifierCollision: IdentifierCollision) {}

	override fun receivedPersistentPutDir(fcpConnection: FcpConnection, persistentPutDir: PersistentPutDir) {
		consume(persistentPutDir, consumer = this::consumePersistentPutDir)
	}

	protected open fun consumePersistentPutDir(persistentPutDir: PersistentPutDir) {}

	override fun receivedPersistentRequestRemoved(fcpConnection: FcpConnection,
			persistentRequestRemoved: PersistentRequestRemoved) {
		consume(persistentRequestRemoved, consumer = this::consumePersistentRequestRemoved)
	}

	protected open fun consumePersistentRequestRemoved(persistentRequestRemoved: PersistentRequestRemoved) {}

	override fun receivedSubscribedUSK(fcpConnection: FcpConnection, subscribedUSK: SubscribedUSK) {
		consume(subscribedUSK, consumer = this::consumeSubscribedUSK)
	}

	protected open fun consumeSubscribedUSK(subscribedUSK: SubscribedUSK) {}

	override fun receivedSubscribedUSKUpdate(fcpConnection: FcpConnection, subscribedUSKUpdate: SubscribedUSKUpdate) {
		consume(subscribedUSKUpdate, consumer = this::consumeSubscribedUSKUpdate)
	}

	protected open fun consumeSubscribedUSKUpdate(subscribedUSKUpdate: SubscribedUSKUpdate) {}

	override fun receivedPluginInfo(fcpConnection: FcpConnection, pluginInfo: PluginInfo) {
		consume(pluginInfo, consumer = this::consumePluginInfo)
	}

	protected open fun consumePluginInfo(pluginInfo: PluginInfo) {}

	override fun receivedPluginRemoved(fcpConnection: FcpConnection, pluginRemoved: PluginRemoved) {
		consume(pluginRemoved, consumer = this::consumePluginRemoved)
	}

	protected open fun consumePluginRemoved(pluginRemoved: PluginRemoved) {}

	override fun receivedFCPPluginReply(fcpConnection: FcpConnection, fcpPluginReply: FCPPluginReply) {
		consume(fcpPluginReply, consumer = this::consumeFCPPluginReply)
	}

	protected open fun consumeFCPPluginReply(fcpPluginReply: FCPPluginReply) {}

	override fun receivedPersistentRequestModified(fcpConnection: FcpConnection,
			persistentRequestModified: PersistentRequestModified) {
		consume(persistentRequestModified, consumer = this::consumePersistentRequestModified)
	}

	protected open fun consumePersistentRequestModified(persistentRequestModified: PersistentRequestModified) {}

	override fun receivedPutSuccessful(fcpConnection: FcpConnection, putSuccessful: PutSuccessful) {
		consume(putSuccessful, consumer = this::consumePutSuccessful)
	}

	protected open fun consumePutSuccessful(putSuccessful: PutSuccessful) {}

	override fun receivedPutFetchable(fcpConnection: FcpConnection, putFetchable: PutFetchable) {
		consume(putFetchable, consumer = this::consumePutFetchable)
	}

	protected open fun consumePutFetchable(putFetchable: PutFetchable) {}

	override fun receivedSentFeed(source: FcpConnection, sentFeed: SentFeed) {
		consume(sentFeed, consumer = this::consumeSentFeed)
	}

	protected open fun consumeSentFeed(sentFeed: SentFeed) {}

	override fun receivedBookmarkFeed(fcpConnection: FcpConnection, receivedBookmarkFeed: ReceivedBookmarkFeed) {
		consume(receivedBookmarkFeed, consumer = this::consumeReceivedBookmarkFeed)
	}

	protected open fun consumeReceivedBookmarkFeed(receivedBookmarkFeed: ReceivedBookmarkFeed) {}

	override fun receivedProtocolError(fcpConnection: FcpConnection, protocolError: ProtocolError) {
		consume(protocolError, consumer = this::consumeProtocolError)
	}

	protected open fun consumeProtocolError(protocolError: ProtocolError) {}

	override fun receivedMessage(fcpConnection: FcpConnection, fcpMessage: FcpMessage) {
		consumeUnknown(fcpMessage)
	}

	protected open fun consumeUnknownMessage(fcpMessage: FcpMessage) {}

	override fun connectionClosed(fcpConnection: FcpConnection, throwable: Throwable) {
		consumeClose(throwable)
	}

}
