package net.akehurst.datastore.gui.compose

import net.akehurst.datastore.api.DataItem
import net.akehurst.language.objectgraph.api.TypedObject

/** The result of a preview run by the host application: the first items read through the chain up to a part. */
sealed interface PreviewResult {
    /** @param more true if there are more items than were returned */
    data class Items(val items: List<DataItem>, val more: Boolean = false) : PreviewResult
    data class Failure(val message: String, val cause: Throwable? = null) : PreviewResult
}

/** The state of the preview of a part. */
sealed interface PreviewStatus {
    data object Idle : PreviewStatus
    data object Running : PreviewStatus
    data class Done(val result: PreviewResult) : PreviewStatus
}

/** A value shown in a preview, read one level at a time. */
sealed interface PreviewValue {
    /** a primitive value or null, shown as text */
    data class Text(val text: String) : PreviewValue

    /** an object whose properties can be shown */
    data class Structure(val obj: TypedObject, val typeName: String) : PreviewValue

    /** a collection whose elements can be shown */
    data class Collection(val obj: TypedObject, val size: Int, val typeName: String) : PreviewValue

    /** reading the value failed */
    data class Error(val message: String) : PreviewValue
}

/** A named value (a property, or an element of a collection as `[i]`) shown in a preview. */
data class PreviewEntry(val name: String, val value: PreviewValue)

/** Reads the values of objects for a preview, one level at a time, so that cyclic or deep data is only read as far as it is opened. */
object PreviewReader {
    /** longer texts are cut to this length */
    const val MAX_TEXT = 200

    /** at most this many elements of a collection are listed */
    const val MAX_ELEMENTS = 100

    private fun messageOf(ex: Throwable) = ex.message ?: ex::class.simpleName ?: "error"

    private fun typeNameOf(obj: TypedObject): String = try {
        obj.type.typeName.value
    } catch (ex: Exception) {
        "?"
    }

    private fun text(value: Any?): String {
        val s = when (value) {
            null -> "null"
            is ByteArray -> "${value.size} bytes"
            is String -> "\"$value\""
            else -> value.toString()
        }
        return if (s.length > MAX_TEXT) s.take(MAX_TEXT) + "…" else s
    }

    /** how 'obj' is shown: as text, or as something that can be opened */
    fun valueOf(obj: TypedObject): PreviewValue = try {
        when {
            obj.isNothing -> PreviewValue.Text("null")
            obj.type.isCollection -> {
                var n = 0
                obj.forEachIndexed { _, _ -> n++ }
                PreviewValue.Collection(obj, n, typeNameOf(obj))
            }

            obj.type.isPrimitive -> PreviewValue.Text(text(try { obj.accessor.valueOf(obj) } catch (_: Exception) { obj.self }))
            else -> PreviewValue.Structure(obj, typeNameOf(obj))
        }
    } catch (ex: Exception) {
        PreviewValue.Error(messageOf(ex))
    }

    /** the properties of 'obj' (one level); a property that cannot be read gives an Error */
    suspend fun propertiesOf(obj: TypedObject): List<PreviewEntry> {
        val names = try {
            obj.type.allResolvedProperty.keys.map { it.value }
        } catch (ex: Exception) {
            return listOf(PreviewEntry("?", PreviewValue.Error(messageOf(ex))))
        }
        return names.map { name ->
            val v = try {
                valueOf(obj.getPropertySuspend(name))
            } catch (ex: kotlin.coroutines.cancellation.CancellationException) {
                throw ex
            } catch (ex: Throwable) {
                PreviewValue.Error(messageOf(ex))
            }
            PreviewEntry(name, v)
        }
    }

    /** the elements of a collection (at most MAX_ELEMENTS), named `[i]` */
    fun elementsOf(obj: TypedObject): List<PreviewEntry> {
        val r = mutableListOf<PreviewEntry>()
        try {
            obj.forEachIndexed { i, v -> if (i < MAX_ELEMENTS) r.add(PreviewEntry("[$i]", valueOf(v))) }
        } catch (ex: Exception) {
            r.add(PreviewEntry("?", PreviewValue.Error(messageOf(ex))))
        }
        return r
    }
}
