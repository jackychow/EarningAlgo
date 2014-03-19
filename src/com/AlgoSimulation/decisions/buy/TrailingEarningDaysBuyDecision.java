package com.AlgoSimulation.decisions.buy;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
//import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.joda.time.LocalDate;

import com.AlgoSimulation.DatabaseAccessor;
import com.AlgoSimulation.DateUtil;
import com.AlgoSimulation.Globals;
import com.AlgoSimulation.IBuyDecision;
import com.AlgoSimulation.Position;

public class TrailingEarningDaysBuyDecision implements IBuyDecision {

	private int[] trailingRangeForTrade;
	private DatabaseAccessor db = DatabaseAccessor.getInstance();
	
	public TrailingEarningDaysBuyDecision(int[] trailingRangeForTrade)
	{
		this.trailingRangeForTrade = trailingRangeForTrade;
	}
	
	@Override
	public List<Position> getBuyCandidates(LocalDate today,	List<Position> constraints) {
		return getCandidates(today);
	}
		
	private List<Position> getCandidates(LocalDate today)
	{
		List<Position> candidates = new LinkedList<Position>();
		LocalDate minEarningDayToConsider = DateUtil.GetTrailingFutureDays(today, trailingRangeForTrade[1]);
		LocalDate maxEarningDayToConsider = DateUtil.GetTrailingFutureDays(today, trailingRangeForTrade[0]);
		
		Date upperBound = new Date(maxEarningDayToConsider.toDate().getTime());
		Date lowerBound = new Date(minEarningDayToConsider.toDate().getTime());
				
		PreparedStatement st = db.prepareStatement(
				"SELECT s.tick_symbol, ee.earning_date, ee.quarter, ee.cons " +
				"FROM earning_events ee " +
				"INNER JOIN symbols s ON s.id = ee.symbol_id " +
				"WHERE ((earning_date = ? AND time = -1) OR earning_date < ?) " +
				"AND ((earning_date = ? AND time = -1) OR earning_date > ?) " +
				"AND ee.valid > 0" +
				"ORDER BY earning_date asc");
		
		try {
			st.setDate(1, upperBound);
			st.setDate(2, upperBound);
			st.setDate(3, lowerBound);
			st.setDate(4, lowerBound);
			
			ResultSet rs = st.executeQuery();
			
			while(rs.next())
			{
				Position pos = new Position(rs.getString(1));				
				pos.addEarning(LocalDate.fromDateFields(rs.getDate(2)), rs.getString(3), rs.getDouble(4));				
				pos.buyDecisions.add(Globals.BuyDecisions.TRAILING_EARNING_DAYS.name());
				candidates.add(pos);
			}
			
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				st.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}			
		}
		
		return candidates;
	}

}
