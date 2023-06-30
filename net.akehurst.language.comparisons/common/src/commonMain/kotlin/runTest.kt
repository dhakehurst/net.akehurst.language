package net.akehurst.language.comparisons.common

import korlibs.io.file.VfsFile

expect var myResourcesVfs: VfsFile

expect fun runTest(block: suspend () -> Unit)