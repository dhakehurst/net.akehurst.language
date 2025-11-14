package net.akehurst.language.comparisons.common

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString

expect val kotlinTarget: String

//expect var myResourcesVfs: VfsFile

//expect fun runTest(block: suspend () -> Unit)

//expect fun getPathsRecursive(dir: VfsFile, filter: (path: String) -> Boolean): List<VfsFile>

fun getPathsRecursive(dir: Path, filter: (path: String) -> Boolean): List<Path> {
    return SystemFileSystem.list(dir).filter { filter.invoke(it.name) }
}

fun readResource(filePath: String): String {
    return SystemFileSystem.source(Path(filePath)).buffered().use { source ->
        source.readString()
    }
}