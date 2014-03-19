package com.AlgoSimulation.decisions.buy;

import java.util.LinkedList;
import java.util.List;

import org.joda.time.LocalDate;

import com.AlgoSimulation.BrokerManager;
import com.AlgoSimulation.Globals;
import com.AlgoSimulation.IBuyDecision;
import com.AlgoSimulation.Position;

public class MinCashAllocationBuyDecision implements IBuyDecision {

	private double minAllocation;	
	
	public MinCashAllocationBuyDecision(double minAllocation)
	{		
		this.minAllocation = minAllocation;
	}
	
	@Override
	public List<Position> getBuyCandidates(LocalDate today,
			List<Position> constraints) {
		
		if(constraints == null)
			return new LinkedList<Position>();

		double cash = BrokerManager.GetInstance().getCash();
		int maxBuy = (int)(cash/minAllocation);
		int total = constraints.size();
		
		if(constraints.size() > maxBuy){
			int toRemove = constraints.size() - maxBuy;
			while(toRemove > 0)
			{
				constraints.remove(constraints.size()-1);
				toRemove--;
			}
		}
		
		String info = "[" + Globals.BuyDecisions.MIN_CASH_ALLOC.name() + " Dropped: " + Math.max((total - maxBuy), 0) + "]";
		
		for(Position pos : constraints)
		{
			pos.buyDecisions.add(info);
		}
		
		return constraints;
	}

}
