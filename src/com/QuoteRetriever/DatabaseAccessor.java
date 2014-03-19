package com.QuoteRetriever;

import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.logging.SimpleFormatter;

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
	
	public void importSymbolInfo(ArrayList<String> list, String remark)
	{
		try {
			PreparedStatement st = db.prepareStatement("INSERT INTO symbols(tick_symbol, list_remark) VALUES (?, ?)");
			int i =0;
			for(i=0; i<list.size(); i++){
				String s = list.get(i);
				
				if(getIdForSymbol(s) > 0){
					logger.info("Found symbol "+s+" already in table, skipping");
					continue;
				}
				
				st.setString(1, s);
				st.setString(2, remark);
				st.addBatch();
				
				if(i % 100 == 0){
					st.executeBatch();
				}				
			}
			
			st.executeBatch();
			
			st.close();			
		} catch (SQLException e) {
			logger.warning(e.toString());
			logger.warning("SQLException happened while inserting symbols");
		}						
	}
	
	public void testput(String symbol)
	{
		try {
			PreparedStatement st = db.prepareStatement("INSERT INTO symbols(tick_symbol) values (?)");
			st.setString(1, symbol);
			int rowsInserted = st.executeUpdate();
			
			System.out.println("Inserted "+rowsInserted);
			
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}		
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
