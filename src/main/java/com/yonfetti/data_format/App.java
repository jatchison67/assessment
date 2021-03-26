package com.yonfetti.data_format;

import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.text.ParseException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

/**
 * Hello world!
 *
 */
public class App {
	private final static String dateFormat = "dd-MMM-yyyy";
	private final static String rawDate = "dd-MMM-yy hh.mm.ss.SSS a";
	private final static String minString = "01-Jan-1980";
	private static Map<String, String> columns;
	private static Date minDate;
	private static Date maxDate;

	public static void main(String[] args) {
		columns = parseMappings(); // get the mappings from the json file
		try {
			minDate = new SimpleDateFormat(dateFormat).parse(minString); // set up the minimum date per requirements
		}
		catch(ParseException pe) {
			pe.printStackTrace();
			System.exit(0);
		}
		maxDate = new Date();// get today's date as max date per requirements
		parseCsv();
	}

	private static Map<String, String> parseMappings() {
		try {
			Object obj = new JSONParser().parse(new FileReader("mappings.jsonc"));

			JSONObject jo = (JSONObject) obj;
			JSONArray columnsList = ((JSONArray)jo.get("columns"));// creating map of RawName and CommonName pairs RawName as key
			Map<String, String> columns = new HashMap<>();
			for(Object columnObj: columnsList) {
				String columnStr = columnObj.toString();
				String columnSubStr = columnStr.substring(columnStr.indexOf("\"")+1, columnStr.lastIndexOf("\""));
				String key = columnSubStr.substring(0, columnSubStr.indexOf("\""));
				String value = columnSubStr.substring(columnSubStr.indexOf(":")+2);
				columns.put(key, value);
			}
			//Map<String, String> columns = columnsList.stream().map(Object::toString).collect(Collectors.toMap(s -> s, s -> "value"));
			return columns;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	private static Date parseDate(String dateString) {
		try {
			return new SimpleDateFormat(rawDate).parse(dateString);
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}

	}

	private static String validateTruncateDate(String inString) {
		Date inDate = parseDate(inString);
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
		if (inDate != null) {// any errors parsing will give us null and we'll handle those
			if (inDate.before(minDate) && inDate.after(maxDate)) {
				System.out.println("Date: " + inDate + " is outside the date parameters.");
				return "";
			}
			return simpleDateFormat.format(inDate);
		}
		System.out.println("Date: " + inDate + " does not parse as a valid date.");
		return "";
	}

	private static void parseCsv() {
		List<List<String>> records = new ArrayList<>();
		try (CSVReader csvReader = new CSVReader(new FileReader("sample.csv"));) {
			String[] values = null;
			while ((values = csvReader.readNext()) != null) {
				records.add(Arrays.asList(values));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		if (records.size() > 0) {
			List<String[]> recordsOut = new ArrayList<>();
			// parse the headers
			List<String> headersRaw = records.get(0);
			String[] headersCommon = new String[headersRaw.size()];
			int i = 0;
			for(String headerRaw : headersRaw) {
				if(columns.containsKey(headerRaw.toString()))
					headersCommon[i] = columns.get(headerRaw).toString();
				else {
					System.out.println("Column raw name: " + headerRaw + " does not match a mapping.");
					headersCommon[i] = "";
				}
				i++;
			}
			recordsOut.add(headersCommon);
			for(List<String> record : records.subList(1, records.size())) {
				String[] recordOut = new String[record.size()];
				i=0;
				for(String item : record) {
					if(isDate(item)) {
						recordOut[i] = validateTruncateDate(item);
					}
					else
						recordOut[i] = item.toLowerCase();
					i++;
				}
				recordsOut.add(recordOut);
			}
			//write our formatted records to a new csv
			try {
			    CSVWriter writer = new CSVWriter(new FileWriter("output.csv"));
			    for (String[] recordOut : recordsOut) {
			        writer.writeNext(recordOut);
			    }
			    
			    writer.close();
			}
			catch(Exception fex) {
				System.out.println("Error writing file: " +fex.getMessage());
			}
		}
		else {
			System.out.println("*****");
			System.out.println("No records were written");
		}
	}
	
    public static boolean isDate(String dateStr) {
        try {
        	new SimpleDateFormat(rawDate).parse(dateStr);
        } catch (ParseException e) {
            return false;
        }
        return true;
    }
}
