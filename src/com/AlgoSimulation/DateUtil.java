package com.AlgoSimulation;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;

public class DateUtil {

	public static LocalDate GetTrailingAheadDays(LocalDate startDay, int numDays)
	{
		int cnt = 0;
		LocalDate endDay = startDay;
		while(cnt < numDays)
		{
			endDay = endDay.minusDays(1);
			if(endDay.getDayOfWeek() != DateTimeConstants.SUNDAY &&
					endDay.getDayOfWeek() != DateTimeConstants.SATURDAY)
			{
				cnt++;
			}			
		}
		return endDay;		
	}
	
	public static LocalDate GetTrailingFutureDays(LocalDate startDay, int numDays)
	{
		int cnt = 0;
		LocalDate endDay = startDay;
		while(cnt < numDays)
		{
			endDay = endDay.plusDays(1);
			if(endDay.getDayOfWeek() != DateTimeConstants.SUNDAY &&
					endDay.getDayOfWeek() != DateTimeConstants.SATURDAY)
			{
				cnt++;
			}			
		}
		return endDay;		
	}

}
