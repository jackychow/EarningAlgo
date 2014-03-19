package com.QuoteRetriever;

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
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class EarningsDataPuller {

	private static String BaseURL = "http://www.streetinsider.com/ec_earnings.php?q=";
	private final MyLogger logger = MyLogger.getInstance();
	private static final int RETRY_ATTEMPTS = 2;
	private static final long RETRY_BACKOFF = 2000;	
	
	private static final SimpleDateFormat YahooDateFormatter = new SimpleDateFormat("yyyy-MM-dd");	
	
	public EarningsDataPuller(){
		
	}
	
	private String getUrlForSymbol(String symbol)
	{
		return BaseURL+symbol;
	}
	
	public void pullEarningsForSymbol(String symbol, String outputLocation){
		//File input = new File("F:\\Temp\\earnings.html");
		String url = getUrlForSymbol(symbol);
		boolean success = true;
		int attempt = 0;
		long backoff = 0;
		
		while(attempt < RETRY_ATTEMPTS)
		{
			try {
				FileWriter fo = new FileWriter(constructOutputPath(symbol, outputLocation));
				PrintWriter pw = new PrintWriter(fo);
				
				pw.println("Date,Time,Qtr,EPS,Cons");
				//Document doc = Jsoup.parse(input, "UTF-8");
				
				Document doc = Jsoup.connect(url).timeout(10*1000).get();				
								
				//System.out.println(doc);
				
				Element info = doc.select(".info-table").get(0);
				Elements tds = info.getElementsByTag("td");
				String earningTime = tds.last().text();
				
				Elements cells = doc.select(".is_hilite");
										
				if(cells.size() == 0){
					cells = doc.select(".LiteHover");
					if(cells.size() == 0){
						success = false;
					}
				}
				
				for(int i=0; i<cells.size(); i++)
				{
					String text = cells.get(i).text();				
					StringTokenizer tokenizer = new StringTokenizer(text, " ");
					ArrayList<String> list = new ArrayList<String>(tokenizer.countTokens());
					while(tokenizer.hasMoreTokens())
					{
						list.add(tokenizer.nextToken().trim());
					}
					
					//System.out.println("Date: "+list.get(0)+" Qtr: "+list.get(1)+" EPS: "+list.get(2)+" Cons: "+list.get(3));
					
					SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy");
					SimpleDateFormat outFormat = new SimpleDateFormat("yyyy-MM-dd");
					Date date = dateFormat.parse(list.get(0));
					
					pw.println(outFormat.format(date)+","+earningTime+","+list.get(1)+","+list.get(2).replace("$", "")+","+list.get(3).replace("$", ""));															
				}
				
				try {
					fo.close();
				} catch (IOException e) {
					logger.warning(e.toString());
				}				
			} catch (IOException e) {
				logger.warning(e.toString());
				logger.warning("ERROR: Failed pulling earning for symbol: "+symbol);
				success = false;
			} catch (Exception e) {
				logger.warning(e.toString());
				logger.warning("ERROR: Failed pulling earning for symbol: "+symbol);
				success = false;
			}
			
			if(!success)
			{
				attempt++;
				logger.warning("Failed to pull earnings data, retry and backoff. Attempt: "+attempt);				
				backoff += RETRY_BACKOFF;
				try {
					Thread.sleep(backoff);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}else{
				logger.info("Done Exporting Earning for Symbol: "+symbol);
				break;
			}
		}		
	}
	
	public void checkEmptyEarnings(String location)
	{
		File folder = new File(location);
		int count = 0;
		ArrayList<String> badSymbols = new ArrayList<String>();
		for(File file : folder.listFiles())
		{
			Path path = Paths.get(file.getAbsolutePath()); 
			BufferedReader reader = null;
			try {
				reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
				String line = null;
				int lines = 0;
				while((line = reader.readLine())!= null)
				{				
					line = reader.readLine();
					lines++;
				}
				if(lines < 3){
					String[] name = file.getName().split("_");
					String symbol = name[0];
					logger.warning("++++++ Found Broken Earning File Readloading for symbol: "+symbol+" ++++++++ ");
					badSymbols.add(symbol);
					//pullEarningsForSymbol(symbol, location);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			count++;
		}
		logger.info("Checked "+count+" earning files");
		logger.info("contains "+badSymbols.size()+" bad symbols");
		for(String s : badSymbols)
		{
			System.out.println(s);
		}
	}
	
	public void importDataIntoDB(String symbol, String tag, String root)
	{
		String filename = symbol+"_"+tag+".csv";		
		File base = new File(root);
		File full = new File(base, filename);
		
		importCSVFileIntoDB(full.getPath(), symbol);
	}
	
	private void importCSVFileIntoDB(String fileLocation, String symbol)
	{
		Path path = Paths.get(fileLocation);
		try{
			int symbol_id = DatabaseAccessor.getInstance().getIdForSymbol(symbol);
			BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
			PreparedStatement st 
			= DatabaseAccessor.getInstance().prepareStatement(
					"INSERT INTO earning_events " +
					"(symbol_id, earning_date, time, quarter, eps, cons) " +
					" values (?, ?, ?, ?, ?, ?)");
			
			
			String line = null;
			int batchSize = 0;
			//read first line of titles
			line = reader.readLine();
			while((line = reader.readLine())!= null)
			{				
				StringTokenizer tokenizer = new StringTokenizer(line, ",");
				ArrayList<String> fields = new ArrayList<String>(tokenizer.countTokens());
				while(tokenizer.hasMoreTokens())
				{
					fields.add(tokenizer.nextToken());
				}
				//System.out.println(fields);
				
				java.sql.Date dated = new java.sql.Date(YahooDateFormatter.parse(fields.get(0)).getTime());
				int time = fields.get(1).equals("After Close") ? -1 : 1;
				String qtr = fields.get(2);
				float eps = Float.parseFloat((fields.get(3).equals("N/A") ? "0.0" : fields.get(3)));
				float cons = Float.parseFloat((fields.get(4).equals("N/A") ? "0.0" : fields.get(4)));
				
				st.setInt(1, symbol_id);
				st.setDate(2, dated);
				st.setInt(3, time);
				st.setString(4, qtr);
				st.setFloat(5, eps);
				st.setFloat(6, cons);
				
				st.addBatch();
				batchSize++;
				
				if(batchSize >= 100){
					st.executeBatch();
					batchSize = 0;					
				}				
			}			
			
			if(batchSize > 0)
				st.executeBatch();

			st.close();
			reader.close();
		}catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}finally{
			
		}
		
	}
	
	private String constructOutputPath(String symbol, String root)
	{
		Date today = new Date();
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
		StringBuilder sb = new StringBuilder();
		sb.append(symbol);
		sb.append("_");
		sb.append(format.format(today));
		sb.append(".csv");
		
		File rootDir = new File(root);
		File fullpath = new File(rootDir, sb.toString());
		
		return fullpath.getPath();
	}	
}
