package com.QuoteRetriever;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class MyLogger {	
	private static final String logLocation = "F:\\Development\\logs\\QuoteRetriever";
	private static FileHandler fileTxt;
	private static SimpleFormatter formatterTxt;
	private static final MyLogger instance = new MyLogger();
	private Logger logger; 
	
	private MyLogger()
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
	
	public static MyLogger getInstance()
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
