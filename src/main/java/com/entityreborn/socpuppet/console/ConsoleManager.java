package com.entityreborn.socpuppet.console;

import com.entityreborn.socbot.Colors;
import com.entityreborn.socpuppet.App;
import com.entityreborn.socpuppet.extensions.ConsoleCommand;
import com.entityreborn.socpuppet.extensions.ExtensionManager;
import com.entityreborn.socpuppet.extensions.ExtensionTracker;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.*;
import java.util.logging.Formatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jline.console.ConsoleReader;
import jline.console.UserInterruptException;
import jline.console.completer.Completer;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

/**
 * A meta-class to handle all logging and input-related console improvements.
 * Based off of Glowstone's implementation.
 */
public final class ConsoleManager {
    private static ConsoleManager instance;
    
    public synchronized static ConsoleManager getInstance() {
        if (instance == null) {
            instance = new ConsoleManager(App.getInstance());
        }
        
        return instance;
    }
    
    private static final String CONSOLE_DATE = "HH:mm:ss";
    private static final String FILE_DATE = "yyyy/MM/dd HH:mm:ss";
    private static final Logger logger = Logger.getLogger("");

    private final Map<Colors, String> replacements = new EnumMap<>(Colors.class);
    private final Colors[] colors = Colors.values();

    private ConsoleReader reader;

    private boolean running = true;
    private boolean jLine = false;

