package net.akehurst.language.comparisons.common

actual val kotlinTarget:String = "JVM"

/*
actual var myResourcesVfs: VfsFile = resourcesVfs

actual fun getPathsRecursive(dir: VfsFile, filter:(path:String)->Boolean) : List<VfsFile> {
    val rootFs = dir
    val files = runBlockingNoJs {  rootFs.listRecursiveSimple().filter { filter(it.path) } }
    return files
}*/
