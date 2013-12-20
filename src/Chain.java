import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * @author wenpeng This class is used to extract Coreference chains from CoNLL corpus, And does some processing according to our task
 *
 */
public class Chain {

	Map<String, Map<String, String>> chains; // chainIndex, markable, MCs
	Map<Integer, Map<String, Map<String, String>>> docs=new HashMap<Integer, Map<String, Map<String, String>>>();
	Map<String, Boolean>positiveMCs=new HashMap<String, Boolean>(); // use a boolean to mean has a MC embedding or not
	Map<String, Boolean>negativeMCs=new HashMap<String, Boolean>();
	//discarded MCs due to lacking embeddings
	Map<String, Double>RareMCs=new HashMap<String, Double>(); // use a boolean to mean has a MC embedding or not

	
	ArrayList<String> finishedMarkables=new ArrayList<String>();	
	ArrayList<String>markableStack=new ArrayList<String>();
	ArrayList<String>indicesStack=new ArrayList<String>();
	ArrayList<String>MC_leftStack=new ArrayList<String>();
	ArrayList<String>MC_rightStack=new ArrayList<String>();
	
	
	String startRegex="\\([0-9]+";
	String endRegex="[0-9]+\\)";
	boolean thisOneFinished=false;
	static double positiveTag=1.0;
	static double negativeTag=-1.0;
	
	public void storeMatureMarkables(String token)
	{
		if(finishedMarkables.size()!=0)
		{
			for(int i=0;i<finishedMarkables.size(); i++)
				MC_rightStack.add(token);
			formCompleteMarkables(finishedMarkables, markableStack,indicesStack, MC_leftStack, MC_rightStack);
			finishedMarkables.clear();// remove all the finished markables
		}
	}
	
	public void extractChainFromCorpus(String[] fileNames) throws IOException
	{
		String currentLine;
		int docIndex=0;
		
		for(String file:fileNames)
		{
			System.out.println("Extracting coreference chains from file: "+file+"...");
			String temp_MC_left="<BOUNDARY>";
			BufferedReader br = new BufferedReader(new FileReader(file));
			while ((currentLine = br.readLine()) != null) {
				if (!currentLine.isEmpty())
				{
					if(currentLine.contains("#begin"))
					{
						chains=new HashMap<String, Map<String, String>>();
						docIndex++;
						temp_MC_left="<BOUNDARY>";
						continue;
					}
					if(currentLine.contains("#end")) // generate a pair for "docs"
					{
						docs.put(docIndex, chains);
						continue;
					}
					String deli=" +";
					String[] tokens=currentLine.split(deli);
					if(tokens[tokens.length-1].contains("-"))
					{
						storeMatureMarkables(tokens[3]);
						if(markableStack.size()==0) {temp_MC_left=tokens[3]; continue;}
						else{
							addToExistingMarkables(markableStack, tokens[3]);
							temp_MC_left=tokens[3];
						}
					}
					if(tokens[tokens.length-1].contains("("))
					{
						storeMatureMarkables(tokens[3]);
						// a new markable appear
						for(String indice: getMatcher(startRegex, tokens[tokens.length-1], true))
						{
							MC_leftStack.add(temp_MC_left);// first store its left context
							indicesStack.add(indice);
						}
						temp_MC_left=tokens[3];
						if(markableStack.size()!=0)
						{
							addToExistingMarkables(markableStack, tokens[3]); // extend existing markables
						}
						//then, put itself into the stack
						for(int i=0;i<getMatcher(startRegex, tokens[tokens.length-1], true).size();i++)
						{
							markableStack.add(tokens[3]);
						}						
					}
					if(tokens[tokens.length-1].contains(")"))
					{
						storeMatureMarkables(tokens[3]);
						// a markable ends
						if(!tokens[tokens.length-1].contains("("))
						{
							addToExistingMarkables(markableStack, tokens[3]); // extend existing markables
							temp_MC_left=tokens[3];
						}								
						finishedMarkables=getMatcher(endRegex, tokens[tokens.length-1], false);
					}
				}
				else{// empty line
					storeMatureMarkables("<BOUNDARY>");
					temp_MC_left="<BOUNDARY>";
				}
			}
			br.close();
		}
	}
	public void formCompleteMarkables(ArrayList<String> finishedMarkables, ArrayList<String> markableStack,ArrayList<String> indicesStack, ArrayList<String> MC_leftStack, ArrayList<String> MC_rightStack)
	{
		for(String chain:finishedMarkables)
		{
			String chainIndex=chain;
			int layer=indicesStack.indexOf(chain);// get the layer index of the current chain
			indicesStack.remove(layer);
			
			String markable=markableStack.get(layer);
			markableStack.remove(layer);
			String leftMC=MC_leftStack.get(layer);
			MC_leftStack.remove(layer);
			String rightMC=MC_rightStack.get(MC_rightStack.size()-1);
			MC_rightStack.remove(MC_rightStack.size()-1);
			
			if(chains.containsKey(chainIndex))
			{
				if(chains.get(chainIndex).containsKey(markable))// if this markable already has a MC
				{
					chains.get(chainIndex).put(markable, chains.get(chainIndex).get(markable)+" "+leftMC+"_"+rightMC);
				}
				else{ // this is the first time for this markable
					chains.get(chainIndex).put(markable, leftMC+"_"+rightMC);
				}				
			}
			else{// this is a new chain
				Map<String, String> Markable2MC=new HashMap<String, String>();
				Markable2MC.put(markable, leftMC+"_"+rightMC);
				chains.put(chainIndex, Markable2MC);
			}
		}		
	}
	
