package com.AlgoSimulation;

public class FixPeriodSettings {
	private int[] initialTrailingRangeForBuy = {29, 26};	
	private int initialTrailingDayForSell = 1;

	//days are inclusive
	private int[] trailingRangeForBuy;	
	private int trailingDayForSell;
	
	private int minTrailingRangeForBuy = 5;
	private int maxTrailingDayForSell = 16;	
	
	public FixPeriodSettings()
	{
		resetRangeForBuy();
		resetDayForSell();
	}
	
	public void setSettings(int[] trailingRangeForBuy, int trailingDayForSell)
	{
		this.trailingRangeForBuy = trailingRangeForBuy;
		this.trailingDayForSell = trailingDayForSell;
	}
	
	public void resetRangeForBuy()
	{
		trailingRangeForBuy = initialTrailingRangeForBuy.clone();		
	}
	
	public void resetDayForSell()
	{
		trailingDayForSell = initialTrailingDayForSell;		
	}
	
	public boolean advanceRangeForBuy(int days)
	{
		if(trailingRangeForBuy[1] > minTrailingRangeForBuy)
		{
			trailingRangeForBuy[0] -= days;
			trailingRangeForBuy[1] -= days;
			return true;
		}
		return false;
	}
	
	public boolean advanceDayForSell(int days)
	{
		if(trailingDayForSell < maxTrailingDayForSell &&
				trailingDayForSell < trailingRangeForBuy[0])
		{
			trailingDayForSell += days;
			return true;
		}
		
		return false;
	}
	
	public int[] getTrailingRangeForBuy()
	{
		return trailingRangeForBuy.clone();
	}
	
	public int getTrailingDayForSell()
	{
		return trailingDayForSell;
	}
	
	public String toString()
	{
		return "_" + trailingRangeForBuy[0] + "_" + trailingRangeForBuy[1] + "_" +trailingDayForSell;
	}

}
