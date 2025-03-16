package std.extensions

infix fun Boolean.implies(other: Boolean): Boolean = this.not() || other