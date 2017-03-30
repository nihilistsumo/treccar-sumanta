package edu.unh.cs.treccar.playground;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.lang3.builder.CompareToBuilder;

import co.nstant.in.cbor.CborException;
import edu.unh.cs.treccar.Data;
import edu.unh.cs.treccar.read_data.DeserializeData;

public class MeasurePerformanceMain {
	//Arguments: ground truth file, outline file, paragraph file
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		MeasurePerformance mp = new MeasurePerformance(args[0], args[1], args[2]);
		try {
			PerformanceMetrics pm = new PerformanceMetrics(mp.getGroundTruth(), mp.getCandidateAssign());
			System.out.println(pm.analyzeBasic());
			System.out.println(pm.calculateRandIndex());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
}
