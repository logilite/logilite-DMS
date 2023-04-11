
package com.logilite.dms.uuid.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * External process run via command line for DMS convert Relational to UUID based file structure
 * </br>
 * </br>
 * NOTE: </br>
 * - Before execute this process class manually, read all below steps</br>
 * - Remove the package name,</br>
 * - Put this class to the exported files directory</br>
 * - Compile it</br>
 * - Execute java file</br>
 * - As a result it exports all the output files on same path</br>
 * </br>
 * If you want to change No of thread in Pool then you can change it
 * 
 * @author Sachin Bhimani @ Logilite Technologies
 */
public class ExternalProcess_DMSConvertRelationalToUUID
{

	public final SimpleDateFormat	SDF_WITH_TIME		= new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");

	private ArrayList<File>			fileList			= new ArrayList<File>();
	private CountDownLatch			latch;
	private String					currentDir			= "";

	// Change as per required
	private int						p_NoOfThreadInPool	= 1;

	public static void main(String[] args)
	{
		ExternalProcess_DMSConvertRelationalToUUID obj = new ExternalProcess_DMSConvertRelationalToUUID();
		obj.doProcess();
	}

	private void doProcess()
	{
		currentDir = System.getProperty("user.dir");
		System.out.println("The current working directory is " + currentDir + "\n");

		//
		File curDirFile = new File(currentDir);
		for (File file : curDirFile.listFiles())
		{
			if (file.isFile() && (file.getName().endsWith(".sh") || file.getName().endsWith(".bat")))
			{
				System.out.println(file.getName());
				file.setExecutable(true);
				fileList.add(file);
			}
		}
		//
		processFilesByCommandLine();
	} // doProcess

	void processFilesByCommandLine()
	{
		latch = new CountDownLatch(fileList.size());
		ExecutorService executor = Executors.newFixedThreadPool(p_NoOfThreadInPool);

		for (File file : fileList)
		{
			Runnable worker = new CommandExeThread(file, this);
			executor.submit(worker);
		}

		try
		{
			latch.await();
		}
		catch (InterruptedException e)
		{
			System.out.println("Error while processing files : " + e.getLocalizedMessage());
		}
		executor.shutdown();
	} // processFilesByCommandLine

	/**
	 * to concurrent execution shell command
	 * 
	 * @author Sachin Bhimani
	 */
	class CommandExeThread implements Runnable
	{
		File										file;
		ExternalProcess_DMSConvertRelationalToUUID	crtruProcess;

		public CommandExeThread(File file, ExternalProcess_DMSConvertRelationalToUUID dmsConvertRelationalToUUIDExternalProcess)
		{
			this.file = file;
			this.crtruProcess = dmsConvertRelationalToUUIDExternalProcess;
		}

		@Override
		public void run()
		{
			System.out.println("Executing file : " + file.getName());

			StringBuilder output = new StringBuilder();
			Date startTime = new Date();
			output.append("\nStartTime: ").append(SDF_WITH_TIME.format(startTime));
			output.append("\nFile: ").append(file.getAbsolutePath()).append("\n\n");

			try
			{

				file.setExecutable(true);
				ProcessBuilder processBuilder = new ProcessBuilder();

				// -- Linux --
				// Run a shell command
				// processBuilder.command("bash", "-c", "ls /home/SB/");

				// Run a shell script
				// processBuilder.command("path/to/hello.sh");

				// -- Windows --
				// Run a command
				// processBuilder.command("cmd.exe", "/c", "dir C:\\Users\\SB");

				// Run a bat file
				// processBuilder.command("C:\\Users\\SB\\hello.bat");

				String fileNameLog = file.getName() + "_" + Thread.currentThread().getId() + "_SB_OUTPUT.log";
				File outFileLog = new File(file.getParentFile() + File.separator + fileNameLog);
				if (!outFileLog.createNewFile())
				{
					System.out.println("Error Failed to create temporary file with name:" + fileNameLog);
				}

				String osName = System.getProperty("os.name");
				String cmd = (osName.toLowerCase().equals("linux") ? "sh " : "") + file.getAbsolutePath() + " |& tee -a " + file.getName() + "_OUTPUT.log";
				System.out.println("CMD ===> " + cmd);
				processBuilder.command(cmd);
				// processBuilder.command(file.getAbsolutePath(), " |& tee -a sachin_output.log");
				processBuilder.redirectOutput();// redirectErrorStream(false);
				// processBuilder.redirectOutput(Redirect.appendTo(outFileLog));
				Process process = processBuilder.start();

				// Process process = Runtime.getRuntime().exec(file.getAbsolutePath() + " >>
				// SB_Test.log ");
				// // Wait for execution completion
				// process.waitFor();

				BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

				String line;
				while ((line = reader.readLine()) != null)
				{
					output.append(line + "\n");
				}

				int exitVal = process.waitFor();
				if (exitVal == 0)
				{
					output.append("\nSuccess! \n");
				}
				else
				{
					output.append("\nAbnormal! \n");
				}
			}
			catch (IOException e)
			{
				output.append("\n\n\nIOException:\n" + file.getName() + "\n");
				output.append(e.getLocalizedMessage() + "\n\n" + e);
			}
			catch (InterruptedException e)
			{
				output.append("\n\n\nInterruptedException:\n" + file.getName() + "\n");
				output.append(e.getLocalizedMessage() + "\n\n" + e);
			}
			finally
			{
				crtruProcess.latch.countDown();
			}

			Date endTime = new Date();
			output.append("\n\nEndTime: ").append(SDF_WITH_TIME.format(endTime));
			output.append("\nDifference in Millis: " + (endTime.getTime() - startTime.getTime()));

			// Output file create
			try
			{
				String fileName = file.getName() + "_" + Thread.currentThread().getId() + ".out";
				File outFile = new File(file.getParentFile() + File.separator + fileName);
				if (!outFile.createNewFile())
				{
					System.out.println("Error Failed to create temporary file with name:" + fileName);
				}

				// append a list of lines, add new lines automatically
				Files.writeString(outFile.toPath(), output, StandardOpenOption.APPEND);
			}
			catch (IOException e)
			{
				System.out.println("Error creating output file : " + e.getLocalizedMessage());
			}
		} // run
	} // Class CommandExeThread

}
