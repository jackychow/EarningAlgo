package com.AlgoSimulation;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.joda.time.LocalDate;

import com.AlgoSimulation.decisions.buy.PrevQtrUpsideBuyDecision;
import com.AlgoSimulation.decisions.buy.TrailingEarningDaysBuyDecision;
import com.AlgoSimulation.decisions.sell.MaxLossSellDecision;
import com.AlgoSimulation.decisions.sell.MinYieldSellDecision;
import com.AlgoSimulation.decisions.sell.TrailingEarningDaysSellDecision;

public class UnConstraintedAlgo {

	//days are inclusive
	private int[] trailingRangeForBuy;	
	private int trailingDayForSell;
		
	private double yieldPctToSell = 15.0;
	private double maxLossPctToSell = -25.0;
	private int[] trailingRangeForAnalysis = {5, 1};

	private Map<String, Position> positions = new HashMap<String, Position>();
	private List<IBuyDecision> buyDecisions = new ArrayList<IBuyDecision>();
	private List<ISellDecision> sellDecisions = new ArrayList<ISellDecision>();
	
	private int currentDayCnt = 0;
	 
	private DatabaseAccessor db = DatabaseAccessor.getInstance();
	private AlgoReportWriter writer = AlgoReportWriter.GetInstance();
	
	private Map<String, List<Double>> symbolYields = new HashMap<String, List<Double>>();
	private DecimalFormat decimalFormatter = new DecimalFormat("#.##");
	
	public UnConstraintedAlgo(double cashToStart, FixPeriodSettings settings)
	{
		trailingRangeForBuy = settings.getTrailingRangeForBuy();
		trailingDayForSell = settings.getTrailingDayForSell();				
		
		initBuyDecisions();
		initSellDecisions();
	}	
		
	
	public String getSettingsString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append("Range For Buy: ");
		sb.append("{"+trailingRangeForBuy[0]+", "+trailingRangeForBuy[1]+"}");
		sb.append(" Trailing Days to Sell: ");
		sb.append(trailingDayForSell);
		sb.append(" Yield % to sell: ");
		sb.append(yieldPctToSell);
		sb.append(" Max Loss % to sell: ");
		sb.append(maxLossPctToSell);
		sb.append(" Range For Analysis: ");
		sb.append("{"+trailingRangeForAnalysis[0]+", "+trailingRangeForAnalysis[1]+"}");
		
		return sb.toString();
	}
	
	/**
	 * Run for the given day
	 */
	public void run(LocalDate today)
	{		
		AlgoReportWriter.GetInstance().printTodaySummary(today);		
		currentDayCnt++;
		
		//First Decide if we are selling anything
		List<Position> candidates = new LinkedList<Position>(positions.values());
		List<Position> toSell = new LinkedList<Position>();
		for(ISellDecision sellDecision : sellDecisions)
		{			
			toSell.addAll(sellDecision.getSellCandidates(candidates, today));
		}
		closePositions(toSell, today);
		
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
		
		openPositions(toBuy, today);
	}
	
	public void reportSummary()
	{
		writer.println("================ Symbol Yield Summary =======================");
		writer.println("Symbol#Yields#TotalPositive#TotalNegative#PctPositive#PctNegative#NetYield");
		for(Map.Entry<String, List<Double>> entry : symbolYields.entrySet())
		{
			int totalPlus = 0;
			int totalMinus = 0;
			double netYield = 0.0;
			StringBuilder sb = new StringBuilder();			
			sb.append(entry.getKey() + "#[");
			for(Double yield : entry.getValue())
			{
				if(yield > 0.0)
					totalPlus++;
				else
					totalMinus++;
				
				netYield += yield;
				sb.append(decimalFormatter.format(yield) + ", ");
			}
			sb.append("]#");
			sb.append(totalPlus+"#");
			sb.append(totalMinus+"#");
			sb.append((int)((double)totalPlus*100.0/(double)(totalPlus+totalMinus)) + "#");
			sb.append((int)((double)totalMinus*100.0/(double)(totalPlus+totalMinus)) + "#");
			sb.append(decimalFormatter.format(netYield));
			writer.println(sb.toString());			
		}
	}
	
	public void reportDailyValues()
	{
		
	}
	
	private void initBuyDecisions()
	{
		TrailingEarningDaysBuyDecision earningDecision = new TrailingEarningDaysBuyDecision(trailingRangeForBuy);
		PrevQtrUpsideBuyDecision qtrDecision = new PrevQtrUpsideBuyDecision();
		
		buyDecisions.add(earningDecision);
		buyDecisions.add(qtrDecision);		
	}
	
	private void initSellDecisions()
	{
		TrailingEarningDaysSellDecision earningDaysDecision = new TrailingEarningDaysSellDecision(trailingDayForSell);
		MinYieldSellDecision yieldDecision = new MinYieldSellDecision(yieldPctToSell);
		MaxLossSellDecision lossDecision = new MaxLossSellDecision(maxLossPctToSell);
		
		sellDecisions.add(earningDaysDecision);
		sellDecisions.add(yieldDecision);
		sellDecisions.add(lossDecision);		
	}
	
	private void closePositions(List<Position> toSell, LocalDate today)
	{
		for(Position pos : toSell)
		{
			positions.remove(pos.getSymbol());
			
			addSymbolYields(pos.getSymbol(), pos.getYieldPctForPrice(pos.getSellPrice()));
			
			writer.printPositionSold(pos, pos.getSellPrice());
		}
	}
	
	private void openPositions(List<Position> candidates, LocalDate today)
	{
		List<String> symbols = new LinkedList<String>();
		for(Position pos : candidates)
		{
			symbols.add(pos.getSymbol());
		}
		
		Map<String, Double> ticks = db.getTicksForSymbols(symbols, today, "close");		
		for(Iterator<Position> iter = candidates.iterator(); iter.hasNext();)
		{
			Position pos = iter.next();
			if(!ticks.containsKey(pos.getSymbol()))
				iter.remove();
			else{
				double price = ticks.get(pos.getSymbol());
				int numShares = 100;
				pos.setBuyPosition(price, numShares, today);
				writer.printPositionBought(pos);				
			}
		}		
		
		for(Position pos : candidates)
		{
			positions.put(pos.getSymbol(), pos);			
		}
	}		
			
	private void addSymbolYields(String symbol, Double yield)
	{
		List<Double> yieldList;
		if(symbolYields.containsKey(symbol))
			yieldList = symbolYields.get(symbol);
		else{
			yieldList = new ArrayList<Double>();
			symbolYields.put(symbol, yieldList);
		}		
		yieldList.add(yield);
	}		
	
	
}
