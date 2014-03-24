package com.DailyAlgo;

import java.text.DecimalFormat;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.joda.time.LocalDate;

import com.AlgoSimulation.DoubleUtil;

public class ExecutionUtil {

	private Options options = new Options();
	private DecimalFormat decimalFormatter = new DecimalFormat("#.##");
	
	public static void main(String[] args)
	{
		ExecutionUtil util = new ExecutionUtil();
		util.parseCommand(args);
	}

	public ExecutionUtil()
	{
		prepareOptions();
	}
	
	private void parseCommand(String[] args)
	{
		CommandLineParser parser = new GnuParser();
		try {
			CommandLine cmd = parser.parse(options, args);
			
			if( cmd.hasOption("position")){
				String action = cmd.getOptionValue("position");
				if(cmd.hasOption("s") && cmd.hasOption("p")){
					String symbol = cmd.getOptionValue("s");
					String price = cmd.getOptionValue("p");
					LocalDate date = LocalDate.now();
					boolean adjust = cmd.hasOption("adjust");
					if(cmd.hasOption("d"))
					{
						date = LocalDate.parse(cmd.getOptionValue("d"));
					}
					if(action.equals("open")) {
						if(cmd.hasOption("n")){
							String numShares = cmd.getOptionValue("n");
							if(addPositions(symbol, Integer.parseInt(numShares), Double.parseDouble(price), date, adjust)){
								System.out.println("Add Position Successfully");
							}
							return;
						}						
					} else if(action.equals("close")) {
						if(closePosition(symbol, Double.parseDouble(price), date, adjust))
						{
							System.out.println("Successfully Closed Position");
						}
						return;
					}
				}
			} else if( cmd.hasOption("property")){
				String action = cmd.getOptionValue("property");
				
				if(action.equals("list")){
					String prop = null;
					if(cmd.hasOption("name")){
						prop = cmd.getOptionValue("name");
					}
					listPropertyValues(prop);
					return;
				}else if (action.equals("set")){
					if(cmd.hasOption("name") && cmd.hasOption("value")){
						String prop = cmd.getOptionValue("name");
						String value = cmd.getOptionValue("value");
						if(AppPropertyValues.SetAppPropertyValues(prop, value)){							
							System.out.println("Successfully set property values for "+prop+" : "+value);
						}
						return;
					}
				}
			}
			
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("ExecutionUtil", options);
			
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
	
	private boolean addPositions(String symbol, int numShares, double price, LocalDate date, boolean adjust)
	{
		double diff = PositionAccessor.OpenNewPosition(symbol, numShares, price, date);
		if(!DoubleUtil.EqualsZero(diff))
		{
			if(adjust)
			{
				String cash = AppPropertyValues.GetValueForProperty("cash");
				double cashDbl = Double.parseDouble(cash);
				cashDbl -= diff;
				AppPropertyValues.SetAppPropertyValues("cash", decimalFormatter.format(cashDbl));
			}
			
			return true;
		}else{
			return false;
		}
	}
	
	private boolean closePosition(String symbol, double price, LocalDate date, boolean adjust)
	{
		double diff = PositionAccessor.ClosePosition(symbol, price, date);
		if(!DoubleUtil.EqualsZero(diff))
		{
			if(adjust)
			{
				String cash = AppPropertyValues.GetValueForProperty("cash");
				double cashDbl = Double.parseDouble(cash);
				cashDbl += diff;
				AppPropertyValues.SetAppPropertyValues("cash", decimalFormatter.format(cashDbl));
			}
			
			return true;
		}else{
			return false;
		}		
	}
	
	private void listPropertyValues(String prop)
	{
		StringBuilder sb = new StringBuilder();
		if(prop == null){
			List<AppPropertyValues> apvs = AppPropertyValues.GetAllAppPropertyValues();
			for(AppPropertyValues apv : apvs)
			{
				sb.append(apv.getPropertyName());
				sb.append("\t");
				sb.append(apv.getPropertyValue());
				sb.append("\n");
			}
		}else{
			String val = AppPropertyValues.GetValueForProperty(prop);
			sb.append(prop);
			sb.append("\t");
			sb.append(val);
			sb.append("\n");			
		}
		
		System.out.println(sb.toString());
	}
		
	@SuppressWarnings("static-access")
	private void prepareOptions()
	{

		Option position = OptionBuilder.withArgName("open|close")
		.hasArg().withDescription("Perform operation on position").create("position");
		Option symbol = OptionBuilder.withArgName("symbol")
		.hasArg().withDescription("Symbol for Stock").create("s");
		Option numShare = OptionBuilder.withArgName("ShareNumbers")
		.hasArg().withDescription("Number of Shares to Buy").create("n");
		Option price = OptionBuilder.withArgName("Price")
		.hasArg().withDescription("Price for the action").create("p");
		Option date = OptionBuilder.withArgName("Date For Action (yyyy-MM-dd)")
		.hasArg().withDescription("Optinoal: Date for the action, Default to today").create("d");
		Option propval = OptionBuilder.withArgName("list|set")
		.hasArg().withDescription("App Property Value actions").create("property");
		Option property = OptionBuilder.withArgName("Property Name")
		.hasArg().withDescription("The Name of Property").create("name");
		Option value = OptionBuilder.withArgName("Property Value")
		.hasArg().withDescription("The Value for the Property").create("value");
		Option adjust = new Option("adjust", "adjust cash also when adjusting positions");
		
		
		options.addOption(position);
		options.addOption(symbol);
		options.addOption(numShare);
		options.addOption(price);
		options.addOption(date);
		options.addOption(propval);
		options.addOption(property);
		options.addOption(value);
		options.addOption(adjust);
		
	}
	
	private void adjustCash(double amount)
	{
		
	}
	
}
