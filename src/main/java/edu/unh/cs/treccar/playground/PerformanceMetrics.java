package edu.unh.cs.treccar.playground;

import java.util.ArrayList;
import java.util.HashMap;

import edu.unh.cs.treccar.Data;
import edu.unh.cs.treccar.playground.AssignParagraphs.SectionPathID;

public class PerformanceMetrics {
	private HashMap<String, ArrayList<String>> groundTruth;
	private HashMap<AssignParagraphs.SectionPathID, ArrayList<Data.Paragraph>> candidateAssign;
	public PerformanceMetrics(HashMap<String, ArrayList<String>> groundTruth, 
			HashMap<AssignParagraphs.SectionPathID, ArrayList<Data.Paragraph>> candidateAssign){
		this.groundTruth = groundTruth;
		this.candidateAssign = candidateAssign;
	}
	public String analyzeBasic(){
		int total = 0, hit = 0;
		String sectionID = "", resultString = "";
		String[] secPathTokens;
		Iterable<String> gtSectionPaths = new ArrayList<String>();
		ArrayList<String> correctParaIds = new ArrayList<String>();
		gtSectionPaths = this.groundTruth.keySet();
		for(String sectionPath : gtSectionPaths){
			System.out.println("Current ground truth sectionPath: "+sectionPath);
			correctParaIds = this.groundTruth.get(sectionPath);
			ArrayList<String> candParaIds = new ArrayList<String>();
			System.out.println("Correct paraIDs corresponding to current section: "+correctParaIds);
			
			try {
				ArrayList<Data.Paragraph> candParaList = new ArrayList<Data.Paragraph>();
				candParaList = this.candidateAssign.get(new AssignParagraphs.SectionPathID(sectionPath));
				if(candParaList != null){
					for(Data.Paragraph candPara : candParaList){
						candParaIds.add(candPara.getParaId());
					}
				}
			} catch (NullPointerException e) {
				// TODO Auto-generated catch block
				System.out.println("sectionPath -> section id from ground truth: "+sectionPath);
				e.printStackTrace();
			}
			if(!candParaIds.isEmpty()){
				for(String correct : correctParaIds){
					if(candParaIds.contains(correct)){
						hit++;
						System.out.println("We have a hit here!! Matched paraID: "+correct);
					}
				}
			}
		}
		resultString = "Hit: "+hit;
		return resultString;
	}
	public String calculateRandIndex(){
		String resultString = "";
		HashMap<String, ArrayList<String>> correct = this.groundTruth;
		HashMap<AssignParagraphs.SectionPathID, ArrayList<Data.Paragraph>> candidate = this.candidateAssign;
		String[] correctSections = new String[correct.size()];
		AssignParagraphs.SectionPathID[] candSections = new AssignParagraphs.SectionPathID[candidate.size()];
		correct.keySet().toArray(correctSections);
		candidate.keySet().toArray(candSections);
		
		int[][] contingencyMatrix = new int[correct.size()][candidate.size()];
		double randIndex = 0.0;
		ArrayList<String> correctParas = new ArrayList<String>();
		ArrayList<Data.Paragraph> candParas = new ArrayList<Data.Paragraph>();
		for(int i=0; i<correct.size(); i++){
			for(int j=0; j<candidate.size(); j++){
				int matchCount = 0;
				correctParas = correct.get(correctSections[i]);
				candParas = candidate.get(candSections[j]);
				if(correctParas == null){
					System.out.println("We have null in correctParas!");
				} else if(candParas != null){
					for(Data.Paragraph candPara : candParas){
						if(correctParas.contains(candPara.getParaId())){
							matchCount++;
						}
					}
				}
				contingencyMatrix[i][j] = matchCount;
			}
		}
		if((new Double(this.computeRand(contingencyMatrix))).isNaN())
			resultString = "Adjusted Rand index could not be computed!";
		else
			resultString = "Calculated RAND index: "+this.computeRand(contingencyMatrix);
		return resultString;
	}
	private double computeRand(int[][] contMat){
		double score = 0.0;
		int sumnij=0, sumni=0, sumnj=0, nC2=0, nrow=0, ncol=0;		
		ncol = contMat[0].length;
		nrow = contMat.length/ncol;
		int[] njvals = new int[ncol];
		nC2 = this.nC2(ncol+nrow);
		for(int i=0; i<nrow; i++){
			int ni=0;
			for(int j=0; j<ncol; j++){
				sumnij+=this.nC2(contMat[i][j]);
				ni+=contMat[i][j];
				njvals[j]+=contMat[i][j];
			}
			sumni+=this.nC2(ni);
		}
		for(int j=0; j<njvals.length; j++){
			sumnj+=this.nC2(njvals[j]);
		}
		
		/* ################### 
		 * This code is for simple Rand index
		
		int a=0, b=0, c=0, d=0;
		a = sumnij;
		b = sumni - sumnij;
		c = sumnj - sumnij;
		d = nC2 - (a+b+c);
		score = ((double)(a+d))/(a+b+c+d);
		
		#################### */
		
		double denom = ((double)(sumni+sumnj))/2-((double)sumni*sumnj/nC2);
		double nom = (sumnij-((double)(sumni*sumnj))/nC2);
		System.out.println("sumnij: "+sumnij+", sumni: "+sumni+", sumnj: "+sumnj+", nC2: "+nC2+", nom: "+nom+", denom: "+denom);
		score = nom/denom;
		return score;
	}
	private int nC2(int n){
		if(n<2) return 0;
		else if(n==2) return 1;
		else{
			return n*(n-1)/2;
		}
	}
}
