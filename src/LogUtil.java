import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.*;

public final class LogUtil {
    private LogUtil() {}
    public static void init() {
        Logger root = Logger.getLogger("");
        for (Handler h : root.getHandlers()) { root.removeHandler(h); }

        ConsoleHandler ch = new ConsoleHandler();
        ch.setLevel(Level.INFO);
        ch.setFormatter(new Formatter() {
            private final DateTimeFormatter fmt =
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                            .withZone(ZoneId.systemDefault());

            @Override public String format(LogRecord r) {
                return String.format("[%s][%s] %s%n",
                        fmt.format(Instant.ofEpochMilli(r.getMillis())),
                        r.getLevel().getName(),
                        r.getMessage());
            }
        });
        root.addHandler(ch);
        root.setLevel(Level.INFO);            // default; use FINE for more details
    }
}
