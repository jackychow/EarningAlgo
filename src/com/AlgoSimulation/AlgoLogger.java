package com.AlgoSimulation;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class AlgoLogger {
	private static final String logLocation = "F:\\Development\\logs\\AlgoSimulation";
	private static FileHandler fileTxt;
	private static SimpleFormatter formatterTxt;
	private static final AlgoLogger instance = new AlgoLogger();
	private Logger logger; 
	
	private AlgoLogger()
	{
		//Logger logger 
		logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
		logger.setLevel(Level.ALL);
		
		try {
			fileTxt = new FileHandler(getLogFile());
			formatterTxt = new SimpleFormatter();
			fileTxt.setFormatter(formatterTxt);
			logger.addHandler(fileTxt);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//logger.info("Log Something");
	}
	
	private static String getLogFile()
	{
		Date now = new Date();
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmm");
		File loc = new File(logLocation);
		File log = new File(loc, (format.format(now)+".txt"));
		
		return log.getPath();
	}		
	
	public static AlgoLogger getInstance()
	{
		return instance;
	}
	
	public void info(String msg)
	{
		logger.info(msg);
	}
	
	public void fine(String msg)
	{
		logger.fine(msg);
	}
	
	public void warning(String msg)
	{
		logger.warning(msg);
	}
}
