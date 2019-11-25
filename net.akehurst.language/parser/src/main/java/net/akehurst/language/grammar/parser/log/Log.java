package net.akehurst.language.grammar.parser.log;

public class Log {

    public static boolean on = false;

    public static void trace(final String text, final Object... args) {
        final String s = String.format(text, args);
        System.out.print(s);
    }

    public static void traceln(final String text, final Object... args) {
        final String s = String.format(text, args);
        System.out.println(s);
    }
}
