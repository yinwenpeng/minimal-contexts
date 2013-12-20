import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Model;


public class BagOfWords extends Baselines{
	Map<String, Integer> word2id=new HashMap<String, Integer>();
	Set<String> allWordsInMCs;
	
	
	public BagOfWords()
	{
		trainingMCs=new HashMap<String, Double>();
		testMCs=new HashMap<String, Double>();		
		fileNames="replaceToYourFile.txt";
	}

	/**in the input file, each word enjoys a line*/
	public void loadRawFile() throws IOException
	{
		BufferedReader br = new BufferedReader(new FileReader(fileNames));
		String currLine;
		while ((currLine = br.readLine()) != null) 
		{
			word2id.put(currLine, word2id.size()+1); // words are used as features, hence the index starts from 1
		}
		dimention_bagofWords=word2id.size();
		br.close();
	}
	/**Given a raw corpus, extract distinct words into a new file*/
	public void splitRawFile2Words() throws NumberFormatException, IOException
	{
		Set<String> words=new HashSet<String>();
		FileWriter FWords = new FileWriter(fileNames);
		BufferedReader br = new BufferedReader(new FileReader("rawCorpus.txt")); 

		String currLine, currDeli=" ";
		int lineCount=0;
		while ((currLine = br.readLine()) != null) 
		{
			lineCount++;
			System.out.println("reading line "+lineCount+"...");
			if(!currLine.trim().isEmpty())
			{
				for(String word: currLine.split(currDeli))
				{
					String tmp_word=word.replaceAll("\\d", "0").toLowerCase();
					if(!words.contains(tmp_word))
					{
						words.add(tmp_word);
						FWords.write(tmp_word+"\n");						
					}
				}
			}
		}//while
		System.out.println("Word spliting finished");
		br.close();
		FWords.close();
		words.clear();
	}
	
	public void loadWordID() throws IOException
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
		loadRawFile();
		concatenate();
	}
	
	public void concatenate()
	{
		System.out.println("concatenating....\n");
		ArrayList<FeatureNode[]> trainingLists=new ArrayList<FeatureNode[]>();
		ArrayList<FeatureNode[]> testLists=new ArrayList<FeatureNode[]>();

		ArrayList<Double>trainingLabels=new ArrayList<Double>();
		ArrayList<Double>testLabels=new ArrayList<Double>();
		// training data
		for(String MC:trainingMCs.keySet())
		{
			int position=MC.indexOf("_");
			// make both to be lowercase and digits replaces
			String leftWord=MC.substring(0, position).replaceAll("\\d", "0").toLowerCase();
			String rightWord=MC.substring(position+1).replaceAll("\\d", "0").toLowerCase();
			if(word2id.containsKey(leftWord)&&word2id.containsKey(rightWord))
			{
				FeatureNode[] list=new FeatureNode[2];
				// left word
				list[0]=new FeatureNode(word2id.get(leftWord), 1);//feature index starts from 1
				// right word
				list[1]=new FeatureNode(word2id.get(rightWord)+dimention_bagofWords, 1);
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
			if(word2id.containsKey(leftWord)&&word2id.containsKey(rightWord))
			{
				FeatureNode[] list=new FeatureNode[2];
				// left word
				list[0]=new FeatureNode(word2id.get(leftWord), 1);//feature index starts from 1
				// right word
				list[1]=new FeatureNode(word2id.get(rightWord)+dimention_bagofWords, 1);
				// instore this list and gold label
				testLists.add(list);
				testLabels.add(testMCs.get(MC));
				RunTogether.string2bow.put(MC, test_count++);
			}
		}
		transfer2MatureFeature(trainingLists, testLists, trainingLabels, testLabels, dimention_bagofWords*2);
	}
	public double printAccu() throws IOException
	{
		loadWordID();
		Model model=Train(trainingData);
		return predict(testData, model);
	}
}
