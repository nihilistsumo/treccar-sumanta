package edu.unh.cs.treccar.playground;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import cc.mallet.cluster.Clustering;
import cc.mallet.cluster.KMeans;
import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.FeatureSequence2FeatureVector;
import cc.mallet.pipe.Input2CharSequence;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SaveDataInSource;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.Target2Label;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceLowercase;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.NormalizedDotProductMetric;
import co.nstant.in.cbor.CborException;
import edu.unh.cs.treccar.Data;
import edu.unh.cs.treccar.read_data.DeserializeData;

public class BaseClusterer {
	private String paraPath;
	private ArrayList<Data.Paragraph> paraList;
	public BaseClusterer(String paraPath) throws FileNotFoundException, CborException{
		this.paraPath = paraPath;
		paraList = new ArrayList<Data.Paragraph>();
		FileInputStream paraStream = new FileInputStream(new File(paraPath));
		for(Data.Paragraph para : DeserializeData.iterableParagraphs(paraStream)){
			paraList.add(para);
		}
	}
	public HashMap<String, ArrayList<String>> getClusters(int numClusters){
		//pipe of instanceList will not be used
		InstanceList instanceList = new InstanceList(BaseClusterer.buildPipe());
		for(Data.Paragraph p : this.paraList){
			Instance i = new Instance(p.getTextOnly(), null, p.getParaId(), null);
			instanceList.addThruPipe(i);
		}
		KMeans kmeans = new KMeans(instanceList.getPipe(), numClusters, new NormalizedDotProductMetric(), KMeans.EMPTY_DROP);
		Clustering clusterData = kmeans.cluster(instanceList);
		return formatClusterData(clusterData);
	}
	private HashMap<String, ArrayList<String>> formatClusterData(Clustering rawClusterData){
		HashMap<String, ArrayList<String>> finalClusterData = new HashMap<String, ArrayList<String>>();
		int labelIndex = 0;
		for(InstanceList iList : rawClusterData.getClusters()){
			String clusterLabel = "Cluster"+labelIndex;
			ArrayList<String> paraIDList = new ArrayList<String>();
			for(Instance i : iList){
				paraIDList.add(i.getName().toString());
			}
			finalClusterData.put(clusterLabel, paraIDList);
			labelIndex++;
		}
		return finalClusterData;
	}
	public static Pipe buildPipe(){
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
        pipeList.add(new FeatureSequence2FeatureVector());
        
        // Print out the features and the label
        //pipeList.add(new PrintInputAndTarget());
        
        return (new SerialPipes(pipeList));
	}
}
