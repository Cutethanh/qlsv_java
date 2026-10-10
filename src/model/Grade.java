package qlsv.model;

import java.io.Serializable;

public class Grade implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String studentId;
    private final String subject;
    private double score;

    public Grade(String studentId, String subject, double score) {
        this.studentId = studentId;
        this.subject = subject;
        this.score = score;
    }

    public String getStudentId() { return studentId; }

    public String getSubject() { return subject; }

    public double getScore() { return score; }

    public void setScore(double score) { this.score = score; }

    public String getRank() {
        if (score >= 8.5) return "A - Giỏi";
        if (score >= 7.0) return "B - Khá";
        if (score >= 5.5) return "C - Trung bình";
        if (score >= 4.0) return "D - Trung bình yếu";
        return "F - Kém";
    }
}
