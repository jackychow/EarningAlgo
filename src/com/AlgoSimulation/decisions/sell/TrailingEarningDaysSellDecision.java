package com.AlgoSimulation.decisions.sell;

import java.util.LinkedList;
import java.util.List;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;

import com.AlgoSimulation.AlgoReportWriter;
import com.AlgoSimulation.DatabaseAccessor;
import com.AlgoSimulation.DateUtil;
import com.AlgoSimulation.Globals;
import com.AlgoSimulation.ISellDecision;
import com.AlgoSimulation.Position;

public class TrailingEarningDaysSellDecision implements ISellDecision {

	private int trailingDayForSell;
	
	public TrailingEarningDaysSellDecision(int trailingDayForSell)
	{
		this.trailingDayForSell = trailingDayForSell;
	}
	
	
	@Override
	public List<Position> getSellCandidates(List<Position> positions, LocalDate today) {

		LocalDate boundary = DateUtil.GetTrailingFutureDays(today, trailingDayForSell);
				
		List<Position> toSell = new LinkedList<Position>();
		for(Position pos : positions)
		{
			LocalDate earningDate = pos.earning.earningDate;
			if(earningDate.isEqual(boundary) || earningDate.isBefore(boundary))
			{
				double price = DatabaseAccessor.getInstance().getTickForSymbol(pos.getSymbol(), today, "close");
				if(price < 0.0)
				{
					AlgoReportWriter.GetInstance().println("[ERROR] Failed to get Price for symbol: "+pos.getSymbol()+ " for " +
							today.toString("yyyy-MM-dd")+" Cannot SELL");
					continue;
				}
				pos.setSellPrice(price);
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
}
