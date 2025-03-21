/**
 * Copyright (C) 2024 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.kotlinx.komposite.common

import kotlin.reflect.KClass

class PrimitiveMapper<P, R>(
    val primitiveKlass: KClass<*>,
    val rawKlass: KClass<*>,
    val toRaw: (P) -> R,
    val toPrimitive: (R) -> P
) {
    companion object {
        fun <P : Any, R : Any> create(
            primitiveKlass: KClass<P>,
            rawKlass: KClass<R>,
            toRaw: (P) -> R,
            toPrimitive: (R) -> P
        ): PrimitiveMapper<P, R> {
            return PrimitiveMapper<P, R>(primitiveKlass, rawKlass, toRaw, toPrimitive)
        }
    }
}