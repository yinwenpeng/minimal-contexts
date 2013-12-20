import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;


public class Baselines {
	static double ratio=3.8; // report 4
	int dimension_MC=200;
	int dimension_Concate=400;
	int dimention_bagofWords;
	int MAX_READLINE=10000000;
	static double unbalanedRatio=1.0;
	
	public Map<String, Double> trainingMCs;
	Map<String, Double> testMCs;	
	Data trainingData;
	Data testData;
	String fileNames;
	
	public Model Train(Data trainingData)
	{
		Problem problem=new Problem();
		problem.l=trainingData.samples;
		problem.n=trainingData.featureNo;
		problem.x=trainingData.values;
		problem.y=trainingData.goldLabels;  //double[]		
		//SolverType solver=SolverType.L2R_L2LOSS_SVR_DUAL;  // regression
		SolverType solver=SolverType.L2R_L2LOSS_SVC_DUAL;    // classifying
		double C=1.0; //C is the cost of constraints violation. (we usually use 1 to 1000)
		double eps=0.01; //eps is the stopping criterion. (we usually use 0.01).
		double weights[]={1.0, 3.0}; // set according to our curpus
		int weightlabels[]={1,-1};
		Parameter parameter=new Parameter(solver, C, eps);
		parameter.setWeights(weights, weightlabels);
		Model model=Linear.train(problem, parameter);	
		return model;
	}
	public double predict(Data testData, Model model) throws IOException
	{
		double[] labels=new double[testData.samples];
		int correct=0;
		for(int i=0;i<testData.samples;i++)
		{		
			try {
				labels[i]=Linear.predict(model, testData.values[i]);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(labels[i]==testData.goldLabels[i])
			{
				correct++;
			}
		}

		double accu=correct*1.0/testData.samples;
		System.out.println("Accuracy is: "+accu);
		return accu;
	}

	public void transfer2MatureFeature(ArrayList<FeatureNode[]> trainingLists, ArrayList<FeatureNode[]> testLists, ArrayList<Double>trainingLabels, ArrayList<Double>testLabels, int dimension)
	{

		System.out.println("training data has: "+trainingLists.size()+" ; test data has: "+testLists.size());
		// training data
		trainingData=new Data(trainingLists.size());
		trainingData.samples=trainingLists.size();
		trainingData.featureNo=dimension;
		trainingData.values=new Feature[trainingLists.size()][];
		double PosiSizeInTraining=0.0;
		for(int i=0;i<trainingLists.size();i++)
		{
			trainingData.values[i]=trainingLists.get(i);	
			trainingData.goldLabels[i]=trainingLabels.get(i);
			if(trainingLabels.get(i)==Chain.positiveTag) PosiSizeInTraining++;
		}
		unbalanedRatio=PosiSizeInTraining/(trainingLists.size()-PosiSizeInTraining);
		//test data
		testData=new Data(testLists.size());
		testData.samples=testLists.size();
		testData.featureNo=dimension;
		testData.values=new Feature[testLists.size()][];
		for(int i=0;i<testLists.size();i++)
		{
			testData.values[i]=testLists.get(i);	
			testData.goldLabels[i]=testLabels.get(i);
		}
	}
	public void balancingTrainingData()
	{
		int count_posi=0;
		int count_nega=0;
		for(String mc:trainingMCs.keySet())
		{
			if(trainingMCs.get(mc)==Chain.positiveTag) count_posi++;
			else count_nega++;
		}
		int i=0;
		Set<String> tmp=new HashSet<String>();
		tmp.addAll(trainingMCs.keySet());
		for(String mc:tmp)
		{
			if(i<(count_posi-count_nega)&&trainingMCs.get(mc)==Chain.positiveTag)
			{
				trainingMCs.remove(mc);
				i++;
			}
		}
		System.out.println("After balancing the training data, it has "+count_nega+" vs. "+count_nega);		
	}
}
