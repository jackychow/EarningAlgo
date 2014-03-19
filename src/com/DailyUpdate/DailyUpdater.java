package com.DailyUpdate;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

import com.AlgoSimulation.DoubleUtil;
import com.QuoteRetriever.DatabaseAccessor;
import com.QuoteRetriever.MyLogger;

public class DailyUpdater {

	private static final String ListRoot = "F:\\FinancialData\\Lists";	
	private static final String TaskRoot = "F:\\FinancialData\\DailyTasks";

	private static final String EarningUpdateFile = "EarningUpdate.csv";
	
	private static final long PullIntervalMs = 1000;
	private static MyLogger logger = MyLogger.getInstance();	
	
	private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//EarningData earning = null;
		//EarningData earning = puller.pullEarningsForSymbol("TSLA");
		DailyUpdater updater = new DailyUpdater();
		updater.runUpdate();
		
		/*
		EarningData earning = null;
		String symbol = "ARG";
		MktWatchDataPuller mktWatchPuller = new MktWatchDataPuller();		
		ZacksEarningDataPuller zacksPuller = new ZacksEarningDataPuller();		
		earning = mktWatchPuller.pullEarningsForSymbol(symbol, earning);
		earning = zacksPuller.pullEarningsForSymbol(symbol, earning);
		


		System.out.println(dateFormatter.format(earning.earningDate));
		System.out.println(earning.cons);
		System.out.println(earning.quarter);
		*/
	}

	public void runUpdate()
	{
		pullNewestEarnings();
		updateDB();
	}
	
	
	public void pullNewestEarnings()
	{
		ArrayList<String> lists = new ArrayList<String>();
		HashSet<String> processed_symbols = new HashSet<String>();
		lists.add("SNP.csv");
		lists.add("DOW.csv");
		lists.add("NASDAQ_TOP200.csv");		
		File listRoot = new File(ListRoot);	
		MktWatchDataPuller mktWatchPuller = new MktWatchDataPuller();
		ZacksEarningDataPuller zacksPuller = new ZacksEarningDataPuller();

		try {
			FileWriter fo = new FileWriter(getEarningPullOutputPath());
			PrintWriter pw = new PrintWriter(fo);
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
						
						logger.info("--------- Updating Earning Est. for ["+symbol+"]------------------");		
						EarningData earning = new EarningData();
						boolean success = true;
						success = success && mktWatchPuller.pullEarningsForSymbol(symbol, earning);
						if(success)
							success = success && zacksPuller.pullEarningsForSymbol(symbol, earning);
						
						if(success){
							pw.println(symbol+","+dateFormatter.format(earning.earningDate) + ","+earning.quarter+","+earning.cons);
							pw.flush();
						}else{
							logger.warning("Failed or no earning data exist for symbol "+symbol);
						}
						processed_symbols.add(symbol);
						
						try {
							Thread.sleep(PullIntervalMs);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}																	
					}
					
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}finally{
					
				}			
			}
			
			pw.close();
			fo.close();
			
		} catch (IOException e1) {
			logger.warning("ERROR: Failed to open file for writing!! "+e1.getMessage());
			return;
		}				
	}
	
	private void updateDB()
	{
		Path filePath = Paths.get(getEarningPullOutputPath());
		try{
			BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
			String line = null;
			while((line = reader.readLine()) != null)
			{
				StringTokenizer tokenizer = new StringTokenizer(line, ",");
				List<String> fields = new ArrayList<String>();
				while(tokenizer.hasMoreTokens())
				{
					fields.add(tokenizer.nextToken());
				}
				String symbol = fields.get(0);
				Date date = dateFormatter.parse(fields.get(1));
				String qtr = fields.get(2);
				double cons = Double.parseDouble(fields.get(3));
								
				syncDBEntryForSymbol(symbol, date, qtr, cons);				
			}						
			
		} catch (ParseException e) {
			logger.warning("ERROR Parsing date string "+e.getMessage());
		} catch (IOException e) {
			logger.warning("ERROR Cannot read file "+e.getMessage());
		}finally{
			
		}
	}
	
	private boolean syncDBEntryForSymbol(String symbol, Date date, String qtr, double cons)
	{
		//check if an entry exists
		try{
			int symbol_id = DatabaseAccessor.getInstance().getIdForSymbol(symbol);
			PreparedStatement st 
			= DatabaseAccessor.getInstance().prepareStatement(
					"SELECT cons, earning_date " +
					"FROM earning_events " +
					"WHERE symbol_id = ? " +
					//"AND earning_date = ? " +
					"AND quarter = ? ");		
			
			java.sql.Date dated = new java.sql.Date(date.getTime());
			
			st.setInt(1, symbol_id);
			//st.setDate(2, dated);
			st.setString(2, qtr);
			
			ResultSet rs = st.executeQuery();			
			String op = "insert"; 
			double dbcons = 99999; 
			java.sql.Date earning_date = null;
			while(rs.next())
			{
				dbcons = rs.getDouble(1);
				earning_date = rs.getDate(2);
				if(DoubleUtil.Equals(cons, dbcons) &&
						earning_date.equals(dated))
				{
					op = "nothing";
				} else {
					op = "update";
				}
			}
			rs.close();
			st.close();
			
			if(op.equals("insert"))
			{
				logger.info("Inserting new Earning Data for "+symbol);
				insertNewEarningRecord(symbol_id, dated, qtr, cons);
			} else if(op.equals("update"))
			{
				logger.info("Update Earning entry for "+symbol+" Cons: "+dbcons+" to "+cons+" Date: "+
						dateFormatter.format(earning_date) + " to " +dateFormatter.format(dated));
				updateConsForEarningRecord(symbol_id, dated, qtr, cons);
			} else {
				logger.info("No Operation Needed for "+symbol);
			}
			//else does nothing
			
		} catch (SQLException e) {
			logger.warning("ERROR failed to sync DB for symbol "+symbol+e.getMessage());
			return false;
		}		
		
		return true;
	}
	
	private void insertNewEarningRecord(int symbol_id, java.sql.Date date, String qtr, double cons)
	{
		try{
			//get the time
			PreparedStatement st 
			= DatabaseAccessor.getInstance().prepareStatement("SELECT DISTINCT time FROM earning_events WHERE symbol_id = ?");
			st.setInt(1, symbol_id);
			
			int time = 1;
			
			ResultSet rs = st.executeQuery();
			while(rs.next())
				time = rs.getInt(1);
			st.close();
			rs.close();
			
			//do the actual update
			st 
			= DatabaseAccessor.getInstance().prepareStatement(
					"INSERT INTO earning_events " +
					"(symbol_id, earning_date, time, quarter, cons) " +
					"VALUES (?, ?, ?, ?, ?)");
			
			st.setInt(1, symbol_id);
			st.setDate(2, date);
			st.setInt(3, time);
			st.setString(4, qtr);
			st.setDouble(5, cons);
			
			st.executeUpdate();
			
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}		
	}
	
	private void updateConsForEarningRecord(int symbol_id, java.sql.Date date, String qtr, double cons)
	{
		try{
			PreparedStatement st 
			= DatabaseAccessor.getInstance().prepareStatement(
					"UPDATE earning_events " +
					"SET cons = ? " +
					",earning_date = ? " +					
					"WHERE symbol_id = ? " +
					"AND quarter = ? ");
			
			st.setDouble(1, cons);	
			st.setDate(2, date);
			st.setInt(3, symbol_id);
			st.setString(4, qtr);
			
			st.executeUpdate();
			
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
			logger.warning("ERROR failed to update earning record "+e.getMessage());
		}							
	}
	
	private String getEarningPullOutputPath()
	{
		Date today = new Date();
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
		File base = new File(TaskRoot);
		File fullbase = new File(base, format.format(today));
		if(!fullbase.exists())
		{
			fullbase.mkdir();
		}
		File fullpath = new File(fullbase, EarningUpdateFile);
		return fullpath.getPath();
	}
	
}
