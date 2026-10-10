package qlsv.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogEntry implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final LocalDateTime time;
    private final String username;
    private final String action;
    private final String detail;
    private final boolean success;
    private final LogLevel level;

    public LogEntry(LocalDateTime time, String username, String action, String detail,
                    boolean success, LogLevel level) {
        this.time = time;
        this.username = username;
        this.action = action;
        this.detail = detail;
        this.success = success;
        this.level = level;
    }

    public LocalDateTime getTime() { return time; }

    public String getFormattedTime() { return time.format(FORMAT); }

    public String getUsername() { return username; }

    public String getAction() { return action; }

    public String getDetail() { return detail; }

    public boolean isSuccess() { return success; }

    public LogLevel getLevel() { return level; }
}