    private ConsoleManager(App server) {
        // install Ansi code handler, which makes colors work on Windows
        AnsiConsole.systemInstall();

        for (Handler h : logger.getHandlers()) {
            logger.removeHandler(h);
        }

        // add log handler which writes to console
        logger.addHandler(new FancyConsoleHandler());

        // reader must be initialized before standard streams are changed
        try {
            reader = new ConsoleReader();
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Exception initializing console reader", ex);
        }
        reader.addCompleter(new CommandCompleter());
        reader.setHandleUserInterrupt(true);
        
        // set system output streams
        System.setOut(new PrintStream(new LoggerOutputStream(Level.INFO), true));
        System.setErr(new PrintStream(new LoggerOutputStream(Level.WARNING), true));

        // set up colorization replacements
        replacements.put(Colors.WHITE, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.WHITE).bold().toString());
        replacements.put(Colors.BLACK, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLACK).boldOff().toString());
        replacements.put(Colors.DARKBLUE, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLUE).boldOff().toString());
        replacements.put(Colors.DARKGREEN, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.GREEN).boldOff().toString());
        replacements.put(Colors.RED, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.RED).bold().toString());
        replacements.put(Colors.DARKRED, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.RED).boldOff().toString());
        replacements.put(Colors.DARKVIOLET, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.MAGENTA).boldOff().toString());
        replacements.put(Colors.ORANGE, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.YELLOW).bold().toString());
        replacements.put(Colors.YELLOW, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.YELLOW).boldOff().toString());
        replacements.put(Colors.LIGHTGREEN, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.GREEN).bold().toString());
        replacements.put(Colors.CYAN, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.CYAN).bold().toString());
        replacements.put(Colors.LIGHTCYAN, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.CYAN).boldOff().toString());
        replacements.put(Colors.BLUE, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLUE).bold().toString());
        replacements.put(Colors.VIOLET, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.MAGENTA).bold().toString());
        replacements.put(Colors.DARKGREY, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.BLACK).bold().toString());
        replacements.put(Colors.LIGHTGREY, Ansi.ansi().a(Ansi.Attribute.RESET).fg(Ansi.Color.WHITE).boldOff().toString());
        replacements.put(Colors.DEFAULT, Ansi.ansi().a(Ansi.Attribute.RESET).toString());
    }

    public void startConsole(boolean jLine) {
        this.jLine = jLine;

        Thread thread = new ConsoleCommandThread(this);
        thread.setName("ConsoleManager");
        //thread.setDaemon(true);
        thread.start();
    }

    public void startFile(String logfile) {
        File parent = new File(logfile).getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) {
            logger.warning("Could not create log folder: " + parent);
        }
        Handler fileHandler = new RotatingFileHandler(logfile);
        fileHandler.setFormatter(new DateOutputFormatter(FILE_DATE, false));
        logger.addHandler(fileHandler);
    }

    public void stop() {
        running = false;
        
        for (Handler handler : logger.getHandlers()) {
            handler.flush();
            handler.close();
        }
        
        App.shutdown();
    }

    private String colorize(String string) {
        if (!string.contains("\u0003")) {
            return string;  // no colors in the message
        } else if (!jLine || !reader.getTerminal().isAnsiSupported()) {
            return Colors.removeAll(string);  // color not supported
        } else {
            String c = "(,(1[0-5]|0?[0-9]))?";
            // colorize or strip all colors
            for (Colors color : colors) {
                if (!string.contains(color.toString())) {
                    continue;
                }
                if (replacements.containsKey(color)) {
                    string = string.replaceAll("(?i)" + color.toString() + c, replacements.get(color));
                } else {
                    string = string.replaceAll("(?i)" + color.toString() + c, "");
                }
            }
            return string + Ansi.ansi().reset().toString();
        }
    }

    private final Pattern regex = Pattern.compile("^(.+)(\\s+?(.*))?", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE | Pattern.COMMENTS);

    public void handleConsoleCommand(String string) {
	Matcher regexMatcher = regex.matcher(string);
        
        if (regexMatcher.matches()) {
            String trigger = regexMatcher.group(1);
            String args = regexMatcher.group(3);
            
            for (ExtensionTracker tracker : ExtensionManager.Get().getTrackers().values()) {
                if (tracker.getConsoleCommands().keySet().contains(trigger)) {
                    ConsoleCommand trig = tracker.getConsoleCommands().get(trigger);
                    System.out.println("Called " + trig.plugin() + ":" + trig.name());
                    
                    String response = trig.exec(trigger, args);
                    
                    if (response != null && !response.trim().isEmpty()) {
                        System.out.println(response);
                    }
                    
                    break;
                }
            }
        }
    }
    
    private class CommandCompleter implements Completer {
        @Override
        public int complete(final String buffer, int cursor, List<CharSequence> candidates) {
            try {
                List<String> completions = null; /*server.getScheduler().syncIfNeeded(new Callable<List<String>>() {
                    @Override
                    public List<String> call() throws Exception {
                        return server.getCommandMap().tabComplete(sender, buffer);
                    }
                });*/
                if (completions == null) {
                    return cursor;  // no completions
                }
                candidates.addAll(completions);

                // location to position the cursor at (before autofilling takes place)
                return buffer.lastIndexOf(' ') + 1;
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Error while tab completing", t);
                return cursor;
            }
        }
    }

    private class ConsoleCommandThread extends Thread {
        final ConsoleManager manager;

        public ConsoleCommandThread(ConsoleManager manager) {
            this.manager = manager;
        }
        
        @Override
        public void run() {
            String command = "";
            while (running) {
                try {
                    if (jLine) {
                        command = reader.readLine(">", null);
                    } else {
                        command = reader.readLine();
                    }

                    if (command == null || command.trim().length() == 0)
                        continue;
                    
                    handleConsoleCommand(command.trim());
                } catch (UserInterruptException ex) {
                    manager.stop();
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "Error while reading commands", ex);
                }
            }
        }
    }

    private static class LoggerOutputStream extends ByteArrayOutputStream {
        private final String separator = System.getProperty("line.separator");
        private final Level level;

        public LoggerOutputStream(Level level) {
            super();
            this.level = level;
        }

        @Override
        public synchronized void flush() throws IOException {
            super.flush();
            String record = this.toString();
            super.reset();

            if (record.length() > 0 && !record.equals(separator)) {
                logger.logp(level, "LoggerOutputStream", "log" + level, record);
            }
        }
    }

    private class FancyConsoleHandler extends ConsoleHandler {
        public FancyConsoleHandler() {
            setFormatter(new DateOutputFormatter(CONSOLE_DATE, true));
            setOutputStream(System.out);
        }

        @Override
        public synchronized void flush() {
            try {
                if (jLine) {
                    reader.print(ConsoleReader.RESET_LINE + "");
                    reader.flush();
                    super.flush();
                    try {
                        reader.drawLine();
                    } catch (Throwable ex) {
                        reader.getCursorBuffer().clear();
                    }
                    reader.flush();
                } else {
                    super.flush();
                }
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "I/O exception flushing console output", ex);
            }
        }
    }

    private static class RotatingFileHandler extends StreamHandler {
        private final SimpleDateFormat dateFormat;
        private final String template;
        private final boolean rotate;
        private String filename;

        public RotatingFileHandler(String template) {
            this.template = template;
            rotate = template.contains("%D");
            dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            filename = calculateFilename();
            updateOutput();
        }

        private void updateOutput() {
            try {
                setOutputStream(new FileOutputStream(filename, true));
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Unable to open " + filename + " for writing", ex);
            }
        }

        private void checkRotate() {
            if (rotate) {
                String newFilename = calculateFilename();
                if (!filename.equals(newFilename)) {
                    filename = newFilename;
                    // note that the console handler doesn't see this message
                    super.publish(new LogRecord(Level.INFO, "Log rotating to: " + filename));
                    updateOutput();
                }
            }
        }

        private String calculateFilename() {
            return template.replace("%D", dateFormat.format(new Date()));
        }

        @Override
        public synchronized void publish(LogRecord record) {
            if (!isLoggable(record)) {
                return;
            }
            checkRotate();
            super.publish(record);
            super.flush();
        }

        @Override
        public synchronized void flush() {
            checkRotate();
            super.flush();
        }
    }

    private class DateOutputFormatter extends Formatter {
        private final SimpleDateFormat date;
        private final boolean color;

        public DateOutputFormatter(String pattern, boolean color) {
            this.date = new SimpleDateFormat(pattern);
            this.color = color;
        }

        @Override
        @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
        public String format(LogRecord record) {
            StringBuilder builder = new StringBuilder();

            builder.append(date.format(record.getMillis()));
            builder.append(" [");
            builder.append(record.getLevel().getLocalizedName().toUpperCase());
            builder.append("] ");
            if (color) {
                builder.append(colorize(formatMessage(record)));
            } else {
                builder.append(formatMessage(record));
            }
            builder.append('\n');

            if (record.getThrown() != null) {
                StringWriter writer = new StringWriter();
                record.getThrown().printStackTrace(new PrintWriter(writer));
                builder.append(writer.toString());
            }

            return builder.toString();
        }
    }

}
