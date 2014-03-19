package com.DailyUpdate;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.QuoteRetriever.MyLogger;

public class MktWatchDataPuller {
	private static String BaseURL = "http://www.marketwatch.com/investing/stock/SYMBOL/analystestimates";
	private final MyLogger logger = MyLogger.getInstance();
	private static final int RETRY_ATTEMPTS = 2;
	private static final long RETRY_BACKOFF = 2000;	
	
	private static final SimpleDateFormat YahooDateFormatter = new SimpleDateFormat("yyyy-MM-dd");	
	
	public MktWatchDataPuller()
	{
		
	}
	
	private String getUrlForSymbol(String symbol)
	{
		return BaseURL.replace("SYMBOL", symbol);
	}

	public boolean pullEarningsForSymbol(String symbol, EarningData earning){
		
		String url = getUrlForSymbol(symbol);
		//File input = new File("F:\\Temp\\amzn.html");		
		boolean success = true;
		int attempt = 0;
		long backoff = 0;
		
		while(attempt < RETRY_ATTEMPTS)
		{
			try {
				//Document doc = Jsoup.parse(input, "UTF-8");
				Document doc = Jsoup.connect(url).timeout(10*1000).get();
				//System.out.println(doc);				
								
				Element detail = doc.select(".snapshot").get(0);
				Elements ths = detail.getElementsByTag("td");				
								
				
				String cons_str = ths.get(7).text();
				
				if(earning == null)
					earning = new EarningData();
				earning.cons = Double.parseDouble(cons_str);
				
				//System.out.println(earning.cons);
				
			}catch (Exception e) {
				logger.warning(e.toString());
				logger.warning("ERROR: Failed pulling earning for symbol: "+symbol);
				success = false;
			}
			
			if(!success)
			{
				attempt++;
				logger.warning("[MktWatchPuller] Failed to pull earnings data for symbol "+symbol+", retry and backoff. Attempt: "+attempt);				
				backoff += RETRY_BACKOFF;
				try {
					Thread.sleep(backoff);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}else{
				logger.info("[MktWatchPuller]Done pulling data for Symbol: "+symbol);
				break;
			}
		}				
		
		if(attempt == RETRY_ATTEMPTS)
			return false;
		
		return true;
	}

}
