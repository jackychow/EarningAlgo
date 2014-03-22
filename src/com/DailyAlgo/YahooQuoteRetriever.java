package com.DailyAlgo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class YahooQuoteRetriever {

	private static String BaseQuery = "http://finance.yahoo.com/d/quotes.csv?s=SYMBOLS&f=sl1";
	private static final DailyTaskLogger logger = DailyTaskLogger.getInstance();
	private static final int RETRY_ATTEMPTS = 3;
	private static final long RETRY_BACKOFF = 5000;
	
	
	public YahooQuoteRetriever()
	{
		
	}
	
	public static Map<String, Double> getQuoteForSymbols(List<String> symbols)
	{
		Map<String, Double> quotes = new HashMap<String, Double>();
		String url = getUrlForSymbols(symbols);
		
		int attempt = 0;
		long backoff = 0;
		
		while(attempt < RETRY_ATTEMPTS)
		{
			CloseableHttpClient httpclient = HttpClients.createDefault();		
			boolean success = true;
			try{
				HttpGet httpget = new HttpGet(url);
				CloseableHttpResponse response = httpclient.execute(httpget);
				
				try{
					//System.out.println(response.getStatusLine());
					HttpEntity entity = response.getEntity();
					//EntityUtils.consume(entity);
					BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
					String line;
					while((line = reader.readLine()) != null)
					{
						String[] splitted = line.split(",");						
						quotes.put(splitted[0].replace("\"", ""), Double.parseDouble(splitted[1]));
					}
					
				}finally{
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
				return quotes;
			}
		}	
		
		return quotes;
	}
	
	private static String getUrlForSymbols(List<String> symbols)
	{
		String str = "";
		for(int i = 0; i<symbols.size(); i++)
		{
			str += symbols.get(i);
			if(i < symbols.size()-1)
				str+="+";
		}
		
		return BaseQuery.replace("SYMBOLS", str);
	}
			
}
