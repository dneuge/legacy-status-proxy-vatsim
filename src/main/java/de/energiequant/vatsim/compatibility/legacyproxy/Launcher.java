package de.energiequant.vatsim.compatibility.legacyproxy;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

/**
 * Launcher wrapping {@link Main} to perform basic system checks. Currently only
 * checks for broken class path elements which will probably cause class
 * initializations to fail.
 */
public class Launcher {

    private static final String LONG_OPTION_PREFIX = "--";
    public static final String OPTION_NAME_NO_GUI = "no-gui";
    public static final String OPTION_NAME_NO_CLASSPATH_CHECK = "no-classpath-check";

    private static final String BROKEN_CLASSPATH_MESSAGE = "It looks like your Java class path may be broken.\n"
        + "If startup fails confirm that you do not run the proxy\n"
        + "from a path containing any directory ending in an\n"
        + "exclamation mark. Move the proxy to another location\n"
        + "or rename the offending directory; Java is unable to\n"
        + "properly access resources from such locations.\n"
        + "This warning can be disabled by: " + LONG_OPTION_PREFIX + OPTION_NAME_NO_CLASSPATH_CHECK;

    /**
     * Paths can be broken by directories ending in an exclamation mark because Java
     * is unable to differentiate between exclamation marks in file paths and the
     * JAR access separator.
     * 
     * @see https://bugs.java.com/bugdatabase/view_bug.do?bug_id=4523159
     */
    private static final String[] BROKEN_PATHS = new String[] { "!/", "!\\" };

    public static void main(String[] args) throws Exception {
        if (shouldCheckClassPath(args)) {
            warnAboutBrokenClassPath(shouldUseGui(args));
        }

        Main.main(args);
    }

    private static boolean shouldCheckClassPath(String[] args) {
        String expectedDisableOption = LONG_OPTION_PREFIX + OPTION_NAME_NO_CLASSPATH_CHECK;
        for (String arg : args) {
            if (expectedDisableOption.equals(arg)) {
                return false;
            }
        }

        return true;
    }

    private static boolean shouldUseGui(String[] args) {
        String expectedDisableOption = LONG_OPTION_PREFIX + OPTION_NAME_NO_GUI;
        for (String arg : args) {
            if (expectedDisableOption.equals(arg)) {
                return false;
            }
        }

        return !GraphicsEnvironment.isHeadless();
    }

    private static void warnAboutBrokenClassPath(boolean shouldUseGui) {
        boolean mayBeBroken = checkClassPathMayBeBroken() || checkClassLoaderPathMayBeBroken();
        if (!mayBeBroken) {
            return;
        }

        System.err.println("======================================================");
        System.err.println(BROKEN_CLASSPATH_MESSAGE);
        System.err.println("======================================================");

        if (shouldUseGui) {
            JOptionPane.showMessageDialog(null, BROKEN_CLASSPATH_MESSAGE, "Startup may fail",
                JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Checks the class path as available from system properties for any issue with
     * {@link #BROKEN_PATHS}.
     * 
     * @return true if a possible issue is found, false if no issues could be
     *         detected
     */
    private static boolean checkClassPathMayBeBroken() {
        String[] classPathParts = ((String) System.getProperties().getOrDefault("java.class.path", ""))
            .split(Pattern.quote(File.pathSeparator));

        for (String part : classPathParts) {
            if (part.isEmpty()) {
                continue;
            }

            if (checkPathForPossibleClassLoaderIssue(part)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks the actual URLs used by the active class loader for any issue with
     * {@link #BROKEN_PATHS}.
     * 
     * @return true if a possible issue is found, false if no issues could be
     *         detected
     */
    private static boolean checkClassLoaderPathMayBeBroken() {
        ClassLoader classLoader = Launcher.class.getClassLoader();
        if (!(classLoader instanceof URLClassLoader)) {
            return false;
        }

        URL[] urls = ((URLClassLoader) classLoader).getURLs();
        for (URL url : urls) {
            if (checkPathForPossibleClassLoaderIssue(url.getFile())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the given file path may fail to load due to {@link #BROKEN_PATHS}.
     * 
     * @param filePath path to check; as read from class path or class loader
     * @return true if a possible issue is found, false if no issues could be
     *         detected
     */
    private static boolean checkPathForPossibleClassLoaderIssue(String filePath) {
        if (filePath == null) {
            return false;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            return false;
        }

        String canonicalPath = "";
        try {
            canonicalPath = file.getCanonicalPath();
        } catch (IOException ex) {
            // ignore
        }

        return containsAny(filePath, BROKEN_PATHS)
            || containsAny(file.getAbsolutePath(), BROKEN_PATHS)
            || containsAny(canonicalPath, BROKEN_PATHS);
    }

    private static boolean containsAny(String s, String... needles) {
        for (String needle : needles) {
            if (s.contains(needle)) {
                return true;
            }
        }

        return false;
    }
}
