package com.AlgoSimulation;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.joda.time.LocalDate;

import com.AlgoSimulation.decisions.buy.BlackListBuyDecision;
import com.AlgoSimulation.decisions.buy.MinCashAllocationBuyDecision;
import com.AlgoSimulation.decisions.buy.PrevQtrUpsideBuyDecision;
import com.AlgoSimulation.decisions.buy.TrailingEarningDaysBuyDecision;
import com.AlgoSimulation.decisions.buy.TrailingTodayTrendBuyDecision;
import com.AlgoSimulation.decisions.sell.MaxLossSellDecision;
import com.AlgoSimulation.decisions.sell.MinYieldSellDecision;
import com.AlgoSimulation.decisions.sell.TrailingEarningDaysSellDecision;

public class FixPeriodAheadEarningAlgo {

	//days are inclusive
	private int[] trailingRangeForBuy;	
	private int trailingDayForSell;
		
	private double yieldPctToSell = 15.0;
	private double maxLossPctToSell = -25.0;
	private double minAllocation = 5000;
	//private double minAllocation = 8350.0;
	private double maxAllocation = 10000;
	//private double maxAllocation = 16667.0;
	private int[] trailingRangeForAnalysis = {5, 1};

	private Map<String, Position> positions = new HashMap<String, Position>();
	private List<IBuyDecision> buyDecisions = new ArrayList<IBuyDecision>();
	private List<ISellDecision> sellDecisions = new ArrayList<ISellDecision>();
	
	private int currentDayCnt = 0;
	private double initialDailyLimit;
	private int initialDaysOfCover;
	
	private boolean reportDailyValue = false;
	
	private BrokerManager broker = BrokerManager.GetInstance(); 
	private DatabaseAccessor db = DatabaseAccessor.getInstance();
	private AlgoLogger logger = AlgoLogger.getInstance();
	private AlgoReportWriter writer = AlgoReportWriter.GetInstance();
	
	private Map<String, List<Double>> symbolYields = new HashMap<String, List<Double>>();
	private List<DailyValuePoint> dailyValues = new LinkedList<DailyValuePoint>();
	private DecimalFormat decimalFormatter = new DecimalFormat("#.##");
	
	public FixPeriodAheadEarningAlgo(double cashToStart, FixPeriodSettings settings, boolean reportDailyValue)
	{
		trailingRangeForBuy = settings.getTrailingRangeForBuy();
		trailingDayForSell = settings.getTrailingDayForSell();
		
		initialDaysOfCover = trailingRangeForBuy[1] - trailingDayForSell;
		initialDailyLimit = Math.min((cashToStart/(double)initialDaysOfCover), maxAllocation);						
		
		
		initBuyDecisions();
		initSellDecisions();
		broker.reset();
		broker.addFund(cashToStart);
		
		this.reportDailyValue = reportDailyValue;
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
		sb.append(" Min Alloc: ");
		sb.append(minAllocation);
		sb.append(" Max Alloc: ");
		sb.append(maxAllocation);
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
		
		List<String> symbols = new LinkedList<String>();
		for(Position pos : toBuy)
		{
			symbols.add(pos.getSymbol());
		}
		Map<String, Double> prices = db.getTicksForSymbols(symbols, today, "close");		
		
		distributeCashToPositions(broker.getCash(), toBuy, today, prices);
		
		
		if(reportDailyValue)
		{
			double dailyValue = BrokerManager.GetInstance().getAcctNetworth(today);
		
			if(DoubleUtil.IsPositive(dailyValue)) //negative indicate some position didn't have price
				dailyValues.add(new DailyValuePoint(today, dailyValue));
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
		writer.println("======================== Daily Values Vectors ================================");
		for(DailyValuePoint point : dailyValues)
		{
			writer.println(point.date.toString("yyyy-MM-dd") + "," + decimalFormatter.format(point.value));
		}
	}
	
	private void initBuyDecisions()
	{
		TrailingEarningDaysBuyDecision earningDecision = new TrailingEarningDaysBuyDecision(trailingRangeForBuy);
		//TrailingTodayTrendBuyDecision trendDecision = new TrailingTodayTrendBuyDecision(trailingRangeForAnalysis);
		PrevQtrUpsideBuyDecision qtrDecision = new PrevQtrUpsideBuyDecision();
		String settingStr = "_" + trailingRangeForBuy[0] + "_" + trailingRangeForBuy[1] + "_" +trailingDayForSell;
		BlackListBuyDecision blacklistDecision = new BlackListBuyDecision("F:\\FinancialData\\Lists\\AlgoBlackList"+settingStr+".txt");
		MinCashAllocationBuyDecision alloDecision = new MinCashAllocationBuyDecision(minAllocation);
		
		buyDecisions.add(earningDecision);
		buyDecisions.add(qtrDecision);
		buyDecisions.add(blacklistDecision);
		buyDecisions.add(alloDecision);		
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
			if(!broker.sellShares(pos.getSymbol(), pos.getSellPrice(), today)){
				logger.warning("Something is wrong, tryiing to sell a position that does not exist "+pos);
			}
			addSymbolYields(pos.getSymbol(), pos.getYieldPctForPrice(pos.getSellPrice()));
			writer.printPositionSold(pos, pos.getSellPrice());
		}
	}
	
	private void openPositions(List<Position> candidates, LocalDate today)
	{
		for(Position pos : candidates)
		{
			if(broker.buyShares(pos.getSymbol(), pos.getBoughtPrice(), pos.getNumShares(), today))
			{
				writer.printPositionBought(pos);
				positions.put(pos.getSymbol(), pos);
			}
		}
	}		
	
	private void distributeCashToPositions(double cash, List<Position> candidates, LocalDate today, Map<String, Double> prices)
	{
		evenCashDistribution(cash, candidates, today, prices);		
	}
	
	private void evenCashDistribution(double cash, List<Position> candidates, LocalDate today, Map<String, Double> ticks)
	{
		double cashPerCandidate = 0.0;
		
		cash -= BrokerManager.GetInstance().getBrokageFee()*(double)candidates.size();
		if(currentDayCnt <= initialDaysOfCover)
			cashPerCandidate = initialDailyLimit/(double)candidates.size();
		else
			cashPerCandidate = Math.min(cash/((double)candidates.size()), maxAllocation);
				
		//for(Position pos : candidates)
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
	
	private static class DailyValuePoint
	{			
		private LocalDate date;
		private double value;
		
		public DailyValuePoint(LocalDate date, double value)
		{
			this.date = date;
			this.value = value;
		}
	}
}
