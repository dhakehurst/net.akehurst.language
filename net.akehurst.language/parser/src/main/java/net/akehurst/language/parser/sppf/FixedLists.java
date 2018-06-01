package net.akehurst.language.parser.sppf;

import net.akehurst.language.core.sppt.FixedList;

public class FixedLists {

    @SuppressWarnings("rawtypes")
    public static FixedList EMPTY = new FixedListSimple<>();

    public static <T> FixedList<T> emptyList() {
        return FixedLists.EMPTY;
    }

    public static <T> FixedList<T> of(final T... elements) {
        return new FixedListSimple<>(elements);
    }
}
