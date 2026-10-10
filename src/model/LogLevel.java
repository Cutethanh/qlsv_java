package qlsv.model;

public enum LogLevel {
    INFO("Thông tin"),
    WARNING("Cảnh báo"),
    ALERT("Nghiêm trọng");

    private final String label;

    LogLevel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
