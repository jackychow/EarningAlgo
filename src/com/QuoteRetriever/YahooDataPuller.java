package com.QuoteRetriever;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.StringTokenizer;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.*;


public class YahooDataPuller {

	private static String BaseQuery = "http://ichart.finance.yahoo.com/table.csv?";
	private static String TempFileLocation = "F:\\Temp\\table.csv";
	private final MyLogger logger = MyLogger.getInstance();
	private static final int RETRY_ATTEMPTS = 3;
	private static final long RETRY_BACKOFF = 5000;
	
	private static final SimpleDateFormat YahooDateFormatter = new SimpleDateFormat("yyyy-MM-dd");	
	
	public YahooDataPuller()
	{
		
	}
	
	/**
	 * fileLocation - file folder and file is named <symbol>_<fromDate>_<toDate>.csv
	 * if NULL, then it would read from link and import into DB
	 * @param fromDate format in yyyy-MM-dd
	 * @param toDate format in yyyy-MM-dd
	 * @param symbol
	 * @param fileLocation
	 */
	public void pullData(String fromDate, String toDate, String symbol, String fileLocation)
	{			
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");	
		
		try {
			Date fromDD = dateFormat.parse(fromDate);
			Date toDD = dateFormat.parse(toDate);
			String query = constructQueryURL(fromDD, toDD, symbol);		
			
			String outputPath = TempFileLocation;
			if(fileLocation != null)
			{
				outputPath = getOutputFileLocation(symbol, fromDD, toDD, fileLocation);
			}
			
			if(executeWSRequest(query, outputPath)){
				logger.info("Successfully Pulled Data for Symbol: "+symbol);
			} else {
				logger.warning("Failed pulling quote Data for Symbol: "+symbol);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	public void importDataIntoDB(String fromDate, String toDate, String symbol, String fileLocation)
	{
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd"); 
		try {
			Date fromDD = dateFormat.parse(fromDate);
			Date toDD = dateFormat.parse(toDate);
			
			String filePath = TempFileLocation;
			if(fileLocation != null)
			{
				filePath = getOutputFileLocation(symbol, fromDD, toDD, fileLocation);
			}
			
			importCSVFileIntoDB(filePath, symbol);
			
		} catch (ParseException e) {
			e.printStackTrace();
		}		
	}
	
	private String getOutputFileLocation(String symbol, Date fromDate, Date toDate, String root)
	{
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
		StringBuilder sb = new StringBuilder();
		sb.append(symbol);
		sb.append("_");
		sb.append(format.format(fromDate));
		sb.append("_");
		sb.append(format.format(toDate));
		sb.append(".csv");
		File base = new File(root);
		File file = new File(base, sb.toString());
		
		return file.getPath();
	}
	
	private String constructQueryURL(Date fromDate, Date toDate, String symbol)
	{
		StringBuilder sb = new StringBuilder();
		Calendar cal = Calendar.getInstance();
		
		sb.append(BaseQuery);
		sb.append("s="+symbol);
		sb.append("&");
		cal.setTime(fromDate);
		sb.append("b="+cal.get(Calendar.DAY_OF_MONTH));
		sb.append("&");
		sb.append("a="+cal.get(Calendar.MONTH)); //YAHOO api takes month minus 1 .... so we don't add 1 to Calendar month
		sb.append("&");
		sb.append("c="+cal.get(Calendar.YEAR));
		sb.append("&");
		cal.setTime(toDate);
		sb.append("e="+cal.get(Calendar.DAY_OF_MONTH));
		sb.append("&");
		sb.append("d="+cal.get(Calendar.MONTH));
		sb.append("&");
		sb.append("f="+cal.get(Calendar.YEAR));
		sb.append("&g=d&ignore=.csv");
		
		return sb.toString();
	}
	
	private boolean executeWSRequest(String url, String outputFilePath)
	{
		int attempt = 0;
		long backoff = 0;
		
		while(attempt < RETRY_ATTEMPTS)
		{
			CloseableHttpClient httpclient = HttpClients.createDefault();		
			boolean success = true;
			try{
				HttpGet httpget = new HttpGet(url);
				CloseableHttpResponse response = httpclient.execute(httpget);
				FileOutputStream fo = new FileOutputStream(outputFilePath);
				
				try{
					//System.out.println(response.getStatusLine());
					HttpEntity entity = response.getEntity();
					//EntityUtils.consume(entity);
					entity.writeTo(fo);
					
				}finally{
					fo.close();
					response.close();
				}			
				
				success = true;
			} catch (ClientProtocolException e) {
				logger.warning(e.toString());
				logger.warning("Failed to execute request: "+url);
				success = false;
			} catch (Exception e) {
				logger.warning(e.toString());
				System.out.println("Failed to execute request: "+url);
				success = false;
			}finally{
				try {
					httpclient.close();
				} catch (IOException e) {
					e.printStackTrace();
				}									
			}

			if(!success)
			{
				attempt++;
				logger.warning("Failed to execute query, retry and backoff. Attempt: "+attempt);				
				backoff += RETRY_BACKOFF;
				try {
					Thread.sleep(backoff);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}else{
				return true;
			}
		}
		
		return false;
	}
	
	private void importCSVFileIntoDB(String fileLocation, String symbol)
	{
		Path path = Paths.get(fileLocation);
		try{
			int symbol_id = DatabaseAccessor.getInstance().getIdForSymbol(symbol);
			BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
			PreparedStatement st 
			= DatabaseAccessor.getInstance().prepareStatement(
					"INSERT INTO ticks " +
					"(symbol_id, date, open, high, low, close, volumn, adj) " +
					" values (?, ?, ?, ?, ?, ?, ?, ?)");
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
				
				java.sql.Date dated = new java.sql.Date(YahooDateFormatter.parse(fields.get(0)).getTime());
				float openf = Float.parseFloat(fields.get(1));
				float highf = Float.parseFloat(fields.get(2));
				float lowf = Float.parseFloat(fields.get(3));
				float closef = Float.parseFloat(fields.get(4));
				int volumni = Integer.parseInt(fields.get(5));
				float adjf = Float.parseFloat(fields.get(6));

				st.setInt(1, symbol_id);
				st.setDate(2, dated);
				st.setFloat(3, openf);
				st.setFloat(4, highf);
				st.setFloat(5, lowf);
				st.setFloat(6, closef);
				st.setInt(7, volumni);				
				st.setFloat(8, adjf);
				
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
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}finally{
			
		}
		
	}
	
}
