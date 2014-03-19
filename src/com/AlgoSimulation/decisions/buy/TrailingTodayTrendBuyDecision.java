package com.AlgoSimulation.decisions.buy;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.joda.time.LocalDate;

import com.AlgoSimulation.AlgoLogger;
import com.AlgoSimulation.DatabaseAccessor;
import com.AlgoSimulation.DateUtil;
import com.AlgoSimulation.Globals;
import com.AlgoSimulation.IBuyDecision;
import com.AlgoSimulation.Position;

public class TrailingTodayTrendBuyDecision implements IBuyDecision {

	private int[] trailingRangeForAnalaysis;
	private DecimalFormat decimalFormatter = new DecimalFormat("#.##");
	private final DatabaseAccessor db = DatabaseAccessor.getInstance();	
	private AlgoLogger logger = AlgoLogger.getInstance();
	
	public TrailingTodayTrendBuyDecision(int[] trailingRangeForAnalaysis)
	{
		this.trailingRangeForAnalaysis = trailingRangeForAnalaysis;
	}
	
	@Override
	public List<Position> getBuyCandidates(LocalDate today, List<Position> constraints) {

		if(constraints == null)
			return new LinkedList<Position>();
		
		Map<Position, List<Double>> ticks = getTrailingTicksForPositions(constraints, today);
		NavigableMap<Double, Position> orderedPos = getTrailingTrendOrderForPosition(ticks);
		
		List<Position> ret = new LinkedList<Position>();
		//reversing the order as treemap sort wit ascending order
		for(Map.Entry<Double, Position> entry : orderedPos.entrySet())
		{
			Position pos = entry.getValue();
			String info = "[" + Globals.BuyDecisions.TRAILIN_TODAY_TREND.name() + " : " +
			decimalFormatter.format(entry.getKey()) + "]";
			pos.buyDecisions.add(info);
			ret.add(pos);
		}
		
		return ret;
	}
	
	private Map<Position, List<Double>> getTrailingTicksForPositions(List<Position> positions, LocalDate today)
	{
		LocalDate endDate = DateUtil.GetTrailingAheadDays(today, trailingRangeForAnalaysis[1]);
		LocalDate startDate = DateUtil.GetTrailingAheadDays(today, trailingRangeForAnalaysis[0]);					
		
		return getTicksBetweenDatesForSymbols(positions, startDate, endDate);
	}
	
	private Map<Position, List<Double>> getTicksBetweenDatesForSymbols(List<Position> positions, LocalDate startDate, LocalDate endDate)
	{
		Map<Position, List<Double>> posTicksMap = new HashMap<Position, List<Double>>();
		PreparedStatement st = db.prepareStatement(
				"SELECT t.close " +
				"FROM ticks t " +
				"INNER JOIN symbols s ON s.id = t.symbol_id " +
				"WHERE (t.date >= ? AND t.date <= ?) " +
				"AND s.tick_symbol = ? ORDER BY t.date ASC ");
		
		try{
			for(Position pos : positions)
			{
				List<Double> ticks = new ArrayList<Double>();
				try {
					st.setDate(1, new Date(startDate.toDate().getTime()));
					st.setDate(2, new Date(endDate.toDate().getTime()));
					st.setString(3, pos.getSymbol());
					
					ResultSet rs = st.executeQuery();
					
					while(rs.next())
					{
						ticks.add(rs.getDouble(1));
					}
					
					rs.close();
				} catch (SQLException e) {
					logger.warning("SQL Exception querying for symbol :"+pos.getSymbol()+e.getMessage());
				}
				posTicksMap.put(pos, ticks);
			}
		}catch(Exception e){
			logger.warning("Unknown excecption getting ticksbetweendates"+e.getMessage());
		}finally{
			try {
				st.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		return posTicksMap;
	}
	
	private NavigableMap<Double, Position> 
	getTrailingTrendOrderForPosition(Map<Position, List<Double> > poistionTicks)
	{
		TreeMap<Double, Position> orderedPosition = new TreeMap<Double, Position>();
		
		for(Map.Entry<Position, List<Double>> entry : poistionTicks.entrySet()){
			
			SimpleRegression regress = new SimpleRegression();
			List<Double> ticks = entry.getValue();
			for(int i=0; i<ticks.size(); i++)
			{
				regress.addData((i+1), ticks.get(i));
			}
			
			if(regress.getSlope() > 0.0)
			{
				orderedPosition.put(regress.getSlope(), entry.getKey());
			}
		}		
		
		return orderedPosition.descendingMap();
	}

}
