/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 * 
 * History
 *   15.05.2006(sieb, ohl): reviewed 
 */
package org.knime.core.node;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Date;
import java.util.HashMap;

import javax.xml.parsers.FactoryConfigurationError;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.varia.LevelRangeFilter;
import org.apache.log4j.varia.NullAppender;
import org.apache.log4j.xml.DOMConfigurator;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.core.util.FileUtil;
import org.knime.core.util.LogfileAppender;

/**
 * The general logger used to write info, warnings, errors , debugging, assert
 * messages, exceptions, and coding problems into the internal Log4J logger. The
 * loggers are configured by the <code>log4j.properties</code> file in the
 * root of the core package. The configuration can be overridden by specifying a
 * file in <code>-Dlog4j.configuration</code> (this is the standard log4j
 * behaviour). Furthermore, it is possible to add and remove additional writers
 * to this logger. Note, calling {@link #setLevelIntern(LEVEL)} does only effect
 * the minimum logging level of the default loggers. All other writers' levels
 * have to be set before hand.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public final class NodeLogger {
    /** The logging levels. */
    public static enum LEVEL {
        /** includes debug and more critical messages. */
        DEBUG,
        /** includes infos and more critical messages. */
        INFO,
        /** includes warnings and more critical messages. */
        WARN,
        /** includes error and more critical messages. */
        ERROR,
        /** includes fatal and more critical messages. */
        FATAL,
        /** includes all messages. */
        ALL
    }

    /** The default log file name, <i>knime.log</i>. */
    public static final String LOG_FILE = "knime.log";

    /** Assertions are on or off. */
    private static final boolean ASSERT;
    static {
        boolean flag;
        try {
            assert false;
            flag = false;
        } catch (AssertionError ae) {
            flag = true;
        }
        ASSERT = flag;
    }

    /** Keeps set of <code>NodeLogger</code> elements by class name as key. */
    private static final HashMap<String, NodeLogger> LOGGERS =
            new HashMap<String, NodeLogger>();

    /** Map of additionally added writers: Writer -> Appender. */
    private static final HashMap<Writer, WriterAppender> WRITER =
            new HashMap<Writer, WriterAppender>();

    /**
     * Maximum number of chars (10000) printed on <code>System.out</code> and
     * <code>System.err</code>.
     */
    private static final int MAX_CHARS = 10000;

    /** <code>System.out</code> log appender. */
    private static final Appender SOUT_APPENDER;

    /** Default log file appender. */
    private static final Appender FILE_APPENDER;

    private static final String LATEST_LOG4J_CONFIG =
            "log4j/log4j-" + KNIMEConstants.MAJOR + "." + KNIMEConstants.MINOR
                    + "." + KNIMEConstants.REV + ".xml";

    /**
     * Inits Log4J logger and appends <code>System.out</code>,
     * <code>System.err</code>, and <i>knime.log</i> to it.
     */
    static {
        assert (NodeLogger.class.getClassLoader().getResourceAsStream(
                LATEST_LOG4J_CONFIG) != null) : "log4j-configuration for "
                + "version " + KNIMEConstants.VERSION + " does not exist yet";
        File knimeDir = new File(KNIMEConstants.getKNIMEHomeDir());
        File log4j = new File(knimeDir, "log4j.xml");

        File legacyFile = new File(knimeDir, "log4j-1.1.0.xml");
        if (legacyFile.exists()) {
            if (!legacyFile.renameTo(log4j)) {
                System.err.println("There are two log4j configuration files"
                        + " in your KNIME home directory ('" 
                        + knimeDir.getAbsolutePath()
                        + " ') - or this directory is write-protected.");
                System.err.println("The 'log4j.xml' is the one actually used."
                        + " Merge changes you may have made"
                        + " to 'log4j-1.1.0.xml' and remove"
                        + " 'log4j-1.1.0.xml' to get rid of this message.");
            }
        }

        try {
            if (!log4j.exists() || checkPreviousLog4j(log4j)) {
                copyCurrentLog4j(log4j);
            }

            if (System.getProperty("log4j.configuration") == null) {
                DOMConfigurator.configure(log4j.toURI().toURL());
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (FactoryConfigurationError ex) {
            ex.printStackTrace();
        }

        // init root logger
        Logger root = Logger.getRootLogger();
        Appender a = root.getAppender("stderr");
        a = root.getAppender("stdout");
        if (a != null) {
            SOUT_APPENDER = a;
        } else {
            root.warn("Could not find 'stdout' appender");
            SOUT_APPENDER = new NullAppender();
        }

        a = root.getAppender("logfile");
        if (a != null) {
            FILE_APPENDER = a;
        } else {
            root.warn("Could not find 'logfile' appender");
            FILE_APPENDER = new NullAppender();
        }

        startMessage();
    }

    private static void copyCurrentLog4j(final File dest) throws IOException {
        InputStream in =
                NodeLogger.class.getClassLoader().getResourceAsStream(
                        LATEST_LOG4J_CONFIG);
        FileOutputStream out = new FileOutputStream(dest);
        FileUtil.copy(in, out);
        in.close();
        out.close();
    }

    /**
     * Checks if any of the previous shipped log4j-XMLs matches the current one
     * the user has in its local KNIME directory.
     * 
     * @param current the user's current file
     * @return <code>true</code> if it matches, <code>false</code> otherwise
     * @throws IOException if an I/O error occurs
     */
    private static boolean checkPreviousLog4j(final File current)
            throws IOException {
        FileInputStream reader = new FileInputStream(current);
        byte[] currentContents = new byte[(int)current.length()];
        reader.read(currentContents);
        reader.close();

        ClassLoader cl = NodeLogger.class.getClassLoader();

        for (int maj = KNIMEConstants.MAJOR; maj >= 1; maj--) {
            outer: for (int min = 0;; min++) {
                for (int rev = 0;; rev++) {
                    if ((maj == KNIMEConstants.MAJOR)
                            && (min == KNIMEConstants.MINOR)
                            && (rev == KNIMEConstants.REV)) {
                        continue;
                    }
                    InputStream in =
                            cl.getResourceAsStream("log4j/log4j-" + maj + "."
                                    + min + "." + rev + ".xml");
                    if (in == null) {
                        if (rev == 0) {
                            break outer;
                        } else {
                            break;
                        }
                    }

                    // compare the two files
                    in = new BufferedInputStream(in);
                    int i = 0;
                    boolean match = true;
                    while (true) {
                        byte b = (byte) in.read();
                        if ((i >= currentContents.length) && (b == -1)) {
                            break;
                        }

                        if (i >= currentContents.length) {
                            match = false;
                            break;
                        }

                        if (b == -1) {
                            match = false;
                            break;
                        }

                        if (currentContents[i] != b) {
                            match = false;
                            break;
                        }
                        i++;
                    }
                    in.close();
                    if (match) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Write start logging message to info logger of this class. */
    private static void startMessage() {
        NodeLogger l = getLogger(NodeLogger.class);
        l.info("#############################################################");
        l.info("#                                                           #");
        l.info("# "
                + ("Welcome to KNIME v" + KNIMEConstants.VERSION + " (Build "
                        + KNIMEConstants.BUILD_DATE
                        + ")                        ").substring(0, 62) + " #");
        l.info("# the Konstanz Information Miner                            #");
        l.info("# Based on Eclipse, www.eclipse.org                         #");
        l.info("# Uses: Java, GEF, Log4J                                    #");
        l.info("#                                                           #");
        l.info("#############################################################");
        l.info("#                                                           #");
        copyrightMessage();
        l.info("#                                                           #");
        l.info("#############################################################");
        if (FILE_APPENDER instanceof LogfileAppender) {
            l.info("# For more details see the KNIME log file:"
                    + "                  #");
            l.info("# " + ((LogfileAppender)FILE_APPENDER).getFile());
            l.info("#-----------------------------------------------"
                    + "------------#");
        }

        l.info("# logging date=" + new Date());
        l.info("# java.version=" + System.getProperty("java.version"));
        l.info("# java.vm.version=" + System.getProperty("java.vm.version"));
        l.info("# java.vendor=" + System.getProperty("java.vendor"));
        l.info("# os.name=" + System.getProperty("os.name"));
        l
                .info("# number of CPUs="
                        + Runtime.getRuntime().availableProcessors());
        l.info("# assertions=" + (ASSERT ? "on" : "off"));
        l.info("#############################################################");
    }

    /** Write copyright message. */
    private static void copyrightMessage() {
        NodeLogger l = getLogger(NodeLogger.class);
        l.info("# Copyright, 2003 - 2008                                    #");
        l.info("# University of Konstanz, Germany.                          #");
        l.info("# Chair for Bioinformatics and Information Mining           #");
        l.info("# Prof. Dr. Michael R. Berthold                             #");
        l.info("# and KNIME GmbH, Konstanz, Germany                         #");
        l.info("# website: http://www.knime.org                             #");
        l.info("# email: contact@knime.org                                  #");
    }

    /** The Log4J logger to which all messages are logged. */
    private final Logger m_logger;

    /**
     * Ignore configure warnings. This field is obsolete. It is a workaround to
     * avoid the flood of configure warning during startup. This field will be
     * deleted when the workflow manager is rewritten.
     */
    private static boolean isIgnoreConfigureWarning;
    
    /**
     * Ignore load data warnings, see {@link #setIgnoreLoadDataError(boolean)}
     * for details.
     */
    private static boolean isIgnoreLoadDataWarning;

    /**
     * Hidden default constructor, logger created by
     * <code>java.lang.Class</code>.
     * 
     * @param c The logger created by Class name.
     */
    private NodeLogger(final Class<?> c) {
        m_logger = Logger.getLogger(c);
    }

    /**
     * Hidden default constructor, logger created by just a name.
     * 
     * @param s The name of the logger.
     */
    private NodeLogger(final String s) {
        m_logger = Logger.getLogger(s);
    }

    /**
     * Creates a new <code>NodeLogger</code> for the given Class.
     * 
     * @param c The logger's Class.
     * @return A new logger for this Class.
     */
    public static NodeLogger getLogger(final Class<?> c) {
        String s = c.getName();
        if (LOGGERS.containsKey(s)) {
            return LOGGERS.get(s);
        } else {
            NodeLogger logger = new NodeLogger(c);
            LOGGERS.put(s, logger);
            return logger;
        }
    }

    /**
     * Creates a new <code>NodeLogger</code> for the given name.
     * 
     * @param s The logger's String.
     * @return A new logger for the given name.
     */
    public static NodeLogger getLogger(final String s) {
        if (LOGGERS.containsKey(s)) {
            return LOGGERS.get(s);
        } else {
            NodeLogger logger = new NodeLogger(s);
            LOGGERS.put(s, logger);
            return logger;
        }
    }

    /**
     * Write warning message into this logger.
     * 
     * @param o The object to print.
     */
    public void warn(final Object o) {
        if (isIgnoreConfigureWarning
                && o.toString().startsWith("Configure failed: ")) {
            return;
        }
        m_logger.warn(o);
    }

    /**
     * Write debugging message into this logger.
     * 
     * @param o The object to print.
     */
    public void debug(final Object o) {
        m_logger.debug(o);
    }

    /**
     * Write info message into this logger.
     * 
     * @param o The object to print.
     */
    public void info(final Object o) {
        if (isIgnoreConfigureWarning && o.toString().equals("reset")) {
            return;
        }
        m_logger.info(o);
    }

    /**
     * Write error message into the logger.
     * 
     * @param o The object to print.
     */
    public void error(final Object o) {
        m_logger.error(o);
    }

    /**
     * Write fatal error message into the logger.
     * 
     * @param o The object to print.
     */
    public void fatal(final Object o) {
        m_logger.fatal(o);
    }

    /**
     * Write warning message and throwable into this logger.
     * 
     * @param o The object to print.
     * @param t The exception to log, including its stack trace.
     */
    public void warn(final Object o, final Throwable t) {
        this.warn(o);
        String message = (t != null ? t.getMessage() : null);
        if (message != null && message.trim().length() > 0) {
            this.warn(message);
        }
        this.debug(o, t);
    }

    /**
     * Write debugging message and throwable into this logger.
     * 
     * @param o The object to print.
     * @param t The exception to log, including its stack trace.
     */
    public void debug(final Object o, final Throwable t) {
        m_logger.debug(o, t);
    }

    /**
     * Write info message and throwable into this logger.
     * 
     * @param o The object to print.
     * @param t The exception to log, including its stack trace.
     */
    public void info(final Object o, final Throwable t) {
        this.info(o);
        String message = (t != null ? t.getMessage() : null);
        if (message != null && message.trim().length() > 0) {
            this.info(message);
        }
        this.debug(o, t);
    }

    /**
     * Write error message and throwable into the logger.
     * 
     * @param o The object to print.
     * @param t The exception to log, including its stack trace.
     */
    public void error(final Object o, final Throwable t) {
        if (isIgnoreLoadDataWarning 
                && m_logger.getName().equals(WorkflowManager.class.getName())
                && t != null && t.getMessage() != null
                && t.getMessage().startsWith("No such data file: ")) {
            debug(o, t);
        } else {
            this.error(o);
            String message = (t != null ? t.getMessage() : null);
            if (message != null && message.trim().length() > 0) {
                this.error(message);
            }
            this.debug(o, t);
        }
    }

    /**
     * Check assert and write into logger if failed.
     * 
     * @param b The expression to check.
     * @param m Print this message if failed.
     */
    public void assertLog(final boolean b, final String m) {
        if (ASSERT) {
            m_logger.assertLog(b, "ASSERT " + m);
        }
    }

    /**
     * Check assertions on/off and write debug message into logger.
     * 
     * @param b The expression to check.
     * @param m Print this message if failed.
     * @param e AssertionError which as been fired.
     */
    public void assertLog(final boolean b, final String m,
            final AssertionError e) {
        if (ASSERT) {
            String message = (e != null ? e.getMessage() : null);
            if (message != null && message.trim().length() > 0) {
                m_logger.assertLog(b, "ASSERT " + m + " " + message);
            } else {
                m_logger.assertLog(b, "ASSERT " + m);
            }
            // for stacktrace
            if (!b & e != null) {
                m_logger.debug("ASSERT\t " + m, e);
            }
        }
    }

    /**
     * Writes CODING PROBLEM plus this message into this logger as error.
     * 
     * @param o The message to print.
     */
    public void coding(final Object o) {
        m_logger.error("CODING PROBLEM\t" + o);
    }

    /**
     * Writes <i>CODING PROBLEM</i> plus this message, as well as the the
     * message of the throwable into this logger as error and debug.
     * 
     * @param o The message to print.
     * @param t The throwable's message to print.
     */
    public void coding(final Object o, final Throwable t) {
        this.coding(o);
        String message = (t != null ? t.getMessage() : null);
        if (message != null && message.trim().length() > 0) {
            this.coding(message);
        }
        this.debug(o, t);
    }

    /**
     * Write fatal error message and throwable into the logger.
     * 
     * @param o The object to print.
     * @param t The exception to log, including its stack trace.
     */
    public void fatal(final Object o, final Throwable t) {
        this.fatal(o);
        String message = (t != null ? t.getMessage() : null);
        if (message != null && message.trim().length() > 0) {
            this.fatal(message);
        }
        this.debug(o, t);
    }

    /**
     * Adds a new {@link java.io.Writer} with the given level to this logger.
     * 
     * @param writer The writer to add.
     * @param minLevel The minimum level to output.
     * @param maxLevel The maximum level to output.
     */
    public static final void addWriter(final Writer writer,
            final LEVEL minLevel, final LEVEL maxLevel) {
        // remove the writer first if existent
        if (WRITER.containsKey(writer)) {
            Appender a = WRITER.get(writer);
            Logger.getRootLogger().removeAppender(a);
            WRITER.remove(writer);
        }
        // register new appender
        WriterAppender app =
                new WriterAppender(new PatternLayout("%-5p\t %c{1}\t %."
                        + MAX_CHARS + "m\n"), writer);
        app.setImmediateFlush(true);
        LevelRangeFilter filter = new LevelRangeFilter();
        filter.setLevelMin(transLEVEL(minLevel));
        filter.setLevelMax(transLEVEL(maxLevel));
        app.addFilter(filter);
        Logger.getRootLogger().addAppender(app);
        WRITER.put(writer, app);
    }

    /**
     * Removes the previously added {@link java.io.Writer} from the logger.
     * 
     * @param writer The Writer to remove.
     */
    public static final void removeWriter(final Writer writer) {
        Appender o = WRITER.get(writer);
        if (o != null) {
            if (o != FILE_APPENDER) {
                Logger.getRootLogger().removeAppender(o);
            }
        } else {
            getLogger(NodeLogger.class).warn(
                    "Could not delete writer: " + writer);
        }
    }

    /**
     * @param level minimum log level
     * @see #setLevel(NodeLogger.LEVEL)
     */
    @Deprecated
    public static void setLevelIntern(final LEVEL level) {
        setLevel(level);
    }
    
    /**
     * Sets an new minimum logging level for all internal appenders, that are,
     * log file, and <code>System.out</code> and <code>System.err</code>
     * appender. The maximum logging level stays <code>LEVEL.ALL</code> for
     * all appenders.
     * 
     * @param level new minimum logging level
     */
    public static void setLevel(final LEVEL level) {
        getLogger(NodeLogger.class).info(
                "Changing logging level to " + level.toString());
        LevelRangeFilter filter = new LevelRangeFilter();
        filter.setLevelMin(transLEVEL(level));
        filter.setLevelMax(transLEVEL(LEVEL.FATAL));
        FILE_APPENDER.clearFilters();
        // SERR_APPENDER.clearFilters();
        SOUT_APPENDER.clearFilters();
        FILE_APPENDER.addFilter(filter);
        // SERR_APPENDER.addFilter(filter);
        SOUT_APPENDER.addFilter(filter);
    }
    
    /**
     * Returns the minimum logging retrieved from the underlying Log4J logger.
     * @return minimum logging level
     */
    public LEVEL getLevel() {
        return transLevel(m_logger.getLevel());
    }
    
    /**   
     * Checks if debug logging level is enabled.
     * @return <code>true</code> if debug logging level is enabled, otherwise 
     *         <code>false</code>     
     */
    public boolean isDebugEnabled() {
        return m_logger.isDebugEnabled();
    }
    
    /**   
     * Checks if info logging level is enabled.
     * @return <code>true</code> if info logging level is enabled, otherwise 
     *         <code>false</code>     
     */
    public boolean isInfoEnabled() {
        return m_logger.isInfoEnabled();
    }
    
    /**
     * Returns <code>true</code> if the underlying Log4J logger is enabled 
     * for the given <code>level</code>.
     * @param level to test logging enabled
     * @return <code>true</code> if logging is enabled, otherwise 
     *         <code>false</code>
     */
    public boolean isEnabledFor(final LEVEL level) {
        return m_logger.isEnabledFor(transLEVEL(level));
    }

    /**
     * Translates this logging <code>LEVEL</code> into Log4J logging levels.
     * 
     * @param level the <code>LEVEL</code> to translate
     * @return the Log4J logging level
     */
    private static Level transLEVEL(final LEVEL level) {
        switch (level) {
        case DEBUG:
            return Level.DEBUG;
        case INFO:
            return Level.INFO;
        case WARN:
            return Level.WARN;
        case ERROR:
            return Level.ERROR;
        case FATAL:
            return Level.FATAL;
        default:
            return Level.ALL;
        }
    }
    
    /**
     * Translates Log4J logging level into this <code>LEVEL</code>.
     * 
     * @param level the Level to translate
     * @return this logging LEVEL
     */
    private static LEVEL transLevel(final Level level) {
        if (level == Level.DEBUG) {
            return LEVEL.DEBUG;
        } else if (level == Level.INFO) {
            return LEVEL.INFO;
        } else if (level == Level.WARN) {
            return LEVEL.WARN;
        } else if (level == Level.ERROR) {
            return LEVEL.ERROR;
        } else if (level == Level.FATAL) {
            return LEVEL.FATAL;
        } else {
            return LEVEL.ALL;
        }
    }

    /**
     * Ignore configure warnings. This field is obsolete. It is a workaround to
     * avoid the flood of configure warning during startup. This field will be
     * deleted when the workflow manager is rewritten.
     * 
     * @param value the isIgnoreConfigureWarning to set
     * @deprecated Obsolete, will be removed when WFM is rewritten.
     */
    @Deprecated
    public static void setIgnoreConfigureWarning(final boolean value) {
        // FIXME: Remove when WFM is rewritten.
        isIgnoreConfigureWarning = value;
    }
    
    /**
     * Ignore error messages that result from loading an exported workflow
     * without data. In the current version of KNIME, exporting a workflow
     * without the data simply deletes the 'data' directories in the workspace
     * dir (but the flags in the node stay set). This is a known bug which 
     * will be fixed as soon as the workflow manager is rewritten (before
     * KNIME 1.3).
     * 
     * <p>Setting this flag will cause the node logger to filter for one 
     * specific error message that is reported from the Node class and only
     * print this error as debug.
     * @param value If to ignore.
     * @deprecated Obsolete, will be removed when WFM is rewritten.
     */
    @Deprecated
    public static void setIgnoreLoadDataError(final boolean value) {
        // FIXME: Remove when WFM is rewritten.
        isIgnoreLoadDataWarning = value;
    }

}
