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
	static String newMCEmbeddingFile="/mounts/data/proj/wenpeng/MC/src/new_vectors_MCs.txt";
	static String oldMCEmbeddingFile="/mounts/data/proj/wenpeng/MC/src/vectors_MCs.txt";
	
	public RunTogether()
	{
		trainingMCs=new HashMap<String, Double>(); // MC -> 1/0
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
		fileNames[0]="//mounts/Users/student/wenpeng/DesiData/en_train_auto_v4.txt";
		fileNames[1]="//mounts/Users/student/wenpeng/DesiData/en_test.txt";
		//fileNames[2]="//mounts/Users/student/wenpeng/sucre/mount/corpora12/sukre/02_Code/NLP/corpus/conll/origin/trainall.conll";
		newChain.extractChainFromCorpus(fileNames);
		newChain.refineChains();
		newChain.splitPosiNega();
		newChain.removeRareMCs();
		newChain.removeRareMCsAgain();
		//newChain.posiEqualNega();	
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
			//word.loadReleasedEmbedding();
			word.concatenate();
			//this.trainingData_Word=word.trainingData;
			this.testData_Word=word.testData;
			wordEmbedding=word.Train(word.trainingData);
			//use BOW to train a model
			//determineTrainingTest(newChain, true);   // BOW share the same with Word method
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
	
	public String predict() throws IOException
	{
		int allValidMC=0;
		int mc_word=0;
		int bow_word=0;
		int mc_bow=0;
		// correct count
		int correct_MC=0;
		int correct_BOW=0;
		int correct_Word=0;
		//System.out.println(testMCs.size());
		int hasNoMcEmbedding=0, hasNoWordEmbedding=0, hasNoWordIndex=0;
		//FileWriter Fwrite = new FileWriter("statistic.txt");
		//Fwrite.write("MC\tGoldTag\tMC_preTag\tBOW_preTag\tConcatenate_preTag\tmarkables\n");
		for(String mc:testMCs.keySet())
		{
			if(string2mc.containsKey(mc)&&string2word.containsKey(mc)&&string2bow.containsKey(mc))
			{
				allValidMC++;
				double mcResult=Linear.predict(MCEmbedding, this.testData_MC.values[string2mc.get(mc)]);
				//Fwrite.write(mc+" "+mcResult+"\n");
				double wordResult=Linear.predict(wordEmbedding, this.testData_Word.values[string2word.get(mc)]);
				double bowResult=Linear.predict(BOW, this.testData_BOW.values[string2bow.get(mc)]);
				//
				if(Math.abs(testMCs.get(mc)-mcResult)<1.0) correct_MC++;
				if(Math.abs(testMCs.get(mc)-bowResult)<1.0) correct_BOW++;
				if(Math.abs(testMCs.get(mc)-wordResult)<1.0)  correct_Word++;
				//
				if(Math.abs(testMCs.get(mc)-mcResult)<Math.abs(testMCs.get(mc)-wordResult)) mc_word++;
				if(Math.abs(testMCs.get(mc)-bowResult)<Math.abs(testMCs.get(mc)-wordResult))  bow_word++;
				if(Math.abs(testMCs.get(mc)-mcResult)<Math.abs(testMCs.get(mc)-bowResult)) mc_bow++;
				//System.out.println(mc+" "+testMCs.get(mc)+" "+mcResult+" "+bowResult+" "+mc_bow+"/"+allValidMC+" "+(correct_MC*1.0/allValidMC)+" "+(correct_BOW*1.0/allValidMC));
				//Fwrite.write(mc+"\t"+testMCs.get(mc)+"\t"+mcResult+"\t"+bowResult+"\t"+wordResult+"\t"+mc2markable.get(mc+"_"+testMCs.get(mc))+"\n");
			}		
			if(!string2mc.containsKey(mc)) hasNoMcEmbedding++;
			if(!string2word.containsKey(mc)) hasNoWordEmbedding++;
			if(!string2bow.containsKey(mc)) hasNoWordIndex++;
		}
		//Fwrite.close();
		System.out.println("损失为: "+hasNoMcEmbedding+" "+hasNoWordEmbedding+" "+hasNoWordIndex);
		System.out.println("All test data has: "+allValidMC);
		System.out.println("MC>Word: "+mc_word);
		System.out.println("BOW>Word: "+bow_word);
		System.out.println("MC>BOW: "+mc_bow);
		// compute the accuracy
		double accu_MC=correct_MC*1.0/allValidMC;
		double accu_BOW=correct_BOW*1.0/allValidMC;
		double accu_Word=correct_Word*1.0/allValidMC;
		System.out.println("Accuracies are :"+accu_MC+" "+accu_BOW+" "+accu_Word);
		return String.valueOf(allValidMC)+"("+String.valueOf(mc_bow)+")";
	}
	public  static void main(String[] args) throws IOException
	{
		RunTogether instance=new RunTogether();
		String result=" ";

		instance.buildDataForLiblinear();
		//System.exit(0);
		result+=instance.predict()+" ";

		System.out.println("finished!, the result is: "+result);
	}
}
