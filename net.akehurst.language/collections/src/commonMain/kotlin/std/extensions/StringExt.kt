package std.extensions

val String.capitalise:String get() = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
val String.decapitalise:String get() = this.replaceFirstChar { it.lowercase() }