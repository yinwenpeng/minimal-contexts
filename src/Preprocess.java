import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 
 * @author wenpeng Given a general corpus, this class reformat it according to the value of k
 *
 */

public class Preprocess {
	public String inputFilePath;
	public String MCFile;
	protected String delimiter = " ";  //tab
	public final static String BOUNDARY = "</s>";
	public Map<String, MC>map;
	
	Preprocess(){};
	Preprocess(String filePath){
		this.inputFilePath=filePath;
		this.MCFile="replaceToYourFile.txt";
		this.map=new HashMap<String, MC>();
	}

	public void buildData() throws IOException
	{
		FileWriter fw = new FileWriter(this.MCFile);
		BufferedReader br = new BufferedReader(new FileReader(this.inputFilePath));
		String currentLine;
		int lineCount=0;
		while ((currentLine = br.readLine()) != null) {
			if (!currentLine.trim().isEmpty()) 
			{
				System.out.println(lineCount++);
				ArrayList<String> newLine=new ArrayList<String>();
				newLine.add(BOUNDARY);
				for (String token:currentLine.split(delimiter))
					newLine.add(token.replaceAll("\\d", "0").toLowerCase(Locale.ENGLISH));
				newLine.add(BOUNDARY);
				//buildMCFile(newLine, fw);	
				buildNewMCFile(newLine, fw);	// build the new formated data
				newLine.clear();
			}
		}
		br.close();
		fw.close();
		System.out.println("buildData over\n");
	}
	
	/**2<=k<=3*/
	public void buildNewMCFile(ArrayList<String> newLine, FileWriter fw) throws IOException
	{
		int left=0;
		int right=newLine.size()-1;
		for(int begin=1; begin<newLine.size()-1;begin++)
		{
			fw.write(newLine.get(begin-1)+"_"+newLine.get(begin+1)+" "+newLine.get(begin)+"\n");// the MC [-1, +1]
			if(Math.abs(begin-left)>=2)
			{
				fw.write(newLine.get(begin-2)+"_"+newLine.get(begin+1)+" "+newLine.get(begin)+"\n");// the [-2, +1]
			}
			if(Math.abs(begin-right)>=2)
			{
				fw.write(newLine.get(begin-1)+"_"+newLine.get(begin+2)+" "+newLine.get(begin)+"\n");// the MC [-1, +2]
			}
			if(Math.abs(begin-left)>=2&&Math.abs(begin-right)>=2)
			{
				fw.write(newLine.get(begin-2)+"_"+newLine.get(begin+2)+" "+newLine.get(begin)+"\n");// the MC [-2, +2]
			}
		}		
	}
	
	/**k==2*/
	public void buildMCFile(ArrayList<String> newLine, FileWriter fw) throws IOException
	{
		for(int begin=1; begin<newLine.size()-1;begin++)
		{
			//write MCs and middle word, each for a sentence
			fw.write(newLine.get(begin-1)+"_"+newLine.get(begin+1)+" "+newLine.get(begin)+"\n");
			if(newLine.get(begin).equals("him")||newLine.get(begin).equals("her"))
			{
				
				if(map.containsKey(newLine.get(begin-1)+"_"+newLine.get(begin+1)))
				{
					map.get(newLine.get(begin-1)+"_"+newLine.get(begin+1)).flag=1;
				}
				else{
					MC mc=new MC();
					mc.firstPosition=map.size();
					mc.flag=1;
					map.put(newLine.get(begin-1)+"_"+newLine.get(begin+1), mc);
				}
			}
			else if (newLine.get(begin).equals("it"))
			{
				if(map.containsKey(newLine.get(begin-1)+"_"+newLine.get(begin+1)))
				{
					map.get(newLine.get(begin-1)+"_"+newLine.get(begin+1)).flag=0;
				}
				else{
					MC mc=new MC();
					mc.firstPosition=map.size();
					mc.flag=0;
					map.put(newLine.get(begin-1)+"_"+newLine.get(begin+1), mc);
				}
			}
		}
	}
	
}
