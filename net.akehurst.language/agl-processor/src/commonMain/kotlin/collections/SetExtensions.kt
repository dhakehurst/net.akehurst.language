package net.akehurst.language.collections

fun <T> Set<T>.transitveClosure(function: (T) -> Set<T>): Set<T> {
    var result:MutableSet<T> = this.toMutableSet()
    var newThings:MutableSet<T> = this.toMutableSet()
    var newStuff = true
    while (newStuff) {
        val temp = newThings.toSet()
        newThings.clear()
        for (nt:T in temp) {
            val s:Set<T> = function.invoke(nt)
            newThings.addAll(s)
        }
        newThings.removeAll(result)
        newStuff = result.addAll(newThings)
    }
    return result
}

fun <T> Set<T>.transitveClosure_old(function: (T) -> Set<T>): Set<T> {
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