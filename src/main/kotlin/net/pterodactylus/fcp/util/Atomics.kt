package net.pterodactylus.fcp.util

import java.util.concurrent.atomic.*
import kotlin.reflect.*

fun <T> atomic(initialValue: T) = Atomic<Any, T>(initialValue)

class Atomic<in R : Any, T>(initialValue: T) {
	private val value = AtomicReference<T>(initialValue)
	operator fun getValue(thisRef: R, property: KProperty<*>): T = value.get()
	operator fun setValue(thisRef: R, property: KProperty<*>, value: T) = this.value.set(value)
}

fun <T> atomicObservable(initialValue: T, consumer: (T) -> Unit) = AtomicObservable<Any, T>(initialValue, consumer)

class AtomicObservable<in R : Any, T>(initialValue: T, private val consumer: (T) -> Unit) {
	private val value = AtomicReference<T>(initialValue)
	operator fun getValue(thisRef: R, property: KProperty<*>): T = value.get()
	operator fun setValue(thisRef: R, property: KProperty<*>, value: T) {
		this.value.set(value)
		consumer(value)
	}
}

