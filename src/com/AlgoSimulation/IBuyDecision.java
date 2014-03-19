package com.AlgoSimulation;

import java.util.List;

import org.joda.time.LocalDate;

public interface IBuyDecision {

	public List<Position> getBuyCandidates(LocalDate today, List<Position> constraints);
}
