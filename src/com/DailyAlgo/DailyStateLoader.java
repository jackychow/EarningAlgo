package com.DailyAlgo;

import java.text.DecimalFormat;
import java.util.List;

import com.AlgoSimulation.Position;

public class DailyStateLoader {

	private int[] trailingRangeForBuy = new int[2];	
	private int trailingDayForSell;
	private double minAllocation;
	private double maxAllocation;
	private double cash;
	private double brokageFee;	
	
	private List<Position> positions;
	
	private final DecimalFormat decimalFormatter = new DecimalFormat("#.##");
	
	public DailyStateLoader(){
		trailingRangeForBuy[0] = Integer.parseInt(AppPropertyValues.GetValueForProperty("trailingRangeForBuy_start"));
		trailingRangeForBuy[1] = Integer.parseInt(AppPropertyValues.GetValueForProperty("trailingRangeForBuy_end"));
		trailingDayForSell = Integer.parseInt(AppPropertyValues.GetValueForProperty("trailingDayForSell"));
		minAllocation = Double.parseDouble(AppPropertyValues.GetValueForProperty("minAllocation"));
		maxAllocation = Double.parseDouble(AppPropertyValues.GetValueForProperty("maxAllocation"));
		brokageFee = Double.parseDouble(AppPropertyValues.GetValueForProperty("brokageFee"));
		cash = Double.parseDouble(AppPropertyValues.GetValueForProperty("cash"));
		
		positions = PositionAccessor.LoadAllActivePositions();
	}

	public int[] getTrailingRangeForBuy() {
		return trailingRangeForBuy.clone();
	}

	public int getTrailingDayForSell() {
		return trailingDayForSell;
	}

	public double getMinAllocation() {
		return minAllocation;
	}

	public double getMaxAllocation() {
		return maxAllocation;
	}

	public double getCash() {
		return cash;
	}

	public List<Position> getPositions() {
		return positions;
	}

	public double getBrokageFee() {
		return brokageFee;
	}
	
	
}
