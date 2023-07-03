package net.akehurst.language.comparisons.common

import korlibs.io.file.VfsFile
import korlibs.io.file.std.StandardPaths
import korlibs.io.file.std.localVfs
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise

actual val kotlinTarget:String = "JS"

actual var myResourcesVfs: VfsFile = localVfs("${StandardPaths.cwd}/kotlin")

actual fun runTest(block: suspend () -> Unit): dynamic = GlobalScope.promise { block() }


actual fun getPathsRecursive(dir: VfsFile, filter: (path: String) -> Boolean): List<VfsFile> {
    val files = mutableListOf<String>()
    walk(dir.absolutePath) { path, stat -> if (filter(path)) files.add(path) }
    return files.map { dir[it] }
}

fun walk(dir: String, callback: (path: String, stat: dynamic) -> Unit) {
    val fs = js("require('fs')")
    val path = js("require('path')")
    for(name in fs.readdirSync(dir)) {
        val filePath = path.join(dir, name)
        val stat = fs.statSync(filePath)
        if (stat.isFile()) {
            callback(filePath, stat)
        } else if (stat.isDirectory()) {
            walk(filePath, callback)
        }
    }
}