	public  ArrayList<String> getMatcher(String regex, String source, boolean left)
	{   
		ArrayList<String> chainIndices=new ArrayList<String>();
	    Pattern pattern = Pattern.compile(regex);  
	    Matcher matcher = pattern.matcher(source);  
	    while(matcher.find()) {
	        String g = matcher.group();
	        if(left==true)
	        	chainIndices.add(g.substring(1));
	        else
	        	chainIndices.add(g.substring(0, g.length()-1));
	    }  
	    return chainIndices;
	} 
	
	public void addToExistingMarkables(ArrayList<String>markableStack, String newWord)
	{
		for(int i=0;i<markableStack.size();i++)
		{
			markableStack.set(i, markableStack.get(i)+" "+newWord);
		}
	}
	
	public void printChains()
	{
		for(int doc: docs.keySet())
		{
			System.out.println("\n                                                       Document "+doc+" has following chains:");
			for(String index: docs.get(doc).keySet())
			{
				System.out.println("\nchain index "+index+":");
				for(String markable: docs.get(doc).get(index).keySet())
				{
					System.out.println("\t"+markable);
					String[] MCs=docs.get(doc).get(index).get(markable).split(" ");
					System.out.print("\t\t");
					for(int i=0;i<MCs.length;i++)
					{
						System.out.print(MCs[i]+" ; ");
					}
					System.out.print("\n");
				}
			}
		}
		System.out.println("\nCongrats, printing is finished!!!");
	}
	
