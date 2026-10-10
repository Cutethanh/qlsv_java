package qlsv.security;

public class AuthException extends Exception {
    private static final long serialVersionUID = 1L;

    public enum Reason {
        INVALID_CREDENTIALS,
        LOCKED,
        REAUTH_FAILED
    }

    private final Reason reason;
    private final long minutesLeft;

    public AuthException(Reason reason, long minutesLeft) {
        super(reason.name());
        this.reason = reason;
        this.minutesLeft = minutesLeft;
    }

    public Reason getReason() { return reason; }

    public long getMinutesLeft() { return minutesLeft; }
}
