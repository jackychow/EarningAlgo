package com.AlgoSimulation;

import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.LocalDate;

import com.QuoteRetriever.MyLogger;

public class DatabaseAccessor {
	
	private static final DatabaseAccessor Instance = new DatabaseAccessor();
	private static final String url = "jdbc:postgresql://192.168.10.150/athena";
	private Connection db = null;
	private final MyLogger logger = MyLogger.getInstance();
	
	private DatabaseAccessor(){
		try {
			db = DriverManager.getConnection(url, "postgres", "j1c2s3k4");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static DatabaseAccessor getInstance()
	{
		return Instance;
	}

	public void testget()
	{
		try {
			Statement st = db.createStatement();
			ResultSet rs = st.executeQuery("SELECT * FROM symbols");
			
			while(rs.next())
			{
				System.out.println(rs.getInt(1));
				System.out.println(rs.getString(2));
			}
			
			rs.close();
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}	
	
	public int getIdForSymbol(String symbol)
	{
		int id = -1;
		try {
			PreparedStatement st = db.prepareStatement("SELECT id FROM symbols WHERE tick_symbol=?");
			st.setString(1, symbol);
			
			ResultSet rs = st.executeQuery();
			while(rs.next())
			{
				id = rs.getInt(1);
			}			
			rs.close();
			st.close();
		} catch (SQLException e) {
			logger.warning(e.toString());
			logger.warning("SQLException happened while retrieving id for symbol "+symbol);
		}		
		
		return id;
	}
	  	
	/**
	 * 
	 * @param symbols
	 * @param date
	 * @param type takes either adj, close, high
	 * @return
	 */
	public Map<String, Double> getTicksForSymbols(List<String> symbols, LocalDate date, String type)
	{
		String ptype = "adj";
		Map<String, Double> ret = new HashMap<String, Double>();
		
		if(type != null && 
				(type.equals("close") || type.equals("open") || type.equals("high") || type.equals("low")))
			ptype = type;

		
		try {			
			PreparedStatement st = db.prepareStatement(
					"SELECT s.tick_symbol, t."+ptype +
					" FROM ticks t " +
					"INNER JOIN symbols s " +
					"ON s.id = t.symbol_id " +
					"WHERE t.date = ? " +
					"AND s.tick_symbol = ? ");		
									
			for(String symbol : symbols)
			{
				st.setDate(1, new Date(date.toDate().getTime()));
				st.setString(2, symbol);								
				
				ResultSet rs = st.executeQuery();
				
				while(rs.next())
				{
					ret.put(rs.getString(1), rs.getDouble(2));
				}
				rs.close();
			}
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} 
		
		return ret;
	}

	public double getTickForSymbol(String symbol, LocalDate date, String type)
	{
		String ptype = "adj";
		double ret = -1.0;
		
		if(type != null && 
				(type.equals("close") || type.equals("open") || type.equals("high") || type.equals("low")))
			ptype = type;

		
		try {			
			PreparedStatement st = db.prepareStatement(
					"SELECT s.tick_symbol, t."+ptype +
					" FROM ticks t " +
					"INNER JOIN symbols s " +
					"ON s.id = t.symbol_id " +
					"WHERE t.date = ? " +
					"AND s.tick_symbol = ? ");		
									
			st.setDate(1, new Date(date.toDate().getTime()));
			st.setString(2, symbol);								
			
			ResultSet rs = st.executeQuery();
			
			while(rs.next())
			{
				ret = rs.getDouble(2);
			}
			
			rs.close();
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} 
		
		return ret;
	}
	
	
	
	public PreparedStatement prepareStatement(String sql){
		try {
			PreparedStatement st 
			= db.prepareStatement(sql);
			return st;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
}