	/**only keep chains we want*/
	public void refineChains()
	{
		int chainCount=0;
		for(int doc: docs.keySet())
			chainCount+=docs.get(doc).size();
		System.out.println("there are total "+chainCount+" chains, refining....");
		Set<Integer> removeDocs=new HashSet<Integer>(); // maybe some docs has no satisfied chains, such kind of docs shoule be removed
		for(int doc: docs.keySet())
		{
			if(!docs.get(doc).isEmpty())
			{
				ArrayList<String> removeIndices=new ArrayList<String>();
				for(String index: docs.get(doc).keySet())
				{
					boolean animate=false;
					boolean inanimate=false;
					for(String markable:docs.get(doc).get(index).keySet())
					{
						String lowercase=markable.toLowerCase();
						if(lowercase.equals("she")||lowercase.equals("her")||lowercase.equals("he")||lowercase.equals("him")||lowercase.equals("his"))
							animate=true;
						if(lowercase.equals("it")||lowercase.equals("its"))
							inanimate=true;
					}
					if(animate==inanimate)// both are false or true
						removeIndices.add(index);
				}
				for(String removeIndex:removeIndices)
				{
					docs.get(doc).remove(removeIndex);
				}
				if(docs.get(doc).isEmpty())
				{
					removeDocs.add(doc); // remember empty docs
				}
			}
		}
		//now, remove some empty docs
		for(int doc:removeDocs)
		{
			docs.remove(doc);
			System.out.println(".....................doc "+doc+" has been removed for empty chains.");
		}
		chainCount=0;
		for(int doc: docs.keySet())
			chainCount+=docs.get(doc).size();
		System.out.println("After chain refining: "+chainCount+" chains remain.");
	}
	
	public void printDoc(int doc)
	{
		System.out.println("The chains in doc "+doc+" is as follows:");
		for(String index: docs.get(doc).keySet())
		{
			System.out.println("\nchain index "+index+":");
			for(String markable: docs.get(doc).get(index).keySet())
			{
				System.out.println("\t"+markable);
				String[] MCs=docs.get(doc).get(index).get(markable).split(" ");
				System.out.print("\t\t");
				for(int i=0;i<MCs.length;i++)
				{
					System.out.print(MCs[i]+";");
				}
				System.out.print("\n");
			}
		}
		System.out.println("\nCongrats, printing doc "+doc+" is finished!!!");
	}
	
	public boolean is_inanimate(Map<String, String> chain)
	{
		boolean flag=false;
		for(String markable:chain.keySet())
		{
			if(markable.equals("it")||markable.equals("its"))
			{
				flag=true;
				break;
			}
		}
		return flag;
	}
	
	/**read MC embedding file to remove those MCs who have not embedding representation*/
	public void removeRareMCs() throws IOException
	{	
		BufferedReader br = new BufferedReader(new FileReader(RunTogether.oldMCEmbeddingFile));
		int line=0;
		String currLine, deli=" ";
		while ((currLine = br.readLine()) != null) {
			line++;
			if(line<=1) continue;
			else
			{
				String[] tokens=currLine.split(deli);
				if(positiveMCs.containsKey(tokens[0]))
				{
					positiveMCs.put(tokens[0], true);
				}
				else if(negativeMCs.containsKey(tokens[0]))
				{
					negativeMCs.put(tokens[0], true);
				}
			}
		}
		br.close();
		// remove those false elements
		Set<String>positiveRare=new HashSet<String>();
		positiveRare.addAll(positiveMCs.keySet());
		Set<String>negativeRare=new HashSet<String>();
		negativeRare.addAll(negativeMCs.keySet());
		for(String mc:positiveRare)
		{
			if(positiveMCs.get(mc)==false)
			{
				positiveMCs.remove(mc);
				RareMCs.put(mc, positiveTag);
			}
		}
		for(String mc:negativeRare)
		{
			if(negativeMCs.get(mc)==false)
			{
				negativeMCs.remove(mc);
				RareMCs.put(mc, negativeTag);
			}
		}
		positiveRare.clear();
		negativeRare.clear();
		System.out.println("After removing rare MCs, positive MC has "+positiveMCs.size()+" ; negative MC has "+negativeMCs.size());		
	}
	
