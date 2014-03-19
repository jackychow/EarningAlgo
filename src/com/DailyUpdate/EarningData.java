package com.DailyUpdate;

import java.util.Date;

public class EarningData {

	public String quarter;
	public Date earningDate;
	public double cons;
	
	public EarningData()
	{
		
	}
	
	public EarningData(String quarter, Date earningData, double cons)
	{
		this.quarter = quarter;
		this.earningDate = earningData;
		this.cons = cons;
	}
}
