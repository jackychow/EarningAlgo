package com.DailyAlgo;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import com.AlgoSimulation.AlgoLogger;
import com.AlgoSimulation.DatabaseAccessor;

public class AppPropertyValues {

	private static final DatabaseAccessor db = DatabaseAccessor.getInstance();
	private static final AlgoLogger logger = AlgoLogger.getInstance();
	
	//stateful variables
	private String propertyName;
	private String propertyValue;
	
	public AppPropertyValues(String name, String value)
	{
		this.setPropertyName(name);
		this.setPropertyValue(value);
	}
	
	
	
	public static String GetValueForProperty(String prop)
	{
		PreparedStatement st = db.prepareStatement(
				"SELECT value " +
				"FROM app_property_values " +
				"WHERE property = ?");
		
		String ret = null;
		try{			
			st.setString(1, prop);
			ResultSet rs = st.executeQuery();

			while(rs.next())
			{
				ret = rs.getString(1);
			}
			
			rs.close();
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}		
		
		return ret;
	}
	
	public static List<AppPropertyValues> GetAllAppPropertyValues()
	{
		PreparedStatement st = db.prepareStatement("select property, value from app_property_values");
		List<AppPropertyValues> ret = new LinkedList<AppPropertyValues>();
		
		try{
			ResultSet rs = st.executeQuery();
			
			while(rs.next())
			{
				AppPropertyValues apv 
				= new AppPropertyValues(rs.getString(1), rs.getString(2));
				ret.add(apv);
			}
			
			rs.close();
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return ret;
	}
	
	public static boolean SetAppPropertyValues(String prop, String value)
	{
		PreparedStatement st = db.prepareStatement("SELECT 1 FROM app_property_values WHERE property = ?");
		
		try{
			int exists = -1;
			st.setString(1, prop);
			ResultSet rs = st.executeQuery();
			while(rs.next())
			{
				exists = rs.getInt(1);
			}
			
			rs.close();
			st.close();
			if(exists > 0)
			{
				//update
				st = db.prepareStatement(
						"UPDATE app_property_values " +
						"SET value = ? " +
						"WHERE property = ? ");
				st.setString(1, value);
				st.setString(2, prop);
				
			}else{
				//insert
				st = db.prepareStatement(
						"INSERT INTO app_property_values " +
						"(property, value) " +
						"VALUES (?, ?)");
				st.setString(1, prop);
				st.setString(2, value);
			}
			
			st.executeUpdate();
		
			st.close();			
		} catch (Exception e) {
			logger.warning("[ERROR] Failed to set property-value pair ("+prop+", "+value+") "+e.getMessage());
			return false;
		}
		
		return true;
	}



	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}



	public String getPropertyName() {
		return propertyName;
	}



	public void setPropertyValue(String propertyValue) {
		this.propertyValue = propertyValue;
	}



	public String getPropertyValue() {
		return propertyValue;
	}

}
