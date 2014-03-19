package com.AlgoSimulation;

import java.util.List;

import org.joda.time.LocalDate;

public interface ISellDecision {

	public List<Position> getSellCandidates(List<Position> positions, LocalDate today);
		
}
