package io.github.jcloix.jpvocab.util;

import com.amazonaws.services.lambda.runtime.Context;

public class LambdaLogger {
    private static LoggerWrapper logger;

    public static void init(Context context) {
        logger = new LoggerWrapper(context);
    }

    public static void log(String message) {
        if (logger != null) logger.log(message);
        else System.out.println(message); // fallback
    }

    private static class LoggerWrapper {
        private final Context context;
        LoggerWrapper(Context context) { this.context = context; }
        void log(String msg) { context.getLogger().log(msg); }
    }
}

