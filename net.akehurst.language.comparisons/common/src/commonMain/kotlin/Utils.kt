package net.akehurst.language.comparisons.common

import korlibs.io.file.VfsFile

expect val kotlinTarget:String

expect var myResourcesVfs: VfsFile

expect fun runTest(block: suspend () -> Unit)

expect fun getPathsRecursive(dir: VfsFile, filter:(path:String)->Boolean) : List<VfsFile>