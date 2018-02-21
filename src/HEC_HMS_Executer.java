import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;

public class HEC_HMS_Executer {

	// Hardcoded paths needed to run HEC_HMS and HEC_DSSVue

	private static String configFileForDarang =  "Darang17.basin";
	private static String configFileSourceForSiron =  "SourceDarang17.basin";

	private static String metFileForDarang =  "DarangMet.met";
	private static String metFileSourceForDarang =  "SourceDarangMet.met";


	public  static String workingFolderPAthForDarang= "C:\\Users\\Dragos\\Desktop\\dizertatie\\Darang\\";

	private static String workingFolderPathForHEC_HMS = "D:\\Program Files (x86)\\HEC\\HEC-HMS\\4.2.1\\";
	private static String runScriptForHEC_HMS = "run_compute.cmd";

	private static String workingFolderPathForHEC_DSSVue = "C:\\Users\\Dragos\\Documents\\Water models\\HEC-DSSVue 2.0.1\\";

	private static String runScriptForHEC_DSSVueOutlet1 = "ReadDarangFlowOutlet1.cmd";
	private static String runScriptForHEC_DSSVueOutlet1Observed = "ReadDarangFlowOutlet1Observed.cmd";

	private static String runScriptForHEC_DSSVueJ55 = "ReadDarangFlowJ55.cmd";
	private static String runScriptForHEC_DSSVueJ55Observed = "ReadDarangFlowJ55Observed.cmd";

	public static boolean DEBUG = false;

	double[] configParametersValuesForDarang = null;


	// HARDCODDED lines that will be changed in "Siron_sub_basin.basin" with new values from each generation

	public static final String[] configSubasinsForDarang = { "W300", "W310", "W330", "W350", "W400","W440" };
	public static final String[] configParametersNamesForDarang = {"     Curve Number: ", "     Initial Abstraction:", "     Snyder Tp: "};

	public static Vector<Double> flowDataOutlet1Observed = null;
	public static Vector<Double> flowDataJ55Observed = null;

	boolean showPlot = false;


	// In some rare cases the .met file is broken and needs to be replaced. The only usage of this counter is for statistics
	public static int counterOfMetFileRegeneration = 0;


	// Create a new exception that will be caught in case the ".met" file is broken
	public class ExecCommandExecption extends Exception
	{
		public ExecCommandExecption(String message)
		{
			super(message);
		}
	}


	// constructor which receives the following parameters: parameters to run simulation on HEC-HMS, 
	// and a showPlot variable to determine if is necessary to create a plot of the output
	public  HEC_HMS_Executer(double[] configParametersValuesForDarang, boolean showPlot)
	{
		this.configParametersValuesForDarang = configParametersValuesForDarang;
		this.showPlot = showPlot;


		try {
			if(flowDataOutlet1Observed == null)
			{
				Vector<String> outputStreamFromHEC_DSSVueObserved =
						execCommand(workingFolderPathForHEC_DSSVue, workingFolderPathForHEC_DSSVue + runScriptForHEC_DSSVueOutlet1Observed);
				flowDataOutlet1Observed = parseDSSOutput(outputStreamFromHEC_DSSVueObserved);
			}

			if(flowDataJ55Observed == null)
			{
				Vector<String> outputStreamFromHEC_DSSVueObserved =
						execCommand(workingFolderPathForHEC_DSSVue, workingFolderPathForHEC_DSSVue + runScriptForHEC_DSSVueJ55Observed);
				flowDataJ55Observed = parseDSSOutput(outputStreamFromHEC_DSSVueObserved);
			}

		} catch (IOException e) 
		{
			e.printStackTrace();
		} catch (ExecCommandExecption e) 
		{
			e.printStackTrace();
		}

	}




	// The run function will run HEC-HMS, HED-DSSVue to create a simulation and obtain data
	// also if showPlot is set, will create a plot of the data after finnishing

	public synchronized double[] run()
	{

		double rmse[] = new double[2];

		try {


			// update configuration file "Siron_sub_basin.basin" with the new values of the parameters
			// The values should match the order in configParametersNamesForSiron list


			updateConfigurationFile();

			// execute HEC_HMS and in case the Met_1.met or Siron_sub_basin.basin are broken recreate them
			boolean runWithoutError = true;
			do 
			{
				try
				{

					execCommand(workingFolderPathForHEC_HMS, workingFolderPAthForDarang + runScriptForHEC_HMS);


				} catch (ExecCommandExecption e) {

					// Met file is broken need to fix it and try again
					// Recreate it from template Met_1_My.met

					Path metTemplateSource = Paths.get(workingFolderPAthForDarang, metFileSourceForDarang);
					Path metFileDestination = Paths.get(workingFolderPAthForDarang, metFileForDarang);

					//update configuration file

					updateConfigurationFile();

					CopyOption[] options = new CopyOption[]{
							StandardCopyOption.REPLACE_EXISTING,
							StandardCopyOption.COPY_ATTRIBUTES
					}; 

					// overwrite the met file  with the met file source (template)
					Files.copy(metTemplateSource, metFileDestination, options);
					e.printStackTrace();

					runWithoutError = false;
					counterOfMetFileRegeneration++;
				}
			}while(!runWithoutError);

			rmse[0] = computeObjective(runScriptForHEC_DSSVueOutlet1,flowDataOutlet1Observed);
			rmse[1] = computeObjective(runScriptForHEC_DSSVueJ55,flowDataJ55Observed);

		} catch (IOException e) {
			e.printStackTrace();
		}

		return rmse;

	}

