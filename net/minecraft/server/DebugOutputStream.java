package net.minecraft.server;

import java.io.OutputStream;

public class DebugOutputStream extends RedirectStream {
    public DebugOutputStream(String name, OutputStream out) {
        super(name, out);
    }

    @Override
    protected void logLine(String message) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        StackTraceElement stackTraceElement = stackTraceElements[Math.min(3, stackTraceElements.length)];
        LOGGER.info("[{}]@.({}:{}): {}", this.name, stackTraceElement.getFileName(), stackTraceElement.getLineNumber(), message);
    }
}
