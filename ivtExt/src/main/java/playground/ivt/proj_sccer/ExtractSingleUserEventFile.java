package playground.ivt.proj_sccer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ExtractSingleUserEventFile {
	
	public static void main(String[] args) throws IOException {
		final String INPUT_FILE = "P:\\Projekte\\SCCER\\zurich_1pc\\scenario\\test.events.xml\\test.events.xml";
		final String OUTPUT_FILE = "P:\\Projekte\\SCCER\\zurich_1pc\\scenario\\test.events.xml\\test2.events.xml";
		final String TO_FIND = "\"894296700\"";
		
		FileReader fr = new FileReader(INPUT_FILE);
		BufferedReader br = new BufferedReader(fr);
		FileWriter fw = new FileWriter(OUTPUT_FILE);
		BufferedWriter bw = new BufferedWriter(fw);
		
		String line;
		while ( (line = br.readLine()) != null) {
			if(line.contains("</events>") 
					|| line.contains("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
					|| line.contains("<events version=\"1.0\">")
					|| line.contains(TO_FIND)
					) 
			{
				bw.write(line);
				bw.newLine();
			}
		}
		bw.close();
		br.close();
		System.out.println("Processing complete!");
	}
}
