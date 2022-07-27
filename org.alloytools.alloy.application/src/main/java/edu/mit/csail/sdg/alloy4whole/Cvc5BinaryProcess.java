package edu.mit.csail.sdg.alloy4whole;

import edu.mit.csail.sdg.alloy4.A4Preferences;
import edu.mit.csail.sdg.alloy4.Util;
import edu.mit.csail.sdg.alloy4.WorkerEngine;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Scanner;

public class Cvc5BinaryProcess
{
  public static final String OS = System.getProperty("os.name");
  public static final String SEP = File.separator;
  public static final String BIN_PATH = SimpleGUI.alloyHome(null) + SEP + "binary" + SEP;
  public static String blockModelOption = A4Preferences.CvcLiterals;

  private Process process;
  private Scanner scanner;
  private OutputStream outputStream;
  private StringBuilder smtCode;

  private Cvc5BinaryProcess(Process process)
  {
    this.process = process;
    this.scanner = new Scanner(process.getInputStream());
    this.outputStream = process.getOutputStream();
    this.smtCode = new StringBuilder();
  }

  public static Cvc5BinaryProcess start(WorkerEngine.WorkerCallback workerCallback) throws Exception
  {
    ProcessBuilder processBuilder = getProcessBuilder();

    workerCallback.callback(new Object[] {
        "", "Executing command: " + String.join(" ", processBuilder.command()) + "\n\n"});
    Process process = processBuilder.start();

    return new Cvc5BinaryProcess(process);
  }

  private String runCvc5() throws Exception
  {
    ProcessBuilder processBuilder = getProcessBuilder();

    String tempDirectory = System.getProperty("java.io.tmpdir");
    // save the smt file
    File smtFile = File.createTempFile("tmp", ".smt2", new File(tempDirectory));
    Formatter formatter = new Formatter(smtFile);
    formatter.format("%s", smtCode.toString());
    formatter.close();
    processBuilder.command().add(smtFile.getAbsolutePath());

    // start the process
    Process process = processBuilder.start();
    BufferedReader bufferedReader =
        new BufferedReader(new InputStreamReader(process.getInputStream()));
    StringBuilder stringBuilder = new StringBuilder();
    String line;
    while ((line = bufferedReader.readLine()) != null)
    {
      stringBuilder.append(line);
      stringBuilder.append("\n");
    }
    process.destroyForcibly();
    return stringBuilder.toString();
  }

  private static ProcessBuilder getProcessBuilder() throws Exception
  {
    String cvc5;

    if (Util.onWindows())
    {
      cvc5 = BIN_PATH + "cvc5_win64.exe";
    }
    else if (Util.onMac())
    {
      cvc5 = BIN_PATH + "cvc5_mac";
    }
    else if (OS.startsWith("Linux"))
    {
      cvc5 = BIN_PATH + "cvc5_linux";
    }
    else
    {
      throw new Exception("No cvc5 binary available for the OS: " + OS);
    }

    File cvc5Binary = new File(cvc5);

    if (!cvc5Binary.exists())
    {
      throw new Exception("cvc5 binary does not exist at: " + cvc5);
    }
    if (!cvc5Binary.canExecute())
    {
      throw new Exception("cvc5 binary cannot be executed at: " + cvc5);
    }

    ProcessBuilder processBuilder = new ProcessBuilder();
    List<String> command = new ArrayList<>();

    command.add(cvc5);

    // tell cvc5 the input language is smt2
    command.add("--lang=smtlib2.6");
    //  command.add("--block-models="  + blockModelOption);
    //        command.add("--print-success");

    processBuilder.command(command);

    processBuilder.redirectErrorStream(true);
    return processBuilder;
  }

  public String sendCommand(String command) throws IOException
  {
    outputStream.write((command + "\n").getBytes());
    smtCode.append(command + "\n");
    try
    {
      outputStream.flush();
    }
    catch (IOException ioException)
    {
      if (ioException.getMessage().toLowerCase().contains("pipe"))
      {
        System.out.println(smtCode.toString());
        try
        {
          String error = runCvc5();
          throw new IOException(error);
        }
        catch (Exception exception)
        {
          exception.printStackTrace();
        }
      }
      throw ioException;
    }
    return receiveOutput();
  }

  private String receiveOutput() throws IOException
  {
    String line = "";
    outputStream.write(("(echo \"success\")\n").getBytes());
    outputStream.flush();

    StringBuilder output = new StringBuilder();

    while (scanner.hasNextLine())
    {
      line = scanner.nextLine();

      if (line.equals("\"success\""))
      {
        return output.toString().trim();
      }
      output.append(line).append("\n");
    }
    return line;
  }

  public void destroy()
  {
    process.destroyForcibly();
  }
}
