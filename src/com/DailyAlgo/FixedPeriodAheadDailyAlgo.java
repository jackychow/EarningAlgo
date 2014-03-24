package com.DailyAlgo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.joda.time.LocalDate;

import com.AlgoSimulation.IBuyDecision;
import com.AlgoSimulation.ISellDecision;
import com.AlgoSimulation.Position;
import com.AlgoSimulation.decisions.buy.BlackListBuyDecision;
import com.AlgoSimulation.decisions.buy.PrevQtrUpsideBuyDecision;
import com.AlgoSimulation.decisions.buy.TrailingEarningDaysBuyDecision;
import com.AlgoSimulation.decisions.sell.IndepTrailingEarningDaysSellDecision;

public class FixedPeriodAheadDailyAlgo {
	
	private int[] trailingRangeForBuy;	
	private int trailingDayForSell;
		
	private double minAllocation;
	private double maxAllocation;

	private double brokageFee;
	private double cash;
	
	private Map<String, Position> positions = new HashMap<String, Position>();
	private List<IBuyDecision> buyDecisions = new ArrayList<IBuyDecision>();
	private List<ISellDecision> sellDecisions = new ArrayList<ISellDecision>();

	private DailyStateLoader loader;
	
	private final static DailyTaskLogger logger = DailyTaskLogger.getInstance();
	private final static DailyReportWriter writer = DailyReportWriter.GetInstance();

	public FixedPeriodAheadDailyAlgo()
	{
		loader = new DailyStateLoader();
		trailingRangeForBuy = loader.getTrailingRangeForBuy();
		trailingDayForSell = loader.getTrailingDayForSell();
		
		minAllocation = loader.getMinAllocation();
		maxAllocation = loader.getMaxAllocation();
		
		cash = loader.getCash();
		brokageFee = loader.getBrokageFee();
		
		for(Position pos : loader.getPositions())
		{
			positions.put(pos.getSymbol(), pos);
		}
				
		initBuyDecisions();
		initSellDecisions();
	}
	
	public void runToday()
	{
		LocalDate today = LocalDate.now();
		try {
			writer.openFile("F:\\FinancialData\\DailyTasks");
		} catch (IOException e) {
			logger.warning("Failed to open report file");
		}		
		
		writer.printTodaySummary(today, loader);
		List<Position> candidates = new LinkedList<Position>(positions.values());
		List<Position> toSell = new LinkedList<Position>();
		for(ISellDecision sellDecision : sellDecisions)
		{			
			toSell.addAll(sellDecision.getSellCandidates(candidates, today));
		}		
		
		if(!toSell.isEmpty())
			closePositions(toSell);
		
		List<Position> toBuy = null;
		for(IBuyDecision buyDecision : buyDecisions)
		{
			toBuy = buyDecision.getBuyCandidates(today, toBuy);
		}
		
		//clear out those we already bought
		for(Iterator<Position> iter = toBuy.iterator(); iter.hasNext();)
		{
			String symbol = iter.next().getSymbol();
			if(positions.containsKey(symbol))
			{
				iter.remove();
			} 
		}		
		
		//limit by min allocation 
		toBuy = getTopBuyCandidatesBasedOnCash(toBuy);
		
		List<String> symbols = new LinkedList<String>();
		for(Position pos : toBuy)
		{
			symbols.add(pos.getSymbol());
		}
		Map<String, Double> prices = getPricesForSymbols(symbols);	
		
		if(!toBuy.isEmpty()){
			
			distributeCashToPositions(toBuy, prices);
		
			openPositions(toBuy);
		}
		
		writer.closeAll();
	}
	
	private void closePositions(List<Position> toSell)
	{
		List<String> symbols = new LinkedList<String>();
		for(Position pos : toSell){			
			symbols.add(pos.getSymbol());
		}
		
		Map<String, Double> prices = getPricesForSymbols(symbols);
		
		for(Position pos : toSell)
		{
			double price = pos.getBoughtPrice();
			
			if(prices.containsKey(pos.getSymbol()))
			{
				price = prices.get(pos.getSymbol());
			}else{
				logger.warning("ERROR Failed to get current price for symbol (using bought price to sell instead) "+pos.getSymbol());
			}
			
			cash += (double)pos.getNumShares()*price;
			positions.remove(pos.getSymbol());
			
			//report
			writer.printPositionSold(pos, price);
			
		}
	}
	
	private void openPositions(List<Position> toBuy)
	{
		for(Position pos : toBuy)
		{
			writer.printPositionBought(pos);
		}		
	}
	
	private Map<String, Double> getPricesForSymbols(List<String> symbols)
	{		
		return YahooQuoteRetriever.getQuoteForSymbols(symbols);
	}
	
	private void initBuyDecisions()
	{
		TrailingEarningDaysBuyDecision earningDecision = new TrailingEarningDaysBuyDecision(trailingRangeForBuy);
		PrevQtrUpsideBuyDecision qtrDecision = new PrevQtrUpsideBuyDecision();
		String settingStr = "_" + trailingRangeForBuy[0] + "_" + trailingRangeForBuy[1] + "_" +trailingDayForSell;
		BlackListBuyDecision blacklistDecision = new BlackListBuyDecision("F:\\FinancialData\\Lists\\AlgoBlackList"+settingStr+".txt");
		
		buyDecisions.add(earningDecision);
		buyDecisions.add(qtrDecision);
		buyDecisions.add(blacklistDecision);		
	}
	
	private void initSellDecisions()
	{
		IndepTrailingEarningDaysSellDecision earningDaysDecision = new IndepTrailingEarningDaysSellDecision(trailingDayForSell);		
		sellDecisions.add(earningDaysDecision);
	}

	private List<Position> getTopBuyCandidatesBasedOnCash(List<Position> candidates)
	{
		int maxBuy = (int)(cash/minAllocation);
		int total = candidates.size();
		
		if(candidates.size() > maxBuy){
			int toRemove = candidates.size() - maxBuy;
			while(toRemove > 0)
			{
				candidates.remove(candidates.size()-1);
				toRemove--;
			}
		}
		
		String info = "[ Selected Top Candidates Dropped: " + Math.max((total - maxBuy), 0) + "]";
		logger.info(info);
		
		return candidates;
	}
	
	private void distributeCashToPositions(List<Position> candidates, Map<String, Double> prices)
	{
		LocalDate today = LocalDate.now();
		evenCashDistribution(cash, candidates, today, prices);		
	}
	
	private void evenCashDistribution(double cash, List<Position> candidates, LocalDate today, Map<String, Double> ticks)
	{
		cash -= brokageFee*(double)candidates.size();
		double cashPerCandidate = Math.min(cash/((double)candidates.size()), maxAllocation);				
				
		for(Iterator<Position> iter = candidates.iterator(); iter.hasNext();)
		{
			Position pos = iter.next();
			if(!ticks.containsKey(pos.getSymbol()))
				iter.remove();
			else{
				double price = ticks.get(pos.getSymbol());
				int numShares = (int)(cashPerCandidate/price);
				pos.setBuyPosition(price, numShares, today);
			}
		}		
	}	
}