	public void removeRareMCsAgain() throws IOException
	{
		BufferedReader br = new BufferedReader(new FileReader(RunTogether.newMCEmbeddingFile));
		int line=0;
		String currLine, deli=" ";
		while ((currLine = br.readLine()) != null) {
			line++;
			if(line<=1) continue;
			else
			{
				String[] tokens=currLine.split(deli);
				if(positiveMCs.containsKey(tokens[0]))
				{
					positiveMCs.put(tokens[0], false);  
				}
				else if(negativeMCs.containsKey(tokens[0]))
				{
					negativeMCs.put(tokens[0], false);
				}
			}
		}
		br.close();
		// remove those false elements
		Set<String>positiveRare=new HashSet<String>();
		positiveRare.addAll(positiveMCs.keySet());
		Set<String>negativeRare=new HashSet<String>();
		negativeRare.addAll(negativeMCs.keySet());
		for(String mc:positiveRare)
		{
			if(positiveMCs.get(mc)==true)
			{
				positiveMCs.remove(mc);
				RareMCs.put(mc, positiveTag);
			}
		}
		for(String mc:negativeRare)
		{
			if(negativeMCs.get(mc)==true)
			{
				negativeMCs.remove(mc);
				RareMCs.put(mc, negativeTag);
			}
		}
		positiveRare.clear();
		negativeRare.clear();
		System.out.println("After removing rare MCs further, positive MC has "+positiveMCs.size()+" ; negative MC has "+negativeMCs.size());		
	}
	
	public void randomDiscard(double ratio)
	{
		Set<String>positiveRare=new HashSet<String>();
		positiveRare.addAll(positiveMCs.keySet());
		Set<String>negativeRare=new HashSet<String>();
		negativeRare.addAll(negativeMCs.keySet());
		int discNo_posi=(int) (positiveMCs.size()*ratio);
		int i=0;
		for(String mc:positiveRare)
		{
			if(i++<discNo_posi)
				positiveMCs.remove(mc);
			else break;
		}
		i=0;
		int discNo_nega=(int)(negativeMCs.size()*ratio);
		for(String mc:negativeRare)
		{
			if(i++<discNo_nega)
				negativeMCs.remove(mc);
		}
		positiveRare.clear();
		negativeRare.clear();
		System.out.println("After random discarding, positive MC has "+positiveMCs.size()+" ; negative MC has "+negativeMCs.size());
	}
	
	public void posiEqualNega()
	{
		if(positiveMCs.size()>negativeMCs.size())
		{
			Set<String>positiveRare=new HashSet<String>();
			positiveRare.addAll(positiveMCs.keySet());
			int gap=positiveMCs.size()-negativeMCs.size();
			int i=0;
			for(String mc:positiveRare)
			{
				if(i<gap) {positiveMCs.remove(mc); i++;}
				else break;
			}
		}
		System.out.println("Now, has equal number of positive and negative: "+positiveMCs.size()+" vs. "+negativeMCs.size());
	}
	
	public void chainsStatistics(boolean repeated)
	{
		if(repeated)
		{
			int chains=0;
			int markables=0;
			int mcs=0;
			String deli=" ";
			// first classified into positive and negative
			for(int doc:docs.keySet())
			{
				for(String index:docs.get(doc).keySet())
				{
					chains++;

						for(String markable:docs.get(doc).get(index).keySet())
						{
							markables++;
							String[] MCs=docs.get(doc).get(index).get(markable).split(deli);
							mcs+=MCs.length;
						}	
				}
			}
			System.out.println("Through statistics of repeated, chains: "+chains+" ; markables: "+markables+" ; MCs "+mcs);
		}
		else{
			int chains=0;
			Set<String> markables=new HashSet<String>();
			Set<String> mcs=new HashSet<String>();
			String deli=" ";
			// first classified into positive and negative
			for(int doc:docs.keySet())
			{
				for(String index:docs.get(doc).keySet())
				{
					chains++;

						for(String markable:docs.get(doc).get(index).keySet())
						{
							if(!markables.contains(markable)) markables.add(markable);
							String[] MCs=docs.get(doc).get(index).get(markable).split(deli);
							for(int i=0;i<MCs.length;i++)
							{
								if(!mcs.contains(MCs[i])) mcs.add(MCs[i]);
							}
						}	
				}
			}
			System.out.println("Through statistics of unrepeated, chains: "+chains+" ; markables: "+markables.size()+" ; MCs "+mcs.size());
		}
	}
	
