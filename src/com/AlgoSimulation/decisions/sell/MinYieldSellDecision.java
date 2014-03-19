package com.AlgoSimulation.decisions.sell;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.joda.time.LocalDate;

import com.AlgoSimulation.DatabaseAccessor;
import com.AlgoSimulation.Globals;
import com.AlgoSimulation.ISellDecision;
import com.AlgoSimulation.Position;

public class MinYieldSellDecision implements ISellDecision {

	private double minYieldPercent;
	private final DatabaseAccessor db = DatabaseAccessor.getInstance();
	
	public MinYieldSellDecision(double minYieldPercent)
	{
		this.minYieldPercent = minYieldPercent;
	}
	
	@Override
	public List<Position> getSellCandidates(List<Position> positions, LocalDate today) {

		List<String> symbols = new LinkedList<String>();
		List<Position> ret = new LinkedList<Position>();
		Map<String, Double> ticks = new HashMap<String, Double>();
		for(Position pos : positions)
		{
			if(pos.getSellPrice() > 0.0)
				ticks.put(pos.getSymbol(), pos.getSellPrice());
			else
				symbols.add(pos.getSymbol());
		}
		
		if(symbols.size() > 0)
			ticks.putAll(db.getTicksForSymbols(symbols, today, "close"));
		
		for(Position pos : positions)
		{
			if(!ticks.containsKey(pos.getSymbol()))
				continue;
			
			double price = ticks.get(pos.getSymbol());
			if(pos.getYieldPctForPrice(price) > minYieldPercent)
			{
				pos.sellDecisions.add(Globals.SellDecisions.MIN_YIELD.name());
				pos.setSellPrice(price);
				ret.add(pos);
			}
		}
		
		for(Position pos : ret)
		{
			positions.remove(pos);
		}
		
		return ret;
	}

}
