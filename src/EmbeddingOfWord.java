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


public class EmbeddingOfWord extends Baselines{

	Set<String> allWordsInMCs;
	Map<String, double[]> embedding=new HashMap<String, double[]>();
	String newFileNames="replaceToYourFile.txt";
	
	public EmbeddingOfWord()
	{
		trainingMCs=new HashMap<String, Double>(); // MC -> 1/0
		testMCs=new HashMap<String, Double>();		
		fileNames="replaceToYourFile.txt";
	}
	
	public void extractMCWords()
	{
		allWordsInMCs=new HashSet<String>();
		for(String MC: trainingMCs.keySet())
		{
			int position=MC.indexOf("_");
			String leftWord=MC.substring(0, position).replaceAll("\\d", "0").toLowerCase();
			String rightWord=MC.substring(position+1).replaceAll("\\d", "0").toLowerCase();
			allWordsInMCs.add(leftWord);
			allWordsInMCs.add(rightWord);
		}
		for(String MC: testMCs.keySet())
		{
			int position=MC.indexOf("_");
			String leftWord=MC.substring(0, position).replaceAll("\\d", "0").toLowerCase();
			String rightWord=MC.substring(position+1).replaceAll("\\d", "0").toLowerCase();
			allWordsInMCs.add(leftWord);
			allWordsInMCs.add(rightWord);
		}
	}
	
	public void loadReleasedEmbedding() throws NumberFormatException, IOException
	{
		extractMCWords();
		BufferedReader br = new BufferedReader(new FileReader(newFileNames));
		String currLine, currDeli=" ";
		boolean finished=false;
		
		while ((currLine = br.readLine()) != null) 
		{
				if(finished) break;
				String[] entries=currLine.split(currDeli);
				String token=entries[0];
				if(!allWordsInMCs.contains(token)) continue;
				else
				{
					double[] lineValues=new double[dimension_MC];
					for(int col=1;col<201;col++)
					{
						lineValues[col-1]=Double.valueOf(entries[col]);
					}
					embedding.put(token, lineValues);
					if(embedding.size()==allWordsInMCs.size()) finished=true;  
				}									
		}//while
		br.close();
	}
	
	public void loadEmbeddingFile() throws NumberFormatException, IOException
	{
		extractMCWords();
		BufferedReader br = new BufferedReader(new FileReader(fileNames));

		String currLine, currDeli=" ";
		int lineCount=0;
		boolean finished=false;
		
		while ((currLine = br.readLine()) != null) 
		{
			lineCount++;
			System.out.println("reading line "+lineCount+"...");
			if(lineCount<2) continue;
			else
			{
				if(finished) break;
				String[] entries=currLine.split(currDeli);
				//if current word is not contained by those MCs
				String token=entries[0].replaceAll("\\d", "0").toLowerCase();
				if(!allWordsInMCs.contains(token)) continue;
				else
				{
					double[] lineValues=new double[dimension_MC];
					for(int col=1;col<201;col++)
					{
						lineValues[col-1]=Double.valueOf(entries[col]);
					}
					embedding.put(token, lineValues);
					if(embedding.size()==allWordsInMCs.size()) finished=true;  
				}									
			}
		}//while
		br.close();
	}
	
	public void concatenate()
	{
		System.out.println("Word embedding concatenating....\n");
		ArrayList<FeatureNode[]> trainingLists=new ArrayList<FeatureNode[]>();
		ArrayList<FeatureNode[]> testLists=new ArrayList<FeatureNode[]>();

		ArrayList<Double>trainingLabels=new ArrayList<Double>();
		ArrayList<Double>testLabels=new ArrayList<Double>();
		
		for(String MC:trainingMCs.keySet())
		{
			int position=MC.indexOf("_");
			String leftWord=MC.substring(0, position).replaceAll("\\d", "0").toLowerCase();
			String rightWord=MC.substring(position+1).replaceAll("\\d", "0").toLowerCase();
			if(embedding.containsKey(leftWord)&&embedding.containsKey(rightWord))
			{
				FeatureNode[] list=new FeatureNode[dimension_Concate];
				// left word
				for(int index_left=0;index_left<embedding.get(leftWord).length;index_left++)
				{
					FeatureNode featureNode=new FeatureNode(index_left+1, embedding.get(leftWord)[index_left]);
					list[index_left]=featureNode;
				}
				// right word
				for(int index_right=0;index_right<embedding.get(rightWord).length;index_right++)
				{
					FeatureNode featureNode=new FeatureNode(index_right+dimension_MC+1, embedding.get(rightWord)[index_right]);
					list[index_right+dimension_MC]=featureNode;
				}
				// instore this list and gold label
				trainingLists.add(list);
				trainingLabels.add(trainingMCs.get(MC));
			}
		}
		// test data
		int test_count=0;
		for(String MC:testMCs.keySet())
		{
			int position=MC.indexOf("_");
			String leftWord=MC.substring(0, position).replaceAll("\\d", "0").toLowerCase();
			String rightWord=MC.substring(position+1).replaceAll("\\d", "0").toLowerCase();
			if(embedding.containsKey(leftWord)&&embedding.containsKey(rightWord))
			{
				FeatureNode[] list=new FeatureNode[dimension_Concate];
				// left word
				for(int index_left=0;index_left<embedding.get(leftWord).length;index_left++)
				{
					FeatureNode featureNode=new FeatureNode(index_left+1, embedding.get(leftWord)[index_left]);
					list[index_left]=featureNode;
				}
				// right word
				for(int index_right=0;index_right<embedding.get(rightWord).length;index_right++)
				{
					FeatureNode featureNode=new FeatureNode(index_right+dimension_MC+1, embedding.get(rightWord)[index_right]);
					list[index_right+dimension_MC]=featureNode;
				}
				// instore this list and gold label
				testLists.add(list);
				testLabels.add(testMCs.get(MC));
				RunTogether.string2word.put(MC, test_count++);
			}
		}
		transfer2MatureFeature(trainingLists, testLists, trainingLabels, testLabels, dimension_Concate);
	}

	public void loadWordEmbeddings() throws IOException
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
		
		trainingMCs.clear();
		testMCs.clear();	
		newChain.splitTrainingTesting(ratio, trainingMCs, testMCs, false);
		//loadEmbeddingFile();
		loadReleasedEmbedding();  
		concatenate();
	}
	
	public double printAccu() throws IOException
	{
		loadWordEmbeddings();
		Model model=Train(trainingData);
		return predict(testData, model);
	}
}
