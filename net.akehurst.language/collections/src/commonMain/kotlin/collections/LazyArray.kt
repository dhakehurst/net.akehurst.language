/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.collections

internal fun <T> lazyArray(size:Int,accessor: (Int) -> T) = LazyArray(size,accessor)

internal class LazyArray<T>(size:Int, val accessor: (Int) -> T)  {

    val arr = arrayOfNulls<Any?>(size) as Array<T?>

    operator fun get(index: Int): T {
        return arr[index] ?: {
            val v = accessor.invoke(index)
            arr[index] = v
            v
        }.invoke()
    }
}