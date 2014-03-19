package com.AlgoSimulation;

public class Globals {

	public static enum BuyDecisions{
		TRAILING_EARNING_DAYS,
		TRAILIN_TODAY_TREND,
		MIN_CASH_ALLOC,
		PREV_QTR_UPSIDE
	}
	
	public static enum SellDecisions{
		TRAILING_EARNING_DAYS,
		MIN_YIELD,
		MAX_LOSS
	}
}
