package common;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Utility class that provides formatted system output functionality.
 * <p>
 * The {@code FormattedSystemOut} class offers methods to set up formatted output streams
 * for {@code System.out} and {@code System.err}. The formatted output includes the current
 * date, time, and thread name prepended to each printed message.
 * <p>
 * This class enhances the readability and debugging capabilities of the application by
 * providing contextual information alongside the printed messages.
 */
public class FormattedSystemOut {

    /**
     * Private constructor to prevent instantiation of the utility class.
     */
    private FormattedSystemOut() {
        // Private constructor to prevent instantiation
    }

    /**
     * Sets up formatted output streams for {@code System.out} and {@code System.err}.
     * <p>
     * This method replaces the default output streams with formatted print streams that
     * include the current date, time, and thread name prepended to each printed message.
     * The formatted output enhances the readability and debugging capabilities of the application.
     */
    public static void setupFormattedSysOut() {
        PrintStream formattedPrintStream = new PrintStream(System.out) {
            private final SimpleDateFormat dateFormat = new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss]");


            @Override
            public void print(String x) {
                // Synchronized formatting and output
                //synchronized (this) {
                    super.print(formatMessage(x));
                //}
            }

            @Override
            public void print(Object x) {
                // Synchronized formatting and output
                //synchronized (this) {
                    super.print(formatMessage(String.valueOf(x)));
                //}
            }

            private String formatMessage(String message) {
                // Include thread name in the message
                return dateFormat.format(new Date()) + " [" + Thread.currentThread().getName() + "] " + message;
            }
        };

        // Redirect System.out and System.err to the formatted print stream
        System.setOut(formattedPrintStream);
        System.setErr(formattedPrintStream);
    }
}