package com.DailyUpdate;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.QuoteRetriever.MyLogger;

public class ZacksEarningDataPuller {

	private static String BaseURL = "http://www.zacks.com/stock/quote/SYMBOL/detailed-estimates";
	private final MyLogger logger = MyLogger.getInstance();
	private static final int RETRY_ATTEMPTS = 2;
	private static final long RETRY_BACKOFF = 2000;	
	
	private static final SimpleDateFormat YahooDateFormatter = new SimpleDateFormat("yyyy-MM-dd");	
	
	public ZacksEarningDataPuller()
	{
		
	}
	
	private String getUrlForSymbol(String symbol)
	{
		return BaseURL.replace("SYMBOL", symbol);
	}

	public EarningData pullEarningsForSymbol(String symbol){
		
		String url = getUrlForSymbol(symbol);
		//File input = new File("F:\\Temp\\amzn.html");		
		boolean success = true;
		int attempt = 0;
		long backoff = 0;
		EarningData earning = null;
		
		while(attempt < RETRY_ATTEMPTS)
		{
			try {
				//Document doc = Jsoup.parse(input, "UTF-8");
				Document doc = Jsoup.connect(url).timeout(10*1000).get();
				Element detail = doc.select("#detail_estimate").get(0);
				Elements ths = detail.getElementsByTag("th");				
								
				//System.out.println(doc);
				
				String date_str = ths.get(0).text();
				String cons_str = ths.get(1).text();
				
				Element growth = doc.select("#earnings_growth_estimates").get(0);
				Elements tds = growth.getElementsByTag("td");
				String qtr_str = tds.get(0).text();
				
				if(qtr_str.equals("NA"))
					return null;
				
				Pattern p = Pattern.compile("\\d{2}/\\d{4}");
				Matcher m = p.matcher(qtr_str);
				 
				while(m.find())
					qtr_str = m.group(0);
				String[] splitted = qtr_str.split("/");
				int quarter_num = Integer.parseInt(splitted[0])/3;
				qtr_str = "Q"+quarter_num+splitted[1].substring(2);					
				
				SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy");
				Date date = dateFormat.parse(date_str);
				
				earning = new EarningData(qtr_str, date, Double.parseDouble(cons_str));
				
			}catch (Exception e) {
				logger.warning(e.toString());
				logger.warning("ERROR: Failed pulling earning for symbol: "+symbol);
				success = false;
			}
			
			if(!success)
			{
				attempt++;
				logger.warning("Failed to pull earnings data for symbol "+symbol+", retry and backoff. Attempt: "+attempt);				
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
		
		return earning;
	}
	
}
