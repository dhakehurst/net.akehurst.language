/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl

import kotlin.reflect.KProperty

// TODO: remove this pre release
// This a workaround for the debugger
// see [https://youtrack.jetbrains.com/issue/KTIJ-1170#focus=Comments-27-4433190.0-0]
//internal operator fun <T> Lazy<T>.getValue(thisRef: Any?, property: KProperty<*>) = value