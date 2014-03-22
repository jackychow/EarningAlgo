package com.AlgoSimulation.decisions.sell;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.List;

import org.joda.time.LocalDate;

import com.AlgoSimulation.AlgoLogger;
import com.AlgoSimulation.AlgoReportWriter;
import com.AlgoSimulation.DatabaseAccessor;
import com.AlgoSimulation.DateUtil;
import com.AlgoSimulation.Globals;
import com.AlgoSimulation.ISellDecision;
import com.AlgoSimulation.Position;

public class IndepTrailingEarningDaysSellDecision implements ISellDecision {

	private int trailingDayForSell;
	
	public IndepTrailingEarningDaysSellDecision(int trailingDayForSell)
	{
		this.trailingDayForSell = trailingDayForSell;
	}
	
	
	@Override
	public List<Position> getSellCandidates(List<Position> positions, LocalDate today) {

		LocalDate boundary = DateUtil.GetTrailingFutureDays(today, trailingDayForSell);
				
		List<Position> toSell = new LinkedList<Position>();
		for(Position pos : positions)
		{
			LocalDate earningDate = getNearestEarningDateForSymbol(pos.getSymbol(), today);
			if(earningDate.isEqual(boundary) || earningDate.isBefore(boundary))
			{
				String info = "["+Globals.SellDecisions.TRAILING_EARNING_DAYS.name() + " : " +
				earningDate.toString("yyyy-MM-dd") +"]";
				
				pos.sellDecisions.add(info);
				toSell.add(pos);
			}			
		}
		
		for(Position pos : toSell)
		{
			positions.remove(pos);
		}
		
		return toSell;
	}
	
	private LocalDate getNearestEarningDateForSymbol(String symbol, LocalDate today)
	{
		PreparedStatement st = DatabaseAccessor.getInstance().prepareStatement(
				"select earning_date " +
				"from earning_events ee, symbols s " +
				"where ee.symbol_id = s.id " +
				"and s.tick_symbol = ? " +
				"and ee.earning_date > ? " +
				"order by earning_date asc limit 1");
		
		LocalDate earningDate = null;
		try{
			st.setString(1, symbol);
			st.setDate(2, new java.sql.Date(today.toDateTimeAtStartOfDay().toDate().getTime()));
			
			ResultSet rs = st.executeQuery();						
			
			while(rs.next()){
				earningDate = LocalDate.fromDateFields(rs.getDate(1));
			}
			
			rs.close();
			st.close();
		}catch (Exception e)
		{
			AlgoLogger.getInstance().warning("[ERROR] Exception Failed to get nearest earning date for "+symbol+e.getMessage());
		}
		
		return earningDate;
	}

}
