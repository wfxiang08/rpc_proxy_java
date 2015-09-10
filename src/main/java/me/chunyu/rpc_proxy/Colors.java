package me.chunyu.rpc_proxy;

import static org.fusesource.jansi.Ansi.ansi;

public class Colors {

    /**
     *  将content标记为红色
     * @param content
     * @return
     */
    public static String red(String content) {
        return ansi().render("@|red " + content + " |@").toString();
    }


    public static String green(String content) {
        return ansi().render("@|green " + content + " |@").toString();
    }


    public static String blue(String content) {
        return ansi().render("@|blue " + content + " |@").toString();
    }


    public static String magenta(String content) {
        return ansi().render("@|magenta " + content + " |@").toString();
    }

    public static String cyan(String content) {
        return ansi().render("@|cyan " + content + " |@").toString();
    }
}
