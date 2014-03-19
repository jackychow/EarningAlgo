package com.AlgoSimulation;

public class DoubleUtil {
	
	private final static double posv_epsilon = 0.001;
	private final static double negv_epsilon = -0.001;
	
	
	public static boolean EqualsZero(double num)
	{
		return (num > negv_epsilon && num < posv_epsilon );
	}
	
	public static boolean IsPositive(double num)
	{
		return (num > posv_epsilon);
	}
	
	public static boolean IsNegative(double num)
	{
		return (num < negv_epsilon);
	}
	
	public static boolean Equals(double num1, double num2)
	{
		return (Math.abs(num1-num2) < posv_epsilon);
	}

}
