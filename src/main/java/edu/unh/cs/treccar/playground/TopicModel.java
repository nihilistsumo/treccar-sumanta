package edu.unh.cs.treccar.playground;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.TreeSet;
import java.util.regex.Pattern;

import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.FeatureSequence2FeatureVector;
import cc.mallet.pipe.Input2CharSequence;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.PrintInputAndTarget;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceLowercase;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.topics.TopicInferencer;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.IDSorter;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelSequence;
import edu.unh.cs.treccar.Data;
import edu.unh.cs.treccar.playground.AssignParagraphs.SectionPathID;

public class TopicModel {
	private ArrayList<AssignParagraphs.SectionPathID> sectionList;
	private Iterable<Data.Paragraph> paraList;
	private HashMap<AssignParagraphs.SectionPathID, ArrayList<Data.Paragraph>> candidate = new HashMap<AssignParagraphs.SectionPathID, ArrayList<Data.Paragraph>>();
	
	public TopicModel(ArrayList<AssignParagraphs.SectionPathID> sectionList, Iterable<Data.Paragraph> paraList){
		this.sectionList = sectionList;
		this.paraList = paraList;
	}
	public HashMap<AssignParagraphs.SectionPathID, ArrayList<Data.Paragraph>> modelTopics(){
		HashMap<AssignParagraphs.SectionPathID, Integer> secToTopicMap = new HashMap<AssignParagraphs.SectionPathID, Integer>();
		InstanceList iList = new InstanceList(this.buildPipe());
		ArrayList<Instance> rawInstanceList = new ArrayList<Instance>();
		for(Data.Paragraph para : this.paraList){
			Instance instance = new Instance(para.getTextOnly(), "", para.getParaId(), para.getTextOnly());
			rawInstanceList.add(instance);
		}
		iList.addThruPipe(rawInstanceList.iterator());
		int numTopics = 100;
		ParallelTopicModel model = new ParallelTopicModel(numTopics, 1.0, 0.01);
		model.addInstances(iList);
		model.setNumThreads(2);
		try {
			model.estimate();
			
			Alphabet dataAlphabet = iList.getDataAlphabet();
	        
	        FeatureSequence tokens = (FeatureSequence) model.getData().get(0).instance.getData();
	        LabelSequence topics = model.getData().get(0).topicSequence;
	        
	        Formatter out = new Formatter(new StringBuilder(), Locale.US);
	        for (int position = 0; position < tokens.getLength(); position++) {
	            out.format("%s-%d ", dataAlphabet.lookupObject(tokens.getIndexAtPosition(position)), topics.getIndexAtPosition(position));
	        }
	        System.out.println(out);
	        
	        // Estimate the topic distribution of the first instance, 
	        //  given the current Gibbs state.
	        double[] topicDistribution = model.getTopicProbabilities(0);

	        // Get an array of sorted sets of word ID/count pairs
	        ArrayList<TreeSet<IDSorter>> topicSortedWords = model.getSortedWords();
	        
	        // Show top 5 words in topics with proportions for the first document
	        for (int topic = 0; topic < numTopics; topic++) {
	            Iterator<IDSorter> iterator = topicSortedWords.get(topic).iterator();
	            
	            out = new Formatter(new StringBuilder(), Locale.US);
	            out.format("%d\t%.3f\t", topic, topicDistribution[topic]);
	            int rank = 0;
	            while (iterator.hasNext() && rank < 5) {
	                IDSorter idCountPair = iterator.next();
	                out.format("%s (%.0f) ", dataAlphabet.lookupObject(idCountPair.getID()), idCountPair.getWeight());
	                rank++;
	            }
	            System.out.println(out);
	        }
	        //Infer topics of section headings
	        InstanceList sectionInstances = new InstanceList(iList.getPipe());
	        TopicInferencer inferencer = model.getInferencer();
	        for(AssignParagraphs.SectionPathID sec : this.sectionList){
	        	sectionInstances.addThruPipe(new Instance(sec.getSectionPathID(), "", sec.getSectionPathID(), ""));
	        }
	        for(int i=0; i<sectionInstances.size(); i++){
	        	//get topic dist of current instance
	        	double[] topicDist = inferencer.getSampledDistribution(sectionInstances.get(i), 10, 1, 5);
	        	//get index of highest topic
	        	int indexHighest = 0;
	        	double maxProb = 0.0;
	        	for(int j=1; j<topicDist.length; j++){
	        		if(topicDist[j]>maxProb){
	        			indexHighest = j;
	        			maxProb = topicDist[j];
	        		}
	        	}
	        	//get sectionpathid object from sectionList using instance name
	        	//map it with the highest topic index
	        	for(AssignParagraphs.SectionPathID secID : this.sectionList){
	        		if(secID.getSectionPathID().equalsIgnoreCase(sectionInstances.get(i).getName().toString())){
	        			secToTopicMap.put(secID, indexHighest);
	        		}
	        	}
	        }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return this.candidate;
	}
	public Pipe buildPipe(){
		ArrayList pipeList = new ArrayList();

        // Read data from File objects
        pipeList.add(new Input2CharSequence("UTF-8"));

        // Regular expression for what constitutes a token.
        //  This pattern includes Unicode letters, Unicode numbers, 
        //   and the underscore character. Alternatives:
        //    "\\S+"   (anything not whitespace)
        //    "\\w+"    ( A-Z, a-z, 0-9, _ )
        //    "[\\p{L}\\p{N}_]+|[\\p{P}]+"   (a group of only letters and numbers OR
        //                                    a group of only punctuation marks)
        Pattern tokenPattern = Pattern.compile("[\\p{L}\\p{N}_]+");

        // Tokenize raw strings
        pipeList.add(new CharSequence2TokenSequence(tokenPattern));

        // Normalize all tokens to all lowercase
        pipeList.add(new TokenSequenceLowercase());

        // Remove stopwords from a standard English stoplist.
        //  options: [case sensitive] [mark deletions]
        pipeList.add(new TokenSequenceRemoveStopwords(false, false));

        // Rather than storing tokens as strings, convert 
        //  them to integers by looking them up in an alphabet.
        pipeList.add(new TokenSequence2FeatureSequence());

        // Do the same thing for the "target" field: 
        //  convert a class label string to a Label object,
        //  which has an index in a Label alphabet.
        //pipeList.add(new Target2Label());

        // Now convert the sequence of features to a sparse vector,
        //  mapping feature IDs to counts.
        //pipeList.add(new FeatureSequence2FeatureVector());
        
        // Print out the features and the label
        pipeList.add(new PrintInputAndTarget());
        
        return (new SerialPipes(pipeList));
	}
}
