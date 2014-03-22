package com.DailyAlgo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.LocalDate;

import com.AlgoSimulation.Position;

public class DailyReportWriter {

	private static final DailyReportWriter instance = new DailyReportWriter();
	private PrintWriter writer = null;	
	private DecimalFormat decimalFormatter = new DecimalFormat("#.##");	
	
	private DailyReportWriter()
	{
		
	}
	

	public static DailyReportWriter GetInstance()
	{
		return instance;
	}
	
	public void openFile(String path) throws IOException
	{		
		if(writer != null)
			writer.close();
			
		FileWriter fo = new FileWriter(getFile(path));
		writer = new PrintWriter(fo);
	}	
	
	public void printTodaySummary(LocalDate today)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("========= Summary For ");
		sb.append(today.toString("yyyy-MM-dd"));
		sb.append(" =========\n");
		sb.append("========= End Of Summary =========");
		writer.println(sb.toString());
	}
	
	public void printPositionBought(Position pos)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("[OPEN NEW POSITION] ");
		sb.append(pos.toString());
		sb.append(pos.printBuyDecisions());
		writer.println(sb.toString());
	}
	
	public void printPositionSold(Position pos, double price)
	{
		StringBuilder sb = new StringBuilder();		
		sb.append("[CLOSE POSITION] [Sell Price: ");
		sb.append(price);
		sb.append(" (Yield ");
		sb.append(decimalFormatter.format(pos.getYieldPctForPrice(price)));
		sb.append("%)");
		sb.append(" ] ");
		sb.append(pos.toString());
		sb.append(pos.printSellDecisions());
		writer.println(sb.toString());
	}
	
	public void println(String msg)
	{
		writer.println(msg);
		writer.flush();
	}

	public void printlnImportant(String msg)
	{
		writer.println(msg);
		writer.flush();
	}

	
	public void closeAll()
	{
		if(writer != null)
		{
			writer.flush();
			writer.close();
		}			
	}
	
	private String getFile(String location)
	{
		Date now = new Date();
		SimpleDateFormat folder = new SimpleDateFormat("yyyyMMdd");
		File loc = new File(location);
		File fullbase = new File(loc, folder.format(now));
		if(!fullbase.exists())
		{
			fullbase.mkdir();
		}

		File log = new File(fullbase, "DailyReport_"+(folder.format(now)+".txt"));
		
		return log.getPath();
	}		
	

	
}
