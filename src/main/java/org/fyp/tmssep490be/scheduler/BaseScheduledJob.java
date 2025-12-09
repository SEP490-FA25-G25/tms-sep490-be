package org.fyp.tmssep490be.scheduler;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public abstract class BaseScheduledJob {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    protected void logJobStart(String jobName) {
        log.info("╔═══════════════════════════════════════════════════════════════╗");
        log.info("║ {} STARTED at {}", padRight(jobName, 30), LocalDateTime.now().format(FORMATTER));
        log.info("╚═══════════════════════════════════════════════════════════════╝");
    }

    protected void logJobEnd(String jobName, int recordsProcessed) {
        log.info("╔═══════════════════════════════════════════════════════════════╗");
        log.info("║ {} COMPLETED: {} records processed", padRight(jobName, 30), recordsProcessed);
        log.info("║ Finished at: {}", LocalDateTime.now().format(FORMATTER));
        log.info("╚═══════════════════════════════════════════════════════════════╝");
    }

    protected void logJobEnd(String jobName, String message) {
        log.info("╔═══════════════════════════════════════════════════════════════╗");
        log.info("║ {} COMPLETED: {}", padRight(jobName, 30), message);
        log.info("║ Finished at: {}", LocalDateTime.now().format(FORMATTER));
        log.info("╚═══════════════════════════════════════════════════════════════╝");
    }

    protected void logJobError(String jobName, Exception e) {
        log.error("╔═══════════════════════════════════════════════════════════════╗");
        log.error("║ {} FAILED", padRight(jobName, 48));
        log.error("║ Error: {}", e.getMessage());
        log.error("║ Failed at: {}", LocalDateTime.now().format(FORMATTER));
        log.error("╚═══════════════════════════════════════════════════════════════╝", e);
    }

    protected void logJobInfo(String message) {
        log.info("║ {}", message);
    }

    protected void logJobWarning(String message) {
        log.warn("║ WARNING: {}", message);
    }

    private String padRight(String s, int n) {
        if (s.length() >= n) {
            return s.substring(0, n);
        }
        return String.format("%-" + n + "s", s);
    }
}
