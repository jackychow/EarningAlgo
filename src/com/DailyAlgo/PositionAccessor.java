package com.DailyAlgo;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.joda.time.LocalDate;

import com.AlgoSimulation.DatabaseAccessor;
import com.AlgoSimulation.Position;

public class PositionAccessor {

	private final static DatabaseAccessor db = DatabaseAccessor.getInstance();
	private final static DailyTaskLogger logger = DailyTaskLogger.getInstance();
	
	public static List<Position> LoadAllActivePositions()
	{
		List<Position> positions = new LinkedList<Position>();
		
		PreparedStatement st = db.prepareStatement(
				"select p.id, s.tick_symbol, p.open_date, p.buy_price, p.num_shares " +
				"from positions p " +
				"inner join symbols s on s.id = p.symbol_id " +
				"where active = 1 ");		
		try{
			
			ResultSet rs = st.executeQuery();
			
			while(rs.next())
			{
				Position pos = new Position(rs.getString(2));
				pos.id = rs.getInt(1);				
				double price = rs.getDouble(4);
				LocalDate open_date = LocalDate.fromDateFields(rs.getDate(3));
				int num_shares = rs.getInt(5);
				pos.setBuyPosition(price, num_shares, open_date);
				positions.add(pos);
			}
			
			rs.close();
			st.close();			
		}catch(Exception e)
		{
			logger.warning("[ERROR] Failed to retrieve active positions "+e.getMessage());
		} finally{
			try {
				st.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		return positions;
	}
	
	public static boolean OpenNewPosition(String symbol, int numShares, double price, LocalDate date)
	{
		int id = db.getIdForSymbol(symbol);
		PreparedStatement st = db.prepareStatement(
				"insert into positions (symbol_id, open_date, buy_price, num_shares, active) " +
				"values (?, ?, ?, ?, 1)") ;
		
		try{			
			st.setInt(1, id);
			st.setDate(2, new java.sql.Date(date.toDateTimeAtStartOfDay().toDate().getTime()));
			st.setDouble(3, price);
			st.setInt(4, numShares);
			
			int updated = st.executeUpdate();
			
			if(updated == 0){
				logger.warning("[ERROR] Failed to open new position for symbol "+symbol);
				st.close();
				return false;
			}
			
		} catch (SQLException e) {
			logger.warning("[ERROR] Failed to open new position for symbol "+symbol+e.getMessage());
			return false;
		}finally{
			try {
				st.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
				
		return true;
	}
	
	public static boolean ClosePosition(String symbol, double price, LocalDate date)
	{
		int id = db.getIdForSymbol(symbol);
		PreparedStatement st = db.prepareStatement(
				"update positions set close_date = ?, sell_price = ?, active=-1 " +
				"where symbol_id = ? and active = 1");
		
		try{
			st.setDate(1, new java.sql.Date(date.toDateTimeAtStartOfDay().toDate().getTime()));
			st.setDouble(2, price);
			st.setInt(3, id);
			
			int updated = st.executeUpdate();
			
			if(updated == 0){
				logger.warning("[ERROR] Failed to close position with symbol "+symbol);
				st.close();
				return false;				
			}			
			
		} catch (SQLException e) {
			e.printStackTrace();
		} finally{
			try {
				st.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}			
		}				
						
		return true;
	}	
	
}
