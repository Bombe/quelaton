# quelaton – a modern FCP client

The Freenet Client Protocol (FCP) is used by [Freenet](https://freenetproject.org/) clients that want to communicate with the Freenet node.

(If none of these words mean anything to you you’re probably in the wrong place.)

Quelaton was originally begun in Java but is now being converted and continued in Kotlin. All examples here are given in Kotlin but can be trivially converted to Java.

## Using quelaton

1. Obtain an instance of an `FcpClient`.
> `val fcpClient = DefaultFcpClient(threadPool, "localhost", 9481) { "client" }`

1. Create the command you want to use…
> `val nodeDataFuture = fcpClient.getNode()`

1. …set parameters…
> `    .includePrivate()`

1. …and execute the command.
> `    .execute()`

1. This will run the command in the given threadpool. The resulting `Future` can be asked for the result at any time; it will block until the command is finished.
> `println(nodeDataFuture.get().version)`
