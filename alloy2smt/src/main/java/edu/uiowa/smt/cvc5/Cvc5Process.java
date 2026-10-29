package edu.uiowa.smt.cvc5;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * An interactive cvc5 process that receives SMT-LIB commands and returns their output.
 * The binary is found in this order: the system property cvc5.path, the environment
 * variable CVC5, the binary bundled in the jar for this platform, the PATH, and the bin
 * directory of the working directory.
 */
public class Cvc5Process
{
    public static final String OS       = System.getProperty("os.name");
    public static final String SEP      = File.separator;
    public static final String BIN_PATH = System.getProperty("user.dir") + SEP + "bin" + SEP;
    // printed by cvc5 for (echo "success") to mark the end of the output of a command
    private static final String END_MARKER = "\"success\"";

    private final Process process;
    private final Scanner scanner;
    private final OutputStream outputStream;
    private final StringBuilder smtCode;

    private Cvc5Process(Process process)
    {
        this.process        = process;
        this.scanner        = new Scanner(process.getInputStream());
        this.outputStream   = process.getOutputStream();
        this.smtCode        = new StringBuilder();
    }

    public static Cvc5Process start() throws Exception
    {
        ProcessBuilder processBuilder = getProcessBuilder();
        Process process = processBuilder.start();
        return new Cvc5Process(process);
    }

    public static String getCommand() throws Exception
    {
        return String.join(" ", getProcessBuilder().command());
    }

    public static File findBinary() throws Exception
    {
        String executable = onWindows() ? "cvc5.exe" : "cvc5";
        List<String> candidates = new ArrayList<>();
        if (System.getProperty("cvc5.path") != null)
        {
            candidates.add(System.getProperty("cvc5.path"));
        }
        if (System.getenv("CVC5") != null)
        {
            candidates.add(System.getenv("CVC5"));
        }
        File bundled = extractBundledBinary();
        if (bundled != null)
        {
            candidates.add(bundled.getPath());
        }
        String path = System.getenv("PATH");
        if (path != null)
        {
            for (String directory : path.split(File.pathSeparator))
            {
                candidates.add(directory + SEP + executable);
            }
        }
        candidates.add(BIN_PATH + executable);
        for (String candidate : candidates)
        {
            File file = new File(candidate);
            if (file.isFile() && file.canExecute())
            {
                return file;
            }
        }
        throw new Exception("The cvc5 binary was not found: this Alloy jar has no cvc5 binary for the platform " + getPlatform() +
                ". Set the system property cvc5.path or the environment variable CVC5 to its path, " +
                "or add it to the PATH. cvc5 can be downloaded from https://cvc5.github.io/downloads.html");
    }

    /**
     * @return the platform directory of the bundled binaries: os-arch, e.g. macos-arm64,
     * or null if no binary is bundled for this platform
     */
    static String getPlatform()
    {
        String os = OS.toLowerCase(Locale.US);
        String arch = System.getProperty("os.arch").toLowerCase(Locale.US);
        String osName = os.startsWith("windows") ? "windows" : os.startsWith("mac") ? "macos" : os.startsWith("linux") ? "linux" : null;
        String archName = arch.equals("amd64") || arch.equals("x86_64") ? "x86_64" :
                          arch.equals("aarch64") || arch.equals("arm64") ? "arm64" : null;
        return osName == null || archName == null ? null : osName + "-" + archName;
    }

    /**
     * Extracts the cvc5 binary bundled in the jar (cvc5/platform/cvc5) into a cache directory
     * in the temporary directory, unless it is already there.
     * @return the extracted binary, or null if there is no binary for this platform
     */
    private static File extractBundledBinary()
    {
        String platform = getPlatform();
        if (platform == null)
        {
            return null;
        }
        String executable = onWindows() ? "cvc5.exe" : "cvc5";
        ClassLoader loader = Cvc5Process.class.getClassLoader();
        URL url = loader.getResource("cvc5/" + platform + "/" + executable);
        if (url == null)
        {
            return null;
        }
        try
        {
            String version = "unknown";
            try (InputStream input = loader.getResourceAsStream("cvc5/VERSION"))
            {
                if (input != null)
                {
                    version = new String(input.readAllBytes(), StandardCharsets.UTF_8).trim();
                }
            }
            URLConnection connection = url.openConnection();
            long size = connection.getContentLengthLong();
            File directory = new File(System.getProperty("java.io.tmpdir"), "alloy-cvc5-" + version + "-" + platform);
            File binary = new File(directory, executable);
            if (binary.isFile() && binary.canExecute() && binary.length() == size)
            {
                return binary;
            }
            Files.createDirectories(directory.toPath());
            // another Alloy process may extract the binary at the same time
            Path temporary = Files.createTempFile(directory.toPath(), executable, ".tmp");
            try (InputStream input = connection.getInputStream())
            {
                Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
            }
            temporary.toFile().setExecutable(true);
            Files.move(temporary, binary.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return binary;
        }
        catch (IOException exception)
        {
            return null;
        }
    }

    private static ProcessBuilder getProcessBuilder() throws Exception
    {
        ProcessBuilder processBuilder = new ProcessBuilder();
        List<String> command = new ArrayList<>();
        command.add(findBinary().getAbsolutePath());
        // tell cvc5 the input language is smt2
        command.add("--lang");
        command.add("smt2");
        processBuilder.command(command);
        processBuilder.redirectErrorStream(true);
        return processBuilder;
    }

    /**
     * Sends the commands and returns their output, without the end marker.
     */
    public String sendCommand(String command) throws IOException
    {
        smtCode.append(command).append("\n");
        outputStream.write((command + "\n(echo \"success\")\n").getBytes());
        outputStream.flush();
        return receiveOutput();
    }

    private String receiveOutput()
    {
        StringBuilder output = new StringBuilder();
        while (scanner.hasNextLine())
        {
            String line = scanner.nextLine();
            if (line.equals(END_MARKER))
            {
                return output.toString().trim();
            }
            output.append(line).append("\n");
        }
        // the process terminated
        return output.toString().trim();
    }

    /**
     * @return the commands sent to the process so far
     */
    public String getSmtCode()
    {
        return smtCode.toString();
    }

    public void destroy()
    {
        process.destroyForcibly();
    }

    private static boolean onWindows()
    {
        return OS.toLowerCase(Locale.US).startsWith("windows");
    }
}