	private synchronized double computeObjective(String runScriptForHEC_DSSVue, Vector<Double> flowDataObserved)
	{
		double rms = Double.MAX_VALUE;
		try {

			// execute HEC_DSS_VUE for simulated and observed data

			Vector<Double> flowData = null;
			Vector<String> outputStreamFromHEC_DSSVue;

			outputStreamFromHEC_DSSVue = execCommand(workingFolderPathForHEC_DSSVue, workingFolderPathForHEC_DSSVue + runScriptForHEC_DSSVue);
			flowData = parseDSSOutput(outputStreamFromHEC_DSSVue);


			// compute the error with RMSD
			rms = computeError(flowData, flowDataObserved);

			// show the plot of observed and simulated data 
			if(showPlot)
				new ChartCreater("RMSD  = " + rms , flowData, flowDataOutlet1Observed);

		} catch (ExecCommandExecption e) {

			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return rms;
	}


	// use the RMSD formula to compute the error
	private synchronized double computeError(Vector<Double> flowData, Vector<Double> flowDataObserved )
	{
		double rms = 0;
		for(int i= 0; i < flowData.size(); i++)
		{
			rms += Math.pow(flowData.get(i) - flowDataObserved.get(i), 2);
		}

		rms = rms/flowData.size();
		rms = Math.sqrt(rms);
		return rms;
	}



	// extract the data from the raw HEC-DSSVue output using data patterns
	private synchronized Vector<Double> parseDSSOutput(Vector<String> outputStream)
	{
		Vector<Double> data = new Vector<Double>();

		for(int i = 0; i < outputStream.size(); i++ )
		{

			if(outputStream.get(i).contains("31 May 2014") || outputStream.get(i).contains("1 June 2014") || outputStream.get(i).contains("2 June 2014") )
			{
				data.add(Double.parseDouble(outputStream.get(i).split(" ")[7]));
			}
		}

		return data;
	}


	// execute a command inside a working folder
	public synchronized Vector<String> execCommand(String workingFolderName, String executableName) throws IOException, ExecCommandExecption
	{
		if(DEBUG)
			System.out.println("Execute: " + workingFolderName + " " +  executableName );

		Vector<String> outputStream = new Vector<String>();

		ProcessBuilder pb = new ProcessBuilder(executableName);


		// set the working folder
		File workingFolder = new File(workingFolderName);
		pb.directory(workingFolder);

		Process proc = pb.start();


		BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

		BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

		// read the output from the command

		String s = null;


		if(DEBUG == true)
			System.out.println("Here is the standard output of the command:\n");

		while ((s = stdInput.readLine()) != null)
		{
			if( !s.equals(""))
			{
				if(DEBUG == true)
					System.out.println(s);
				outputStream.add(s);
			}
		}


		stdInput.close();

		// read data from standard error output from the attempted command
		if(DEBUG)
			System.out.println("Here is the standard error of the command (if any):\n");


		String errorMessage = "";
		while ((s = stdError.readLine()) != null)
		{
			System.out.println(s);
			errorMessage += s;
		}


		stdError.close();

		// wait for the process to close
		try {

			proc.waitFor();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		// if the error stream is not empty this might be caused by the fact that the met file and .subasin file are broken
		// throw exception so that we can fix it
		if(!errorMessage.equals(""))
			throw new ExecCommandExecption(errorMessage);
		return outputStream;

	}


	// update the configuration file by reading the data from a template 
	// and overwriting the configuration file with the new parameters
	// this method also fixes most of the cases when the .subasin configuaration file is broken

	private synchronized void updateConfigurationFile() throws IOException
	{

		if (configParametersValuesForDarang == null)
			return;


		// obtain path objects for the configuration file and the template
		Path sironConfigFilePathSource = Paths.get(workingFolderPAthForDarang, configFileSourceForSiron);
		Path sironConfigFilePathDestination = Paths.get(workingFolderPAthForDarang, configFileForDarang);

		// read all the data from the template
		List<String> fileContent = new ArrayList<>(Files.readAllLines(sironConfigFilePathSource, StandardCharsets.UTF_8));

		// change the values of the parameters for Darang

		for (int lineIndex = 0; lineIndex < fileContent.size(); lineIndex++) 
		{
			for(int subasinIndex = 0; subasinIndex < configSubasinsForDarang.length; subasinIndex++)
			{
				if (fileContent.get(lineIndex).contains(configSubasinsForDarang[subasinIndex]))
				{
					do
					{
						lineIndex++;
						for(int varNameIndex = 0; varNameIndex < configParametersNamesForDarang.length; varNameIndex++)
						{
							if (fileContent.get(lineIndex).contains(configParametersNamesForDarang[varNameIndex])) 
							{

								fileContent.set(lineIndex, configParametersNamesForDarang[varNameIndex] + 
										configParametersValuesForDarang[subasinIndex*configParametersNamesForDarang.length + varNameIndex]);
							}
						}

					}
					while(!fileContent.get(lineIndex).contains("End:"));


				}
			}
		}

		// write data to the .basin file
		Files.write(sironConfigFilePathDestination, fileContent, StandardCharsets.UTF_8);
	}

}
