package com.AlgoSimulation;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.joda.time.Days;
import org.joda.time.LocalDate;

public class BrokerManager {
	private static final BrokerManager instance = new BrokerManager();
	private double cash;
	//private TreeMap<LocalDate, ArrayList<TransactionItem>> transactions 
	//= new TreeMap<LocalDate, ArrayList<TransactionItem>>();
	private Map<String, TransactionItem> transactions = new HashMap<String, TransactionItem>();
	
	private double brokageFee = 10.0;
	
	private final AlgoLogger logger = AlgoLogger.getInstance();
	
	private BrokerManager()
	{
		
	}
	
	public static BrokerManager GetInstance()
	{
		return instance;
	}
	
	public void reset()
	{
		cash = 0.0;
		transactions.clear();
	}
	
	public void addFund(double cash)
	{
		this.cash = cash;
	}
	
	public double getBrokageFee()
	{
		return brokageFee;
	}
	
	public boolean buyShares(String symbol, double price, int numShares, LocalDate day)
	{		
		TransactionItem item = new TransactionItem(symbol, price, numShares, day);
		if(cash < item.getPrincipal())
		{
			logger.warning("Not Enough Cash for Transaction: ["+symbol+" "+price+" "+numShares+" "+day.toString("yyyy-MM-dd"));	
			return false;
		}else{
			transactions.put(symbol, item);
			cash -= item.getPrincipal();
			cash -= brokageFee;
			return true;
		}		
	}
	
	public boolean sellShares(String symbol, double price, LocalDate day)
	{
		TransactionItem item = transactions.get(symbol);
		if(item == null)
		{
			logger.warning("Not Transaction to sell: ["+symbol+" "+price+" "+day.toString("yyyy-MM-dd"));
			return false;
		}else{
			cash += item.sellAtPrice(price);
			cash -= brokageFee;
			transactions.remove(symbol);
			return true;
		}
	}
	
	public void reclaimAllPositions(LocalDate day)
	{
		List<TransactionItem> items = new LinkedList<TransactionItem>();
		for(TransactionItem item : transactions.values())
		{
			items.add(item);
		}
		
		for(TransactionItem item : items)
		{
			sellShares(item.symbol, item.boughtprice, day);
		}
	}
	
	public double getAcctNetworth(LocalDate day)
	{
		double total = cash;
		
		
		//List<String> symbols = new LinkedList<String>(transactions.keySet());
		//Map<String, Double> prices = DatabaseAccessor.getInstance().getTicksForSymbols(symbols, day, "close");
		
		for(TransactionItem item : transactions.values())
		{
			total += item.getPrincipal();
			/*
			if(prices.containsKey(item.symbol))
			{
				total += item.getValue(prices.get(item.symbol));
			} else {
				return -1; //return -1 immediately to indicate somefailure
			}
			*/
		}
		
		return total;
	}
	
	public double getCash()
	{
		return cash;
	}	
	
	public String getSummary()
	{
		StringBuilder sb = new StringBuilder();		
		sb.append("[Cash In Hand: ");
		sb.append(cash);
		sb.append("]\n");
		
		for(TransactionItem item : transactions.values())
		{
			sb.append(item.toString());
			sb.append("\n");
		}
		return sb.toString();
	}
	
	private class TransactionItem{
		public double boughtprice;
		public double sellprice;
		public int shareBought;
		private String symbol;
		public LocalDate dayBought;		
		
		public TransactionItem(String symbol, double boughtprice, int shareBought, LocalDate day)
		{
			this.symbol = symbol;
			this.boughtprice = boughtprice;
			this.shareBought = shareBought;
			this.dayBought = day;
		}
		
		public double sellAtPrice(double price)
		{
			return ((double)shareBought)*price;
		}
		
		public int numberOfDaysHeld(LocalDate today)
		{
			return Days.daysBetween(dayBought, today).getDays();
		}
		
		public double getYieldAmt()
		{
			return ((double)shareBought)*(sellprice - boughtprice);
		}
		
		public double getValue(double price)
		{
			return ((double)shareBought)*price;
		}
		
		public double getPrincipal()
		{
			return ((double)shareBought)*boughtprice;
		}
		
		public String toString()
		{
			StringBuilder sb = new StringBuilder();
			sb.append("[Symbol: ");
			sb.append(symbol);
			sb.append(", boughtOn: ");
			sb.append(dayBought.toString("yyyy-MM-dd"));			
			sb.append(", boughtPrice: ");
			sb.append(boughtprice);
			sb.append(", SharesBought: ");
			sb.append(shareBought);
			sb.append(", TotalyPrincipal: ");
			sb.append(this.getPrincipal());
			sb.append("]");
			return sb.toString();
		}
	}
}
