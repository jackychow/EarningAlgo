package com.DailyAlgo;

import java.util.LinkedList;
import java.util.List;

public class DailyAlgoRunner {

	public static void main(String[] argc)
	{
		/*
		YahooQuoteRetriever retriever = new YahooQuoteRetriever();
		List<String> symbols = new LinkedList<String>();
		symbols.add("AMZN");
		symbols.add("AAPL");
		retriever.getQuoteForSymbols(new LinkedList<String>(symbols));
		*/
		FixedPeriodAheadDailyAlgo algo = new FixedPeriodAheadDailyAlgo();
		algo.runToday();
	}
	
	
}
