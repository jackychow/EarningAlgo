package com.AlgoSimulation;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;

public class Position {
	private String symbol;
	private double boughtPrice;
	private double sellPrice;
	private int numShares;
	private LocalDate boughtDate;
	
	public List<String> buyDecisions = new ArrayList<String>();
	public List<String> sellDecisions = new ArrayList<String>();
	
	//public LocalDate earningDate; //cheating and make things easier
	//public String qtr;
	
	public Earning earning;
	
	public Position(String symbol)
	{
		this.symbol = symbol;
		this.boughtPrice = -1;
		this.sellPrice = -1;
		this.numShares = -1;
	}
	
	public Position(String symbol, double price, int numShares)
	{
		this.symbol = symbol;
		this.boughtPrice = price;
		this.numShares = numShares;
		this.sellPrice = -1;
	}
	
	public void addEarning(LocalDate earningDate, String qtr, double cons)
	{
		Earning e = new Earning();
		e.earningDate = earningDate;
		e.qtr = qtr;
		e.cons = cons;
		this.earning = e;
	}
	
	public void setBuyPosition(double buyPrice, int numShares, LocalDate boughtDate)
	{
		this.boughtPrice = buyPrice;
		this.numShares = numShares;
		this.boughtDate = boughtDate;
	}
		
	public String getSymbol()
	{
		return symbol;
	}
	
	public double getBoughtPrice()
	{
		return boughtPrice;
	}
	
	public void setSellPrice(double price)
	{
		this.sellPrice = price;
	}
	
	public double getSellPrice()
	{
		return sellPrice;
	}
	
	public int getNumShares()
	{
		return numShares;
	}
	
	public LocalDate getBoughtDate()
	{
		return boughtDate;
	}
	
	public double getYieldPctForPrice(double price)
	{
		return ((price - boughtPrice)/boughtPrice)*100.0;
	}
	
	public String printBuyDecisions()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("BuyDecisions: ");
		sb.append(buyDecisions);
		return sb.toString();
	}
	
	public String printSellDecisions()
	{
		StringBuilder sb = new StringBuilder();		
		sb.append(" SellDecisions: ");
		sb.append(sellDecisions);
		return sb.toString();				
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("[Symbol: ");
		sb.append(symbol);
		sb.append(", BoughtPrice: ");
		sb.append(boughtPrice);
		sb.append(", numShares: ");
		sb.append(numShares);
		sb.append(", boughtDate: ");
		sb.append(boughtDate.toString("yyyy-MM-dd"));
		sb.append("] ");
		return sb.toString();
	}
	
	public static class Earning
	{
		public LocalDate earningDate;
		public String qtr;
		public double cons;
	}
}
