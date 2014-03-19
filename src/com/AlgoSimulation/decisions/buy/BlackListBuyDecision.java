package com.AlgoSimulation.decisions.buy;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.joda.time.LocalDate;

import com.AlgoSimulation.AlgoLogger;
import com.AlgoSimulation.IBuyDecision;
import com.AlgoSimulation.Position;

public class BlackListBuyDecision implements IBuyDecision {

	private Set<String> blacklist;
		
	public BlackListBuyDecision(String file)
	{
		blacklist = new HashSet<String>(); 
		Path path = Paths.get(file);
		try {
			BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
			
			String line = null;
			
			while((line = reader.readLine())!= null)
			{									
				String symbol = line.trim();
				blacklist.add(symbol);
			}			
			reader.close();
		
		} catch (IOException e) {
			AlgoLogger.getInstance().warning("Failed to open file: "+file+e.getMessage());
		}
	}

	@Override
	public List<Position> getBuyCandidates(LocalDate today, List<Position> constraints) {

		List<Position> ret = new LinkedList<Position>();
		if(constraints == null)
			return ret;
		
		for(Position pos : constraints)
		{
			if(!blacklist.contains(pos.getSymbol()))
			{
				ret.add(pos);				
			}
		}
		
		return ret;
	}
	
	
	
	
}
