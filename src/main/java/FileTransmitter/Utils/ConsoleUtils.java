package FileTransmitter.Utils;

import java.io.Console;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.Formatter;

public class ConsoleUtils
{
    private static final Console _sysConsole;
    private static final PrintWriter _writer;

    private static final DateFormat _dateFormat;

    static {
        _sysConsole = System.console();
        if (_sysConsole != null) {
            _writer = _sysConsole.writer();
        } else {
            _writer = new PrintWriter(System.out);
        }
        _dateFormat = DateFormat.getDateTimeInstance();
    }

    //==========================================================================

    public static void tprintf(String msg, Object ...args) {
        Formatter formatter = new Formatter();
        _writer.format("[%s] - %s\n", stime(), formatter.format(msg, args).toString());
        _writer.flush();
    }

    public static void printf(String msg, Object ...args) {
        _writer.format(msg, args);
        _writer.flush();
    }

    public static void newline() {
        _writer.println();
        _writer.flush();
    }

    public static String readLine() {
        return readLine(">>> ");
    }

    public static String readLine(String fmt, Object ...args) {
        if (_sysConsole == null) {
            return null;
        }
        return _sysConsole.readLine(fmt, args);
    }

    public static char[] readPassword() {
        return readPassword("[password] >>> ");
    }

    public static char[] readPassword(String fmt, Object ...args) {
        if (_sysConsole == null) {
            return null;
        }
        return _sysConsole.readPassword(fmt, args);
    }

    private static String stime() {
        return _dateFormat.format(new Date());
    }

}