	public void splitPosiNega() throws IOException
	{
		String deli=" ";
		// first classified into positive and negative
		for(int doc:docs.keySet())
		{
			for(String index:docs.get(doc).keySet())
			{
				if(is_inanimate(docs.get(doc).get(index)))
				{
					for(String markable:docs.get(doc).get(index).keySet())
					{
						String[] MCs=docs.get(doc).get(index).get(markable).split(deli);
						for(String MC: MCs)
						{
							negativeMCs.put(MC, false);  // initially, assume each MC has not MC embedding
							if(RunTogether.mc2markable.containsKey(MC+"_"+negativeTag))
							{
								RunTogether.mc2markable.put(MC+"_"+negativeTag, RunTogether.mc2markable.get(MC+"_"+negativeTag)+"["+markable+"]");
							}
							else RunTogether.mc2markable.put(MC+"_"+negativeTag, "["+markable+"]");
						}
					}
					
				}
				else{
					for(String markable:docs.get(doc).get(index).keySet())
					{
						String[] MCs=docs.get(doc).get(index).get(markable).split(deli);
						for(String MC: MCs)
						{
							positiveMCs.put(MC, false);
							if(RunTogether.mc2markable.containsKey(MC+"_"+positiveTag))
							{
								RunTogether.mc2markable.put(MC+"_"+positiveTag, RunTogether.mc2markable.get(MC+"_"+positiveTag)+"["+markable+"]");
							}
							else RunTogether.mc2markable.put(MC+"_"+positiveTag, "["+markable+"]");
						}
					}
				}
			}
		}
		System.out.println("Before removing rare MCs, positive MC has "+positiveMCs.size()+" ; negative MC has "+negativeMCs.size());	
	}
	
	public void splitTrainingTesting(double ratio, Map<String, Double> trainingMCs, Map<String, Double> testMCs, boolean notMCMethod)
	{
		// first compute how many positive MCs and negative MCs should be put into test data respectively
		int sizeOfTestData=(int)((positiveMCs.size()+negativeMCs.size())/ratio);
		int sizeOfEachCateInTestData=sizeOfTestData/2;
		// store the elements in positiveMCs into an array
		String[] posiMC=new String[positiveMCs.size()];
		int th=0;
		for(String mc:positiveMCs.keySet())
			posiMC[th++]=mc;
		// store the elements in negativeMCs into an array
		String[] negaMC=new String[negativeMCs.size()];
		th=0;
		for(String mc:negativeMCs.keySet())
			negaMC[th++]=mc;
		
		// split
		for(int i=0;i<posiMC.length;i++)
		{
			
			if(i<sizeOfEachCateInTestData) testMCs.put(posiMC[i], positiveTag); 
			else trainingMCs.put(posiMC[i], positiveTag); 
		}
		for(int i=0;i<negaMC.length;i++)
		{
			if(i<sizeOfEachCateInTestData) testMCs.put(negaMC[i], negativeTag); 
			else trainingMCs.put(negaMC[i], negativeTag);
		}
		// add the discarded MCs for missing embedding for BOW and Word methods
		if(notMCMethod)
		{
			trainingMCs.putAll(RareMCs);
		}
		//compute how many positive MCs are in training data
		int count_posi=0;
		for(String mc:trainingMCs.keySet())
		{
			if(trainingMCs.get(mc)==positiveTag) count_posi++;
		}
		System.out.println("trainingData:"+count_posi+" vs. "+(trainingMCs.size()-count_posi)+" ; testData:"+sizeOfEachCateInTestData+" vs. "+sizeOfEachCateInTestData);	
	}
	
	public static void main(String[] args) throws IOException
	{
		Chain ch=new Chain();
		String[] fileNames=new String[1];
		fileNames[0]="replaceToYourFile.txt";
		ch.extractChainFromCorpus(fileNames);
		ch.refineChains();
		//ch.printChains();
		//ch.printDoc(2);
		//ch.splitTrainingTesting();
	}
}
