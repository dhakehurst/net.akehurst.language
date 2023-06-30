package net.akehurst.language.comparisons.common

import korlibs.io.file.VfsFile
import korlibs.io.file.std.StandardPaths
import korlibs.io.file.std.localVfs
import korlibs.io.file.std.resourcesVfs
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

actual var myResourcesVfs: VfsFile = localVfs("${StandardPaths.cwd}/kotlin")

actual fun runTest(block: suspend () -> Unit):dynamic = GlobalScope.promise{ block() }