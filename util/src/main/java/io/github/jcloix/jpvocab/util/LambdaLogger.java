package io.github.jcloix.jpvocab.util;

import com.amazonaws.services.lambda.runtime.Context;
import java.io.PrintWriter;
import java.io.StringWriter;

public class LambdaLogger {
    private static LoggerWrapper logger;

    /** Call this in handleRequest to initialize with Lambda Context */
    public static void init(Context context) {
        logger = new LoggerWrapper(context);
    }

    /** Regular logging after init */
    public static void log(String message) {
        if (logger != null) logger.log(message);
        else System.out.println(message); // fallback
    }

    /** Log an exception with stack trace */
    public static void log(String message, Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        log(message + "\n" + sw);
    }

    /** Logging usable in static blocks, before Context exists */
    public static void logStatic(String message) {
        System.out.println(message); // just fallback to stdout
    }

    private static class LoggerWrapper {
        private final Context context;
        LoggerWrapper(Context context) { this.context = context; }
        void log(String msg) { context.getLogger().log(msg); }
    }
}
