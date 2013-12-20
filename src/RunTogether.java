import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;


public class RunTogether{

	Model MCEmbedding;
	Model wordEmbedding;
	Model BOW;
	public Map<String, Double> trainingMCs;
	Map<String, Double> testMCs;	
	Data trainingData_MC, testData_MC, trainingData_Word, testData_Word, trainingData_BOW, testData_BOW;
	static Map<String, Integer>string2mc=new HashMap<String, Integer>();
	static Map<String, Integer>string2word=new HashMap<String, Integer>();
	static Map<String, Integer>string2bow=new HashMap<String, Integer>();	
	static Map<String, String>mc2markable=new HashMap<String, String>();
	
	//some common loading files
	static String newMCEmbeddingFile="C&W_embedding.txt";
	static String oldMCEmbeddingFile="skip-gram-embedding.txt";
	
	public RunTogether()
	{
		trainingMCs=new HashMap<String, Double>(); 
		testMCs=new HashMap<String, Double>();	
		MCEmbedding=new Model();
		wordEmbedding=new Model();
		BOW=new Model();
	}
	
	public void determineTrainingTest(Chain newChain, boolean noMCMethod)
	{
		//for loop, first should clear the trainingMCs and testMCs
		trainingMCs.clear();
		testMCs.clear();	
		newChain.splitTrainingTesting(Baselines.ratio, trainingMCs, testMCs, noMCMethod);
	}
	
	public void extractMC(Chain newChain) throws IOException
	{
		String[] fileNames=new String[2];
		fileNames[0]="coreferenceFile_1.txt";
		fileNames[1]="coreferenceFile_2.txt";
		newChain.extractChainFromCorpus(fileNames);
		newChain.refineChains();
		newChain.splitPosiNega();
		newChain.removeRareMCs();
		newChain.removeRareMCsAgain();
	}
	public void buildDataForLiblinear() throws IOException
	{
		Chain newChain=new Chain();
		extractMC(newChain);
		// use embeddingOfMC to train a model
		determineTrainingTest(newChain, false);			
		EmbeddingOfMC mc=new EmbeddingOfMC();
		mc.trainingMCs=this.trainingMCs;
		mc.testMCs=this.testMCs;
		mc.balancingTrainingData();
		mc.loadEmbeddingFile();
		mc.concatenate();
		//this.trainingData_MC=mc.trainingData;
		this.testData_MC=mc.testData;
		MCEmbedding=mc.Train(mc.trainingData);
			
		// use embeddingOfWord to train a model
		//determineTrainingTest(newChain, true);   // make the following two methods share the same training data with MC method
		EmbeddingOfWord word=new EmbeddingOfWord();
		word.trainingMCs=this.trainingMCs;
		word.testMCs=this.testMCs;
		word.balancingTrainingData();
		word.loadEmbeddingFile();
		//word.loadReleasedEmbedding(); // especially for C&W embedding
		word.concatenate();
		//this.trainingData_Word=word.trainingData;
		this.testData_Word=word.testData;
		wordEmbedding=word.Train(word.trainingData);
		
		//use BOW to train a model
		//determineTrainingTest(newChain, true);   // BOW share the same training data
		BagOfWords bow=new BagOfWords();
		bow.trainingMCs=this.trainingMCs;
		bow.testMCs=this.testMCs;
		bow.balancingTrainingData();
		bow.loadRawFile();
		bow.concatenate();
		//this.trainingData_BOW=bow.trainingData;
		this.testData_BOW=bow.testData;
		BOW=bow.Train(bow.trainingData);	
	}
	
	public void predict() throws IOException
	{
		int allValidMC=0;
		int correct_MC=0;
		int correct_BOW=0;
		int correct_Word=0;
		int hasNoMcEmbedding=0, hasNoWordEmbedding=0, hasNoWordIndex=0;
		for(String mc:testMCs.keySet())
		{
			if(string2mc.containsKey(mc)&&string2word.containsKey(mc)&&string2bow.containsKey(mc))
			{
				allValidMC++;
				double mcResult=Linear.predict(MCEmbedding, this.testData_MC.values[string2mc.get(mc)]);
				double wordResult=Linear.predict(wordEmbedding, this.testData_Word.values[string2word.get(mc)]);
				double bowResult=Linear.predict(BOW, this.testData_BOW.values[string2bow.get(mc)]);
				//
				if(Math.abs(testMCs.get(mc)-mcResult)<1.0) correct_MC++;
				if(Math.abs(testMCs.get(mc)-bowResult)<1.0) correct_BOW++;
				if(Math.abs(testMCs.get(mc)-wordResult)<1.0)  correct_Word++;
			}		
			if(!string2mc.containsKey(mc)) hasNoMcEmbedding++;
			if(!string2word.containsKey(mc)) hasNoWordEmbedding++;
			if(!string2bow.containsKey(mc)) hasNoWordIndex++;
		}
		System.out.println("loss: "+hasNoMcEmbedding+" "+hasNoWordEmbedding+" "+hasNoWordIndex);
		System.out.println("All test data has: "+allValidMC);
		// compute the accuracy
		double accu_MC=correct_MC*1.0/allValidMC;
		double accu_BOW=correct_BOW*1.0/allValidMC;
		double accu_Word=correct_Word*1.0/allValidMC;
		System.out.println("Finished, accuracies are :"+accu_MC+" "+accu_BOW+" "+accu_Word);
	}
	
	public  static void main(String[] args) throws IOException
	{
		RunTogether instance=new RunTogether();
		instance.buildDataForLiblinear();
		instance.predict();
	}
}
