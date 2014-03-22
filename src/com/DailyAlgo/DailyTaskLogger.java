package com.DailyAlgo;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class DailyTaskLogger {
	private static final String logLocation = "F:\\Development\\logs\\DailyTasks";
	private static FileHandler fileTxt;
	private static SimpleFormatter formatterTxt;
	private static final DailyTaskLogger instance = new DailyTaskLogger();
	private Logger logger; 
	
	private DailyTaskLogger()
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
		SimpleDateFormat folderFormat = new SimpleDateFormat("yyyyMMdd");
		File loc = new File(logLocation);
		File fullBase = new File(loc, folderFormat.format(now));		
		if(!fullBase.exists()){
			fullBase.mkdir();
		}
		File log = new File(fullBase, (format.format(now)+".txt"));
		
		return log.getPath();
	}		
	
	public static DailyTaskLogger getInstance()
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
