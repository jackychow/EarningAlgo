package com.QuoteRetriever;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class QuoteRetriever {

	private static final String TickDataRoot = "F:\\FinancialData\\HistoricalQuotes";
	private static final String EarningsRoot = "F:\\FinancialData\\Earnings";
	private static final String ListRoot = "F:\\FinancialData\\Lists";	

	private static final long PullIntervalMs = 4000;
	private static MyLogger logger = MyLogger.getInstance();
	
	/*
	public static void main(String[] argv)
	{
		MyLogger logger = MyLogger.getInstance();
				
		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		//DatabaseAccessor accessor = DatabaseAccessor.getInstance();
		//accessor.testget();
		//accessor.testput("GOOG");		
		
		//QuoteRetriever retriever = new QuoteRetriever();
		//retriever.insertSymbolList();
		//YahooDataPuller puller = new YahooDataPuller();
		//puller.importDataIntoDB("2010-01-01", "2014-03-04", "AMZN", TickDataRoot);
		//retriever.insertSymbolList();
		//retriever.pullAllHistoricalData("2010-01-01", "2014-03-04");
		//EarningsDataPuller earningPuller = new EarningsDataPuller();
		//earningPuller.pullEarningsForSymbol("DLTR", "F:\\Temp");
		//earningPuller.checkEmptyEarnings(QuoteRetriever.EarningsRoot);
		//earningPuller.importDataIntoDB("AMZN", "20140306", EarningsRoot);
		//retriever.insertEarningForTag("20140306");		
		//retriever.insertHistoricalQuotes("2010-01-01", "2014-03-04");
	}
		
		*/
	
	public void insertHistoricalQuotes(String fromDate, String toDate)
	{
		ArrayList<String> lists = new ArrayList<String>();
		HashSet<String> processed_symbols = new HashSet<String>();
		lists.add("SNP.csv");
		lists.add("DOW.csv");
		lists.add("NASDAQ_TOP200.csv");		
		File listRoot = new File(ListRoot);
		YahooDataPuller quotePuller = new YahooDataPuller();	
		
		for (String listFile : lists){
			File full = new File(listRoot, listFile);
			
			Path path = Paths.get(full.getPath());
			logger.info("------------ Pulling Data From List File: "+listFile+" -------------------");
			try {
				BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
				String line = null;
				
				while((line = reader.readLine())!= null)
				{									
					String symbol = line.trim();
					if(processed_symbols.contains(symbol))
						continue;
					logger.info("--------- Inserting Historical Quotes for ["+symbol+"]------------------");					
					quotePuller.importDataIntoDB(fromDate, toDate, symbol, TickDataRoot);
					processed_symbols.add(symbol);
				}
				
				reader.close();
				logger.info("Finished inserting all historical quotes");
			} catch (IOException e) {
				e.printStackTrace();
				logger.warning("Failed to read file: "+listFile);
			}			
		}				
	}

	public void insertEarningForTag(String tag)
	{
		ArrayList<String> lists = new ArrayList<String>();
		HashSet<String> processed_symbols = new HashSet<String>();
		lists.add("SNP.csv");
		lists.add("DOW.csv");
		lists.add("NASDAQ_TOP200.csv");		
		File listRoot = new File(ListRoot);
		EarningsDataPuller puller = new EarningsDataPuller();
		
		for (String listFile : lists){
			File full = new File(listRoot, listFile);
			
			Path path = Paths.get(full.getPath());
			logger.info("------------ Pulling Data From List File: "+listFile+" -------------------");
			try {
				BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
				String line = null;
				
				while((line = reader.readLine())!= null)
				{									
					String symbol = line.trim();
					if(processed_symbols.contains(symbol))
						continue;
					logger.info("--------- Inserting Earning for ["+symbol+"]------------------");					
					puller.importDataIntoDB(symbol, tag, EarningsRoot);
					processed_symbols.add(symbol);
				}
				
				reader.close();
				logger.info("Finished inserting all earning events");
			} catch (IOException e) {
				e.printStackTrace();
				logger.warning("Failed to read file: "+listFile);
			}			
		}				
	}		
	
	public void insertSymbolList()
	{
		ArrayList<String> lists = new ArrayList<String>();
		lists.add("SNP.csv");
		lists.add("DOW.csv");
		lists.add("NASDAQ_TOP200.csv");		
		File listRoot = new File(ListRoot);
		
		for (String listFile : lists){
			File full = new File(listRoot, listFile);
			
			Path path = Paths.get(full.getPath());
			logger.info("------------ Pulling Data From List File: "+listFile+" -------------------");
			try {
				BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
				String remark = listFile.split("\\.")[0];
				String line = null;
				ArrayList<String> symbols = new ArrayList<String>();
				while((line = reader.readLine())!= null)
				{				
					symbols.add(line.trim());
				}
				
				DatabaseAccessor.getInstance().importSymbolInfo(symbols, remark);
				
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
				logger.warning("Failed to read file: "+listFile);
			}										
		}
		
		System.out.println("Finished");
	}
	
	
	public void pullAllHistoricalData(String fromDate, String toDate)
	{
		ArrayList<String> lists = new ArrayList<String>();
		lists.add("SNP.csv");
		lists.add("DOW.csv");
		lists.add("NASDAQ_TOP200.csv");		
		File listRoot = new File(ListRoot);
		YahooDataPuller quotePuller = new YahooDataPuller();	
		EarningsDataPuller earningPuller = new EarningsDataPuller();
		
		for (String listFile : lists){
			File full = new File(listRoot, listFile);
			
			Path path = Paths.get(full.getPath());
			logger.info("------------ Pulling Data From List File: "+listFile+" -------------------");
			try {
				BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
				String line = null;
				
				while((line = reader.readLine())!= null)
				{				
					String symbol = line.trim();
					logger.info("--------- Pulling All Historical Data for ["+symbol+"]------------------");					
					quotePuller.pullData(fromDate, toDate, symbol, TickDataRoot);					
					earningPuller.pullEarningsForSymbol(symbol, EarningsRoot);
					
					try {
						Thread.sleep(PullIntervalMs);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}					
				}
				
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
				logger.warning("Failed to read file: "+listFile);
			}			
		}		
	}	 
}
