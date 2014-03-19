package com.AlgoSimulation;

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

public class AlgoReportWriter {

	private static final AlgoReportWriter instance = new AlgoReportWriter();
	private PrintWriter writer = null;	
	private DecimalFormat decimalFormatter = new DecimalFormat("#.##");
	private boolean enabled = true;	
	
	private AlgoReportWriter()
	{
		
	}
	
	public void setEnable(boolean enable)
	{
		enabled = enable;
	}
	
	public static AlgoReportWriter GetInstance()
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
		if(!enabled)
			return;

		StringBuilder sb = new StringBuilder();
		sb.append("========= Summary For ");
		sb.append(today.toString("yyyy-MM-dd"));
		sb.append(" =========\n");
		sb.append(BrokerManager.GetInstance().getSummary());
		sb.append("========= End Of Summary =========");
		writer.println(sb.toString());
	}
	
	public void printPositionBought(Position pos)
	{
		if(!enabled)
			return;

		StringBuilder sb = new StringBuilder();
		sb.append("[OPEN NEW POSITION] ");
		sb.append(pos.toString());
		sb.append(pos.printBuyDecisions());
		writer.println(sb.toString());
	}
	
	public void printPositionSold(Position pos, double price)
	{
		if(!enabled)
			return;

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
		if(!enabled)
			return;

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
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss");
		File loc = new File(location);
		File log = new File(loc, (format.format(now)+".txt"));
		
		return log.getPath();
	}		

	
}
