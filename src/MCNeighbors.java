import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 
 * @author wenpeng It is used to find some nearest MCs for a given MC
 *
 */
public class MCNeighbors {
	Map<String, double[]> embedding=new HashMap<String, double[]>();
	Map<Integer, String> id2mc=new HashMap<Integer, String>();
	String readFile="replaceToYourFile.txt"; // MC embedding file
	String writeFile="mcNeighbors.txt";
	int dimension=200;
	int interestedNo=1000;
	int topK=100;
	Map<String, Double> existingSimi=new HashMap<String, Double>();
	int TotalWanted=400000;
	
	public void loadEmbeddingFile() throws NumberFormatException, IOException
	{
		BufferedReader br = new BufferedReader(new FileReader(readFile));
		int line=0, validMC=0;
		String currLine, deli=" ";
		while ((currLine = br.readLine()) != null) {
			line++;
			System.out.println("loading line "+line+"... and mc "+validMC);
			if(validMC>=TotalWanted) break;
			if(line<=1) continue;
			else if(currLine.indexOf("_")>=0)  // contain MC
			{	
				String[] tokens=currLine.split(deli);
				id2mc.put(validMC++, tokens[0]);
				double[] lineValues=new double[dimension];
				for(int col=1;col<201;col++)
				{
					lineValues[col-1]=Double.valueOf(tokens[col]);	
				}
				embedding.put(tokens[0], normalizeEmbedding(lineValues));	
			}
		}
		br.close();
		System.out.println("MC embedding loads finished!");
	}
	
	public double[] normalizeEmbedding(double[] embedding)
	{
		double length=0.0;
		for(int i=0;i<embedding.length;i++)
		{
			length+=Math.pow(embedding[i], 2.0);
		}
		length=Math.sqrt(length);
		for(int i=0;i<embedding.length;i++)
		{
			embedding[i]/=length;
		}
		return embedding;
	}
	
	public double cosine(double[] a, double[] b)
	{
		double simi=0.0;
		for(int i=0;i<a.length;i++)
		{
			simi+=a[i]*b[i];
		}
		return simi;
	}
	
	public int[] randomArray(int min,int max,int n){  
	    int len = max-min+1;  
	      
	    if(max < min || n > len){  
	        return null;  
	    }  
	    int[] source = new int[len];  
	       for (int i = min; i < min+len; i++)
	       {  
	        source[i-min] = i;  
	       }      
	       int[] result = new int[n];  
	       Random rd = new Random();  
	       int index = 0;  
	       for (int i = 0; i < result.length; i++) {  
	           index = Math.abs(rd.nextInt() % len--);  
	           result[i] = source[index];   
	           source[index] = source[len];  
	       }  
	       return result;  
	} 
	
	public Map<Double, Integer> getSimiMap(int current)
	{
		Map<Double, Integer> result=new HashMap<Double, Integer>();
		for(int i: id2mc.keySet())
		{
			if(i!=current)
			{
				// if this is not the first time to compute the similarity
				if(existingSimi.containsKey(current+"_"+i)) result.put(existingSimi.get(current+"_"+i), i);
				else if (existingSimi.containsKey(i+"_"+current)) result.put(existingSimi.get(i+"_"+current), i);
				else{
					double simi=cosine(embedding.get(id2mc.get(current)), embedding.get(id2mc.get(i)));
					result.put(simi, i);
					existingSimi.put(current+"_"+i, simi);
				}				
			}			
		}
		return result;
	}
	
	public ArrayList<String> sort(Map<Double, Integer> longMap)
	{
		ArrayList<String>shortMap=new ArrayList<String>();
		Object[] key =  longMap.keySet().toArray();    
		Arrays.sort(key);    
		  
		for(int i = key.length-1; i>=key.length-topK; i--)  
		{    
			shortMap.add(id2mc.get(longMap.get(key[i])));  
		}  
		return shortMap;
	}
	
	public ArrayList<String> findKNeighbors(int current)
	{
		Map<Double, Integer>simi2index=new HashMap<Double, Integer>();
		simi2index=getSimiMap(current);		
		// then, sort
		ArrayList<String>topKmc=new ArrayList<String>();
		topKmc=sort(simi2index);
		return topKmc;
	}
	
	public void write2File (FileWriter Fwrite, int index, ArrayList<String> neighbors) throws IOException
	{
		String target=id2mc.get(index);
		Fwrite.write(target);
		for(String mc:neighbors)
		{
			Fwrite.write(" "+mc);
		}
		Fwrite.write("\n");
	}
	
	public  static void main(String[] args) throws NumberFormatException, IOException
	{
		MCNeighbors instance=new MCNeighbors();
		instance.loadEmbeddingFile();
		FileWriter Fwrite = new FileWriter(instance.writeFile);
		int[] mcIndex=instance.randomArray(0,instance.embedding.size()-1,instance.embedding.size());
		for(int i=0; i<instance.interestedNo;i++)
		{
			System.out.println("Computing the "+ i+"th MC......");
			ArrayList<String> neighbors=new ArrayList<String>();
			neighbors=instance.findKNeighbors(mcIndex[i]);
			instance.write2File(Fwrite,mcIndex[i], neighbors);
		}
		Fwrite.close();
		System.out.println("All done...");
	}
}
