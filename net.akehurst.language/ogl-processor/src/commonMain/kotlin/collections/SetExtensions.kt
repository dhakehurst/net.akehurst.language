package net.akehurst.language.collections

fun <T> Set<T>.transitveClosure(function: (T) -> Set<T>): Set<T> {
    var result:MutableSet<T> = this.toMutableSet()
    var oldResult:MutableSet<T> = mutableSetOf<T>()
    while (!oldResult.containsAll(result)) {
        oldResult = result.toMutableSet()
        for (nt:T in oldResult) {
            val s:Set<T> = function.invoke(nt)
            result.addAll(s)
        }
    }
    return result
}