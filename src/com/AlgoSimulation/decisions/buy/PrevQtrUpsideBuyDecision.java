package com.AlgoSimulation.decisions.buy;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.joda.time.LocalDate;

import com.AlgoSimulation.DatabaseAccessor;
import com.AlgoSimulation.DoubleUtil;
import com.AlgoSimulation.Globals;
import com.AlgoSimulation.IBuyDecision;
import com.AlgoSimulation.Position;

public class PrevQtrUpsideBuyDecision implements IBuyDecision {

	private final DatabaseAccessor db = DatabaseAccessor.getInstance();	
	private DecimalFormat decimalFormatter = new DecimalFormat("#.##");
	
	@Override
	public List<Position> getBuyCandidates(LocalDate today, List<Position> constraints) {
		
		List<Position> ret = new LinkedList<Position>();
		
		if(constraints == null)
			return ret;

		NavigableMap<Double, Position> orderedPos = getQtrUpsideForPositions(constraints);
		
		//reversing the order as treemap sort wit ascending order
		for(Map.Entry<Double, Position> entry : orderedPos.entrySet())
		{
			//skip if this is a lower quarter
			if(DoubleUtil.IsNegative(entry.getKey()))
				continue;
			
			Position pos = entry.getValue();
			String info = "[" + Globals.BuyDecisions.PREV_QTR_UPSIDE.name() + " : " +
			decimalFormatter.format(entry.getKey()) + "]";
			pos.buyDecisions.add(info);
			ret.add(pos);
		}
		
		return ret;

	}

	
	private NavigableMap<Double, Position> getQtrUpsideForPositions(List<Position> positions)
	{
		TreeMap<Double, Position> orderedList = new TreeMap<Double, Position>();

		PreparedStatement st = db.prepareStatement(	
				"SELECT ee.eps " +
				"FROM earning_events ee " +
				"INNER JOIN symbols s ON s.id = ee.symbol_id " +
				"WHERE s.tick_symbol = ? " +
				"AND ee.quarter = ? ");		
			
		try {
				
			for(Position pos : positions)
			{				
				String currQtr = pos.earning.qtr;
				double cons = pos.earning.cons;
				
				if(DoubleUtil.EqualsZero(cons))
					continue;
				
				int yr = Integer.parseInt(currQtr.substring(2));
				yr -= 1;
				String lastQtr = currQtr.substring(0, 2) + yr;
				//double lasteps = pos.earning.cons;
				Double lasteps = null;
				
				st.setString(1, pos.getSymbol());
				st.setString(2, lastQtr);
				
				ResultSet rs = st.executeQuery();
				
				while(rs.next())
				{
					lasteps = rs.getDouble(1);
				}
				
				rs.close();

				if(lasteps == null || DoubleUtil.EqualsZero(lasteps))
					continue;				
				
				double upside = (cons - lasteps)/Math.abs(lasteps);								
				orderedList.put(upside, pos);				
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				st.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}	
		
		return orderedList.descendingMap();
	}	
}
