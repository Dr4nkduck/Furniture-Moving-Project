package SWP301.Furniture_Moving_Project.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Ghi log theo chuẩn: THINK / STEP / WARN / ERROR */
public final class ThinkingLogger {
    private final Logger log;
    private ThinkingLogger(Class<?> clazz) { this.log = LoggerFactory.getLogger(clazz); }
    public static ThinkingLogger get(Class<?> clazz) { return new ThinkingLogger(clazz); }

    public void think(String msg, Object... args) { log.info("THINK: " + msg, args); }
    public void step (String msg, Object... args) { log.info("STEP : " + msg, args); }
    public void warn (String msg, Object... args) { log.warn("WARN : " + msg, args); }
    public void err  (String msg, Object... args) { log.error("ERROR: " + msg, args); }
}
