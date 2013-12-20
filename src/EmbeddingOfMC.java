import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Model;


public class EmbeddingOfMC extends Baselines{
	Map<String, FeatureNode[]> MCembedding=new HashMap<String, FeatureNode[]>();
	Set<String> allMCs;
	
	public EmbeddingOfMC()
	{
		trainingMCs=new HashMap<String, Double>(); // MC -> 1/0
		testMCs=new HashMap<String, Double>();		
		fileNames=RunTogether.newMCEmbeddingFile;
	}
	
	public void uniformAllMC()
	{
		allMCs=new HashSet<String>();
		allMCs.addAll(trainingMCs.keySet());
		allMCs.addAll(testMCs.keySet());
	}
	
	public void loadEmbeddingFile() throws IOException
	{
		uniformAllMC();
		BufferedReader br = new BufferedReader(new FileReader(fileNames));
		int line=0;
		String currLine, deli=" ";
		boolean finished=false;
		while ((currLine = br.readLine()) != null) {
			line++;
			if(line<=1) continue;
			else if (currLine.indexOf("_")>=0)
			{
				
				if(finished) break; 
				System.out.println("reading line "+line+"...");
				String[] tokens=currLine.split(deli);
				if(!allMCs.contains(tokens[0])) continue;
				else{
					FeatureNode[] lineValues=new FeatureNode[dimension_MC];
					for(int col=1;col<201;col++)
					{
						lineValues[col-1]=new FeatureNode(col, Double.valueOf(tokens[col]));	
					}
					MCembedding.put(tokens[0], lineValues);
					if(MCembedding.size()==allMCs.size()) finished=true;
				}
			}
		}
		System.out.println("MCembedding size is "+MCembedding.size());
		br.close();
	}
	
	public void concatenate() throws IOException
	{
		System.out.println("MC embedding loading....\n");
		ArrayList<FeatureNode[]> trainingLists=new ArrayList<FeatureNode[]>();
		ArrayList<FeatureNode[]> testLists=new ArrayList<FeatureNode[]>();
		ArrayList<Double>trainingLabels=new ArrayList<Double>();
		ArrayList<Double>testLabels=new ArrayList<Double>();
		int test_count=0;
		for(String mc: trainingMCs.keySet())
		{
			trainingLists.add(MCembedding.get(mc));
			trainingLabels.add(trainingMCs.get(mc));
		}
		for(String mc: testMCs.keySet())
		{
			testLists.add(MCembedding.get(mc));
			testLabels.add(testMCs.get(mc));
			RunTogether.string2mc.put(mc, test_count++);
		}
		transfer2MatureFeature(trainingLists, testLists, trainingLabels, testLabels, dimension_MC);
	}
		
	public void loadMCembeddings() throws IOException
	{
		Chain newChain=new Chain();
		String[] fileNames=new String[2];
		fileNames[0]="coreferenceFile_1.txt";
		fileNames[1]="coreferenceFile_2.txt";
		newChain.extractChainFromCorpus(fileNames);
		newChain.refineChains();
		newChain.splitPosiNega();
		newChain.removeRareMCs();
		newChain.removeRareMCsAgain();

		
		//for loop, first should clear the trainingMCs and testMCs
		trainingMCs.clear();
		testMCs.clear();	
		newChain.splitTrainingTesting(ratio, trainingMCs, testMCs, false);	
		loadEmbeddingFile();	
		concatenate();
	}
	
	public double printAccu() throws IOException
	{
		loadMCembeddings();
		Model model=Train(trainingData);
		return predict(testData, model);
	}
}
