package net.pterodactylus.fcp.util

/**
 * Executes the given action if `this` is `true`.
 */
fun Boolean.ifTrue(action: () -> Unit) = apply { if (this) action() }
