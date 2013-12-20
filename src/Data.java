import de.bwaldvogel.liblinear.Feature;


public class Data {
	String[] names;
	Feature[][] values;
	double[] goldLabels;
	int samples;
	int featureNo;
	
	Data(int length) {
		this.names=new String[length];
		this.goldLabels=new double[length];
	}

}
