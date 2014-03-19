package com.AlgoSimulation;

import java.io.IOException;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.text.DecimalFormat;
import java.util.*;

public class SimulationDriver {
	
	
	public static void main(String[] argv)
	{
		DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd");
		LocalDate start = formatter.parseLocalDate("2011-01-02");
		LocalDate end = formatter.parseLocalDate("2012-01-02");
		LocalDate start1 = formatter.parseLocalDate("2012-01-02");
		LocalDate end1 = formatter.parseLocalDate("2013-01-02");		
		LocalDate start2 = formatter.parseLocalDate("2013-01-02");
		LocalDate end2 = formatter.parseLocalDate("2014-01-02");		
		LocalDate start3 = formatter.parseLocalDate("2011-01-02");
		LocalDate end3 = formatter.parseLocalDate("2014-01-02");		

		
		double cash = 60000.0;
		
		SimulationDriver driver = new SimulationDriver();
		
		/*
		driver.simulateAllSettings(start, end, cash);
		
		
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		driver.simulateAllSettings(start1, end1, cash);		

		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		driver.simulateAllSettings(start2, end2, cash);
		
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		driver.simulateAllSettings(start3, end3, cash);
		*/
		
		/*
		FixPeriodSettings settings = new FixPeriodSettings();
		int[] rangeBuy = {16, 13};
		settings.setSettings(rangeBuy, 7);
		driver.simulateDatesWithReport(start3, end3, cash, settings);
		*/
						
	}

	private DecimalFormat decimalFormatter = new DecimalFormat("#.##");
		
	public void simulateAllSettings(LocalDate startDay, LocalDate endDay, double cash)
	{
		AlgoReportWriter writer = AlgoReportWriter.GetInstance();
		FixPeriodSettings settings = new FixPeriodSettings();
		writer.setEnable(false);
		try {
			writer.openFile("F:\\Development\\logs\\AlgoReports\\");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		while(settings.advanceRangeForBuy(1))
		{			
			while(settings.advanceDayForSell(1))
			{
				String info = "[" +settings.getTrailingRangeForBuy()[0] +" - "+settings.getTrailingRangeForBuy()[1] + "]";
				System.out.println("Simulating: "+info+" , "+ settings.getTrailingDayForSell());				
				
				simulateDates(startDay, endDay, cash, settings);
				
				StringBuilder sb = new StringBuilder();
				sb.append(info);
				sb.append(",");
				sb.append(settings.getTrailingDayForSell());
				sb.append(",");
				sb.append(decimalFormatter.format(BrokerManager.GetInstance().getCash()));
				writer.printlnImportant(sb.toString());
			}			
			settings.resetDayForSell();
		}
		
		writer.closeAll();
	}
	
	public void simulateDates(LocalDate startDay, LocalDate endDay, double cash, FixPeriodSettings settings)
	{
		LocalDate today = startDay;
		FixPeriodAheadEarningAlgo algo = new FixPeriodAheadEarningAlgo(cash, settings, false);
		/*
		AlgoReportWriter writer = AlgoReportWriter.GetInstance();
		try {
			writer.openFile("F:\\Development\\logs\\AlgoReports\\");
		} catch (IOException e) {
			e.printStackTrace();
		}
		*/
		
		//writer.println("Report For Period "+startDay.toString("yyyyMMdd") + " to " +endDay.toString("yyyyMMdd"));
		while(!today.isEqual(endDay))
		{
			//System.out.println("Running for: "+today.toString("yyyy-MM-dd"));
			if(today.getDayOfWeek() == DateTimeConstants.SATURDAY ||
					today.getDayOfWeek() == DateTimeConstants.SUNDAY)
			{
				//writer.println("xxxx Skipping "+today.toString("yyyyMMdd")+ " because this is a saturday or sunday xxxxx");
				today = today.plusDays(1);
				continue;
			}
			
			algo.run(today);
			
			today = today.plusDays(1);
		}
		
		BrokerManager.GetInstance().reclaimAllPositions(today);
		//writer.printTodaySummary(today);
		
		//writer.closeAll();
	}

	
	public void simulateDatesWithReport(LocalDate startDay, LocalDate endDay, double cash, FixPeriodSettings settings)
	{
		LocalDate today = startDay;
		FixPeriodAheadEarningAlgo algo = new FixPeriodAheadEarningAlgo(cash, settings, true);
		//UnConstraintedAlgo algo = new UnConstraintedAlgo(cash, settings);
		
		AlgoReportWriter writer = AlgoReportWriter.GetInstance();
		try {
			writer.openFile("F:\\Development\\logs\\AlgoReports\\");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		writer.println("Report For Period "+startDay.toString("yyyyMMdd") + " to " +endDay.toString("yyyyMMdd"));
		writer.println(algo.getSettingsString());
		while(!today.isEqual(endDay))
		{
			System.out.println("Running for: "+today.toString("yyyy-MM-dd"));
			if(today.getDayOfWeek() == DateTimeConstants.SATURDAY ||
					today.getDayOfWeek() == DateTimeConstants.SUNDAY)
			{
				writer.println("xxxx Skipping "+today.toString("yyyyMMdd")+ " because this is a saturday or sunday xxxxx");
				today = today.plusDays(1);
				continue;
			}
			
			algo.run(today);
			
			today = today.plusDays(1);
		}
		
		BrokerManager.GetInstance().reclaimAllPositions(today);
		writer.printTodaySummary(today);
		algo.reportSummary();
		algo.reportDailyValues();
		writer.closeAll();
	}
	
	
}
