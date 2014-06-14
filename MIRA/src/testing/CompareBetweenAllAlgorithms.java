package testing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Test;

import sequential.Algorithm;
import sequential.Algorithm.Conclusion;
import sequential.Algorithm.RuleStatus;
import sequential.BayesianA;
import sequential.BayesianB;
import sequential.Mira;
import sequential.YounesA;
import sequential.YounesB;
import afc.basic.datastructure.New;
import afc.graphing.r.R;
import afc.graphing.r.RGraph;
import afc.graphing.r.RPlotGraph;
import afc.utils.Binning;

public class CompareBetweenAllAlgorithms {
	
	@Test
	public void generateGraph(){
		/*
		 * Compare between Younes A, Younes B, MIRA A and MIRA B, and generates the following four graphs:
		 * Graph 1 - y = sample size and x = theta
		 * Graph 2 - y = errors + undecided and x = theta
		 * Graph 3 - y = errors, x = theta
		 * Graph 4 - y = undecided, x = theta
		 */
		double binWidth = 0.05; 
		final double trueThreshold = 0.25;
		final double delta = 0.025;//Indifference Region
		
		final int repeats = 1000;
		final double thetaStart = 0.00;
		final double thetaEnd = 1.0;
		final double thetaInterval = 0.01;
		final double alpha = 0.01;
		final double beta = alpha;
		final double gamma = Math.min(alpha, beta);
		final int maxSamples = 3000;//Sample limit for MIRA B
		//Parameters for Bayesian Model Checking algorithms
		final double bayesFactorThreshold = 10000.0;//Threshold for Bayesian A
		final double bayesBThreshold = 0.00001;//Threshold for Bayesian B
		final boolean showBayesA = false;
		final boolean showBayesB = false;
		final double yaxisLimit = 25000.0; 
		
		List<Double> thetaList = New.arrayList();
		int nThreads = Runtime.getRuntime().availableProcessors();//Use all available processors 
		ExecutorService threadExecutor = Executors.newFixedThreadPool(nThreads);
		List<Future<Results>> resultList = new ArrayList<Future<Results>>();
		
		for(BigDecimal theta = new BigDecimal(thetaStart + ""); theta.doubleValue() <= thetaEnd; theta = theta.add(new BigDecimal(thetaInterval + ""))){
			if(trueThreshold == theta.doubleValue()) continue;//skip this since mira will not terminate
			thetaList.add(theta.doubleValue());
			resultList.add(threadExecutor.submit(new Compute(trueThreshold, theta.doubleValue(), delta, alpha, beta, gamma, repeats, maxSamples,
					trueThreshold > theta.doubleValue(), bayesFactorThreshold, bayesBThreshold, showBayesA, showBayesB)));
		}
		threadExecutor.shutdown();
	
		try{
			List<Double> younesATotalSampleList = New.arrayList();
			List<Double> younesATotalIncorrectList = New.arrayList();
			
			List<Double> bayesianATotalSampleList = New.arrayList();
			List<Double> bayesianATotalIncorrectList = New.arrayList();
			
			List<Double> bayesianBTotalSampleList = New.arrayList();
			List<Double> bayesianBTotalIncorrectList = New.arrayList();
			
			List<Double> younesBTotalSampleList = New.arrayList();
			List<Double> younesBTotalIncorrectList = New.arrayList();
			List<Double> younesBTotalUndecidedList = New.arrayList();
			List<Double> younesBTotalIncorrectPlusUndecidedList = New.arrayList();
			
			List<Double> miraATotalSampleList = New.arrayList();
			List<Double> miraATotalIncorrectList = New.arrayList();
			List<Double> miraATotalIncorrectSampleSizeList = New.arrayList();
			
			List<Double> miraBTotalSampleList = New.arrayList();
			List<Double> miraBTotalIncorrectList = New.arrayList();
			List<Double> miraBTotalIncorrectSampleSizeList = New.arrayList();
			List<Double> miraBTotalIncorrectPlusByPValueList = New.arrayList();
			List<Double> miraBTotalCorrectPValueList = New.arrayList();
			List<Double> miraBTotalIncorrectPValueList = New.arrayList();
			
			//This defers from the above in that it keeps its original values
			List<Double> miraBCorrectPValueList = New.arrayList();
			List<Double> miraBIncorrectPValueList = New.arrayList();
			
			for(Future<Results> r:resultList){
				younesATotalSampleList.add((r.get().getTotalSamples(Results.Algorithm.YOUNESA) + 0.0) / repeats);
				younesATotalIncorrectList.add((r.get().getTotalIncorrect(Results.Algorithm.YOUNESA) + 0.0) / repeats);
				
				younesBTotalSampleList.add((r.get().getTotalSamples(Results.Algorithm.YOUNESB) + 0.0) / repeats);
				younesBTotalIncorrectList.add((r.get().getTotalIncorrect(Results.Algorithm.YOUNESB) + 0.0) / repeats);
				younesBTotalUndecidedList.add((r.get().getTotalUndecided(Results.Algorithm.YOUNESB) + 0.0) / repeats);
				younesBTotalIncorrectPlusUndecidedList.add((r.get().getTotalIncorrectPlusUndecided(Results.Algorithm.YOUNESB) + 0.0) / repeats);
				
				miraATotalSampleList.add((r.get().getTotalSamples(Results.Algorithm.MIRAA) + 0.0) / repeats);
				miraATotalIncorrectList.add((r.get().getTotalIncorrect(Results.Algorithm.MIRAA) + 0.0) / repeats);
				miraATotalIncorrectSampleSizeList.add((r.get().getTotalIncorrectSampleSize(Results.Algorithm.MIRAA) + 0.0) / 
						r.get().getTotalIncorrect(Results.Algorithm.MIRAA));
				
				miraBTotalSampleList.add((r.get().getTotalSamples(Results.Algorithm.MIRAB) + 0.0) / repeats);
				miraBTotalIncorrectList.add((r.get().getTotalIncorrect(Results.Algorithm.MIRAB) + 0.0) / repeats);
				miraBTotalIncorrectSampleSizeList.add((r.get().getTotalIncorrectSampleSize(Results.Algorithm.MIRAB) + 0.0) /
						r.get().getTotalIncorrect(Results.Algorithm.MIRAB));
				miraBTotalIncorrectPlusByPValueList.add((r.get().getTotalIncorrect(Results.Algorithm.MIRAB) + 
						r.get().getIncorrectByPValue() + 0.0) / repeats);				
				miraBTotalCorrectPValueList.add((r.get().getCorrectPValue() + 0.0) / r.get().getCorrectByPValue());
				miraBTotalIncorrectPValueList.add((r.get().getIncorrectPValue() + 0.0) / r.get().getIncorrectByPValue());
				miraBCorrectPValueList.addAll(r.get().getCorrectPValueList());
				miraBIncorrectPValueList.addAll(r.get().getIncorrectPValueList());
				
				if(showBayesA){
					bayesianATotalSampleList.add((r.get().getTotalSamples(Results.Algorithm.BAYESA) + 0.0) / repeats);
					bayesianATotalIncorrectList.add((r.get().getTotalIncorrect(Results.Algorithm.BAYESA) + 0.0) / repeats);
				}
				
				if(showBayesB){
					bayesianBTotalSampleList.add((r.get().getTotalSamples(Results.Algorithm.BAYESB) + 0.0) / repeats);
					bayesianBTotalIncorrectList.add((r.get().getTotalIncorrect(Results.Algorithm.BAYESB) + 0.0) / repeats);
				}
			}
			String younesA = "Younes A";
			String younesB = "Younes B";
			String miraA = "OSM A";
			String miraB = "OSM B";
			String bayesA = "Bayesian A";
			String theta = "expression(theta)";
			String sampleSize = "Average Sample Size";
			String errors = "Error/Undecided Rate";
			String pValue = "Average P-Value";
			
			RGraph graph1_YounesA = RGraph.init(younesATotalSampleList, thetaList).xLabel(theta).yLabel(sampleSize).legendTitle(younesA).build();
			RGraph graph1_YounesB = RGraph.init(younesBTotalSampleList, thetaList).xLabel(theta).yLabel(sampleSize).legendTitle(younesB).build();
			RGraph graph1_MiraA = RGraph.init(miraATotalSampleList, thetaList).xLabel(theta).yLabel(sampleSize).legendTitle(miraA).build();
			RGraph graph1_MiraB = RGraph.init(miraBTotalSampleList, thetaList).xLabel(theta).yLabel(sampleSize).legendTitle(miraB).build();
			List<RGraph> graph1 = New.arrayList();
			graph1.add(graph1_YounesA);
			graph1.add(graph1_YounesB);
			graph1.add(graph1_MiraA);
			graph1.add(graph1_MiraB);
			if(showBayesA){
				RGraph graph1_BayesA = RGraph.init(bayesianATotalSampleList, thetaList).xLabel(theta).yLabel(sampleSize).legendTitle(bayesA).build();
				graph1.add(graph1_BayesA);
			}
			
			RGraph graph2_YounesA = RGraph.init(younesATotalIncorrectList, thetaList).xLabel(theta).yLabel(errors).legendTitle(younesA).build();
			RGraph graph2_YounesB = RGraph.init(younesBTotalIncorrectPlusUndecidedList, thetaList).xLabel(theta).yLabel(errors).legendTitle(younesB).build();
			RGraph graph2_MirachA = RGraph.init(miraATotalIncorrectList, thetaList).xLabel(theta).yLabel(errors).legendTitle(miraA).build();
			RGraph graph2_MirachB = RGraph.init(miraBTotalIncorrectPlusByPValueList, thetaList).xLabel(theta).yLabel(errors).legendTitle(miraB).build();
			List<RGraph> graph2 = new ArrayList<RGraph>();
			graph2.add(graph2_YounesA);
			graph2.add(graph2_YounesB);
			graph2.add(graph2_MirachA);
			graph2.add(graph2_MirachB);
			if(showBayesA){
				RGraph graph2_BayesA = RGraph.init(bayesianATotalIncorrectList, thetaList).xLabel(theta).yLabel(errors).legendTitle(bayesA).build();
				graph2.add(graph2_BayesA);
			}
			
			RGraph graph3_MirachBCorrectPValue = RGraph.init(miraBTotalCorrectPValueList, thetaList).legendTitle(miraB+"_Correct").xLabel(theta).yLabel(pValue).build();
			RGraph graph3_MirachBIncorrectPValue = RGraph.init(miraBTotalIncorrectPValueList, thetaList).legendTitle(miraB+"_InCorrect").xLabel(theta).yLabel(pValue).build();
			List<RGraph> graph3 = new ArrayList<RGraph>();
			graph3.add(graph3_MirachBCorrectPValue);
			graph3.add(graph3_MirachBIncorrectPValue);
			
			int numOfInterval = (int)((0.5 - binWidth) / binWidth) + 1;
			double min = 0.0;
			double[][] xyAxisCorrect = Binning.fixedWidthBinning(miraBCorrectPValueList, numOfInterval, binWidth, min);
			double[][] xyAxisIncorrect = Binning.fixedWidthBinning(miraBIncorrectPValueList, numOfInterval, binWidth, min);
			RGraph graph4_Correct = RGraph.init(xyAxisCorrect[1], xyAxisCorrect[0]).legendTitle(miraB+"_Correct").xLabel("pValue").yLabel("Frequency").build();
			RGraph graph4_Incorrect = RGraph.init(xyAxisIncorrect[1], xyAxisIncorrect[0]).legendTitle(miraB+"_Incorrect").xLabel("pValue").yLabel("Frequency").build();
			List<RGraph> graph4 = New.arrayList();
			graph4.add(graph4_Correct);
			graph4.add(graph4_Incorrect);
			
			double[][] combined = new double[xyAxisCorrect.length][xyAxisCorrect[0].length];
			for(int j = 0; j < combined[0].length; j++){
				combined[0][j] = xyAxisCorrect[0][j];
			}
			for(int j = 0; j < combined[0].length; j++){
				combined[1][j] = xyAxisCorrect[1][j] / (xyAxisCorrect[1][j] + xyAxisIncorrect[1][j]);
			}
			RGraph graph5_g = RGraph.init(combined[1], combined[0]).xLabel("pValue").yLabel("P(Correct)").build();
			List<RGraph> graph5 = new ArrayList<RGraph>();
			graph5.add(graph5_g);
			
			
			final String outputLocation = "./graphs/";
			R r = new R();
			StringBuffer s = RPlotGraph.init(graph1, outputLocation + "Graph1_" + trueThreshold + "_" + delta + ".pdf").yMin(0.0).yMax(yaxisLimit).build().draw();
			r.runCode(s, false);
			
			s = RPlotGraph.init(graph2, outputLocation + "Graph2_" + trueThreshold + "_" + delta + ".pdf").yMin(0.0).yMax(1.0).build().draw();
			r.runCode(s);
			
			s = RPlotGraph.init(graph3, outputLocation + "Graph3_" + trueThreshold + "_" + delta + ".pdf").graphTitle("Avg P-Value vs Theta").build().draw();
			r.runCode(s);
			
			s = RPlotGraph.init(graph4, outputLocation + "Graph4_" + trueThreshold + "_" + delta + ".pdf").graphTitle("Frequency vs PValue").build().draw();
			r.runCode(s);
			
			s = RPlotGraph.init(graph5_g, outputLocation + "Graph5_" + trueThreshold + "_" + delta + ".pdf").build().draw();
			r.runCode(s);
			
			System.out.println("Finish");
		}catch(Exception e){e.printStackTrace();}
	}
		
}

class Results{
	/*
	 * This class is to store the results of the comparison as it runs
	 */
	public enum Algorithm{
		YOUNESA, YOUNESB, MIRAA, MIRAB, BAYESA, BAYESB
	}
	
	private int younesATotalSamples;
	private int younesATotalCorrect;
	private int younesATotalIncorrect;
	
	private int younesBTotalSamples;
	private int younesBTotalCorrect;
	private int younesBTotalIncorrect;
	private int younesBTotalUndecided;
	
	private int bayesATotalSamples;
	private int bayesATotalCorrect;
	private int bayesATotalIncorrect;
	
	private int bayesBTotalSamples;
	private int bayesBTotalCorrect;
	private int bayesBTotalIncorrect;
	
	private int miraATotalSamples;
	private int miraATotalCorrect;
	private int miraATotalIncorrect;
	private int miraATotalIncorrectSampleSize;
	
	private int miraBTotalSamples;
	private int miraBTotalCorrect;
	private int miraBTotalIncorrect;
	private int miraBTotalIncorrectSampleSize;
	private int miraBTotalH0;
	private int miraBTotalH1;
	private double miraBTotalH0Pvalue;
	private double miraBTotalH1Pvalue;
	private List<Double> miraBH0PvalueList;
	private List<Double> miraBH1PvalueList;
	
	private boolean isCorrectConclusionH0;
	
	public void setYounesA(int totalSamples, int totalCorrect, int totalIncorrect){
		this.younesATotalSamples = totalSamples;
		this.younesATotalCorrect = totalCorrect;
		this.younesATotalIncorrect = totalIncorrect;
	}
	
	public void setYounesB(int totalSamples, int totalCorrect, int totalIncorrect, int totalUndecided){
		this.younesBTotalSamples = totalSamples;
		this.younesBTotalCorrect = totalCorrect;
		this.younesBTotalIncorrect = totalIncorrect;
		this.younesBTotalUndecided = totalUndecided;
	}
	
	public void setBayesA(int totalSamples, int totalCorrect, int totalIncorrect){
		this.bayesATotalSamples = totalSamples;
		this.bayesATotalCorrect = totalCorrect;
		this.bayesATotalIncorrect = totalIncorrect;
	}
	
	public void setBayesB(int totalSamples, int totalCorrect, int totalIncorrect){
		this.bayesBTotalSamples = totalSamples;
		this.bayesBTotalCorrect = totalCorrect;
		this.bayesBTotalIncorrect = totalIncorrect;
	}
	
	public void setMirachA(int totalSamples, int totalCorrect, int totalIncorrect, int incorrectSampleSize){
		this.miraATotalSamples = totalSamples;
		this.miraATotalCorrect = totalCorrect;
		this.miraATotalIncorrect = totalIncorrect;
		this.miraATotalIncorrectSampleSize = incorrectSampleSize;
	}
	
	public void setMirachB(int totalSamples, int totalCorrect, int totalIncorrect, int totalIncorrectSampleSize, int totalH0, int totalH1,
			double totalH0Pvalue, double totalH1Pvalue, boolean isCorrectConclusionH0, List<Double> miraBH0PvalueList, List<Double> miraBH1PvalueList){
		this.miraBTotalSamples = totalSamples;
		this.miraBTotalCorrect = totalCorrect;
		this.miraBTotalIncorrect = totalIncorrect;
		this.miraBTotalIncorrectSampleSize = totalIncorrectSampleSize;
		this.miraBTotalH0 = totalH0;
		this.miraBTotalH1 = totalH1;
		this.miraBTotalH0Pvalue = totalH0Pvalue;
		this.miraBTotalH1Pvalue = totalH1Pvalue;
		this.isCorrectConclusionH0 = isCorrectConclusionH0;
		this.miraBH0PvalueList = miraBH0PvalueList;
		this.miraBH1PvalueList = miraBH1PvalueList;
	}
	
	public int getTotalIncorrectSampleSize(Algorithm a){
		switch(a){
		case MIRAA: return this.miraATotalIncorrectSampleSize;
		case MIRAB: return this.miraBTotalIncorrectSampleSize;
		default: throw new Error("Unhandled Algorithm");
		}
	}
	
	public int getTotalSamples(Algorithm a){
		switch(a){
		case YOUNESA: return this.younesATotalSamples;
		case YOUNESB: return this.younesBTotalSamples;
		case MIRAA: return this.miraATotalSamples;
		case MIRAB: return this.miraBTotalSamples;
		case BAYESA: return this.bayesATotalSamples;
		case BAYESB: return this.bayesBTotalSamples;
		default: throw new Error("Unhandled Algorithm");
		}
	}
	
	public int getTotalCorrect(Algorithm a){
		switch(a){
		case YOUNESA: return this.younesATotalCorrect;
		case YOUNESB: return this.younesBTotalCorrect;
		case MIRAA: return this.miraATotalCorrect;
		case MIRAB: return this.miraBTotalCorrect;
		case BAYESA: return this.bayesATotalCorrect;
		case BAYESB: return this.bayesBTotalCorrect;
		default: throw new Error("Unhandled Algorithm");
		}
	}
	
	public int getTotalIncorrect(Algorithm a){
		switch(a){
		case YOUNESA: return this.younesATotalIncorrect;
		case YOUNESB: return this.younesBTotalIncorrect;
		case MIRAA: return this.miraATotalIncorrect;
		case MIRAB: return this.miraBTotalIncorrect;
		case BAYESA: return this.bayesATotalIncorrect;
		case BAYESB: return this.bayesBTotalIncorrect;
		default: throw new Error("Unhandled Algorithm");
		}
	}
	
	public int getTotalUndecided(Algorithm a){
		switch(a){
		case YOUNESB: return this.younesBTotalUndecided;
		default: throw new Error("Unhandled Algorithm");
		}
	}
	
	public int getTotalIncorrectPlusUndecided(Algorithm a){
		switch(a){
		case YOUNESB: return this.younesBTotalIncorrect + this.younesBTotalUndecided;
		default: throw new Error("Unhandled Algorithm");
		}
	}
	
	public int getTotalH0(){ return this.miraBTotalH0; }
	public int getTotalH1(){ return this.miraBTotalH1; }
	public double getTotalH0ByPValue(){ return this.miraBTotalH0Pvalue; }
	public double getTotalH1ByPValue(){ return this.miraBTotalH1Pvalue; }
	
	public int getIncorrectByPValue(){
		//Returns the number with incorrect conclusion
		if(this.isCorrectConclusionH0) return this.miraBTotalH1;
		else return this.miraBTotalH0;
	}
	
	public int getCorrectByPValue(){
		//Returns the number with correct conclusion
		if(this.isCorrectConclusionH0) return this.miraBTotalH0;
		else return this.miraBTotalH1;
	}
	
	public double getCorrectPValue(){
		//Returns the pvalue for the correct conclusion
		if(this.isCorrectConclusionH0) return this.miraBTotalH0Pvalue;
		else return this.miraBTotalH1Pvalue;
	}
	
	public double getIncorrectPValue(){
		//Returns the pvalue for the incorrect conclusion
		if(this.isCorrectConclusionH0) return this.miraBTotalH1Pvalue;
		else return this.miraBTotalH0Pvalue;
	}
	
	public List<Double> getCorrectPValueList(){
		if(this.isCorrectConclusionH0) return this.miraBH0PvalueList;
		else return this.miraBH1PvalueList;
	}
	
	public List<Double> getIncorrectPValueList(){
		if(this.isCorrectConclusionH0) return this.miraBH1PvalueList;
		else return this.miraBH0PvalueList;
	}
}

class Compute implements Callable<Results>{
	/*
	 * This class is used to give the program ability to run in parallel
	 */
	
	/*
	 * 	Here's your problem:

		while (!executor.isTerminated()) {
		
		}
	
		Your "main" method is spinning the CPU doing nothing. Use invokeAll() instead, and your thread will block without a busy wait.
		
		final ExecutorService executor = Executors.newFixedThreadPool(threadAmount);
		final List<Callable<Object>> tasks = new ArrayList<Callable<Object>>();
		
		for (int i = 1; i < 50; i++) {
		    tasks.add(Executors.callable(new ActualThread(i)));
		}
		executor.invokeAll(tasks);
		executor.shutdown();  // not really necessary if the executor goes out of scope.
		System.out.println("Finished all threads");
		Since invokeAll() wants a collection of Callable, note the use of the helper method Executors.callable(). 
		You can actually use this to get a collection of Futures for the tasks as well, 
		which is useful if the tasks are actually producing something you want as output.
	 */
	
	private double trueThreshold;
	private double theta;
	private double delta;
	private double alpha;
	private double beta;
	private double gamma;
	private int repeats;
	private int maxSamples;
	private boolean isCorrectConclusionH0;
	private double bayesFactorThreshold;
	private double bayesBThreshold;
	private boolean showBayesA;
	private boolean showBayesB;
	
	public Compute(double trueThreshold, double theta, double delta, double alpha, double beta, double gamma, int repeats, int maxSamples,
			boolean isCorrectConclusionH0, double bayesianThreshold, double bayesBThreshold, boolean showBayesA, boolean showBayesB){
		this.trueThreshold = trueThreshold;
		this.theta = theta;
		this.delta = delta;
		this.alpha = alpha;
		this.beta = beta;
		this.gamma = gamma;
		this.repeats = repeats;
		this.maxSamples = maxSamples;
		this.isCorrectConclusionH0 = isCorrectConclusionH0;
		this.bayesFactorThreshold = bayesianThreshold;
		this.bayesBThreshold = bayesBThreshold;
		this.showBayesA = showBayesA;
		this.showBayesB = showBayesB;
	}

	public Results call() throws Exception {
		Conclusion correctConclusion;//H0: p > theta, H1: p < theta
		if(trueThreshold < theta) correctConclusion = Conclusion.H1;
		else if(trueThreshold > theta) correctConclusion = Conclusion.H0;
		else throw new Error("trueThreshold cannot be the same as theta");
		
		int younesATotalSamples = 0;
		int younesATotalCorrect = 0;
		int younesATotalIncorrect = 0;
		
		int younesBTotalSamples = 0;
		int younesBTotalCorrect = 0;
		int younesBTotalIncorrect = 0;
		int younesBTotalUndecided = 0;
		
		int bayesianATotalSamples = 0;
		int bayesianATotalCorrect = 0;
		int bayesianATotalIncorrect = 0;
		
		int bayesianBTotalSamples = 0;
		int bayesianBTotalCorrect = 0;
		int bayesianBTotalIncorrect = 0;
		
		int miraATotalSamples = 0;
		int miraATotalCorrect = 0;
		int miraATotalIncorrect = 0;
		int miraATotalIncorrectSampleSize = 0;
		
		int miraBTotalSamples = 0;
		int miraBTotalCorrect = 0;
		int miraBTotalIncorrect = 0;
		int miraBTotalIncorrectSampleSize = 0;
		int miraBTotalH0 = 0;
		int miraBTotalH1 = 0;
		double miraBTotalH0pValue = 0.0;
		double miraBTotalH1pValue = 0.0;
		List<Double> miraBH0pValueList = new ArrayList<Double>();
		List<Double> miraBH1pValueList = new ArrayList<Double>();
		
		Random rand = new Random();
		for(int i = 0; i < repeats; i++){
			/*
			 * Note that the Rule.Operator is not important in the next three constructor since I base on conclusion directly
			 */
			if(i % 100 == 0) System.out.println(this.theta + ": " + i);
			//Without undecided results - YOUNES Algorithm A
			YounesA younesARule = new YounesA("", Algorithm.Operator.GREATER, this.theta, this.delta, this.alpha, this.beta, 0);
			//With undecided results - YOUNES Algorithm B
			YounesB younesBRule = new YounesB("", Algorithm.Operator.GREATER, this.theta, this.delta, this.alpha, this.beta, this.gamma, 0);
			//Without indifference region - MIRA Algorithm A without sample limit
			Mira miraARule = new Mira("", Algorithm.Operator.GREATER, this.theta, this.alpha, this.beta, 0);
			//Without indifference region - MIRA Algorithm B with sample limit
			Mira miraBRule = new Mira("", Algorithm.Operator.GREATER, this.theta, this.alpha, this.beta, this.maxSamples);
			//Bayesian A
			BayesianA bayesARule = new BayesianA("", Algorithm.Operator.GREATER, this.theta, this.alpha, this.beta, this.bayesFactorThreshold, 0);
			//Bayesian B
			BayesianB bayesBRule = new BayesianB("", Algorithm.Operator.GREATER, this.theta, this.alpha, this.beta, this.bayesBThreshold, 0);
			/*
			 * Do sampling
			 */
			boolean younesABoolean = true;
			boolean younesBBoolean = true;
			boolean miraABoolean = true;
			boolean miraBBoolean = true;
			boolean bayesABoolean = this.showBayesA;
			boolean bayesBBoolean = this.showBayesB;
			
			int currentMirachASampleSize = 0;
			int currentMirachBSampleSize = 0;
			do{
				boolean isTrue = false;
				/*
				 * Stochastic simulation of true/false based on true threshold
				 */
				if(rand.nextDouble() < trueThreshold) isTrue = true; 
				
				if(younesABoolean){
					younesATotalSamples++;
					younesARule.update(isTrue, RuleStatus.APNOTSATISFIED);
					younesABoolean = younesARule.obtainAnotherSample();					
				}
				if(younesBBoolean){
					younesBTotalSamples++;
					younesBRule.update(isTrue, RuleStatus.APNOTSATISFIED);
					younesBBoolean = younesBRule.obtainAnotherSample();					
				}			
				if(bayesABoolean){
					bayesianATotalSamples++;
					bayesARule.update(isTrue, RuleStatus.APNOTSATISFIED);
					bayesABoolean = bayesARule.obtainAnotherSample();
				}
				if(bayesBBoolean){
					bayesianBTotalSamples++;
					bayesBRule.update(isTrue, RuleStatus.APNOTSATISFIED);
					bayesBBoolean = bayesBRule.obtainAnotherSample();
				}
				if(miraABoolean){
					currentMirachASampleSize++;
					miraATotalSamples++;
					miraARule.update(isTrue, RuleStatus.APNOTSATISFIED);
					miraABoolean = miraARule.obtainAnotherSample();
				}				
				if(miraBBoolean){
					currentMirachBSampleSize++;
					miraBTotalSamples++;
					miraBRule.update(isTrue, RuleStatus.APNOTSATISFIED);
					miraBBoolean = miraBRule.obtainAnotherSample();
				}
			}while(younesABoolean || younesBBoolean || miraABoolean || miraBBoolean || bayesABoolean || bayesBBoolean);
			
			/*
			 * Obtain the conclusion of each algorithm
			 */
			
			//Younes A
			Conclusion c = younesARule.getConclusion();	
			if(correctConclusion == c) younesATotalCorrect++;
			else if(c == Conclusion.MAXSAMPLESIZE) throw new Error("Younes A should not hit max sample size");
			else younesATotalIncorrect++;
			//Younes B
			c = younesBRule.getConclusion();
			if(correctConclusion == c) younesBTotalCorrect++;
			else if(c == Conclusion.MAXSAMPLESIZE) throw new Error("Younes B should not hit max sample size");
			else if(c == Conclusion.UNDECIDED) younesBTotalUndecided++;
			else younesBTotalIncorrect++;
			//Bayes A
			c = bayesARule.getConclusion();
			if(correctConclusion == c) bayesianATotalCorrect++;
			else if(c == Conclusion.MAXSAMPLESIZE) throw new Error("Bayes A should not hit max sample size");
			else bayesianATotalIncorrect++;
			//Bayes B
			c = bayesBRule.getConclusion();
			if(correctConclusion == c) bayesianBTotalCorrect++;
			else if(c == Conclusion.MAXSAMPLESIZE) throw new Error("Bayes B should not hit max sample size");
			else bayesianBTotalIncorrect++;
			//Mira A
			c = miraARule.getConclusion();
			if(correctConclusion == c) miraATotalCorrect++;
			else if(c == Conclusion.pValueH0 || c == Conclusion.pValueH1) throw new Error("Mira A should not hit max sample size: " + this.theta);
			else{
				miraATotalIncorrect++;
				miraATotalIncorrectSampleSize += currentMirachASampleSize;
			}
			//Mira B
			c = miraBRule.getConclusion();
			if(correctConclusion == c) miraBTotalCorrect++;
			else if(c == Conclusion.pValueH0){
				miraBTotalH0++;
				double pValue = miraBRule.getH0pValue();
				miraBTotalH0pValue += pValue; 
				if(pValue > 0.5) pValue = 0.49999;//Need this because the computation of p-value is not super precise
				miraBH0pValueList.add(pValue);
			}else if(c == Conclusion.pValueH1){
				miraBTotalH1++;
				double pValue = miraBRule.getH1pValue();
				miraBTotalH1pValue += pValue;
				if(pValue > 0.5) pValue = 0.49999;//Need this because the computation of p-value is not super precise
				miraBH1pValueList.add(pValue);
			}else{
				miraBTotalIncorrect++;
				miraBTotalIncorrectSampleSize += currentMirachBSampleSize;
			}
		}		
		System.out.println(this.theta + ": DONE!");
		Results r = new Results();
		r.setYounesA(younesATotalSamples, younesATotalCorrect, younesATotalIncorrect);
		r.setYounesB(younesBTotalSamples, younesBTotalCorrect, younesBTotalIncorrect, younesBTotalUndecided);
		r.setMirachA(miraATotalSamples, miraATotalCorrect, miraATotalIncorrect, miraATotalIncorrectSampleSize);
		r.setMirachB(miraBTotalSamples, miraBTotalCorrect, miraBTotalIncorrect, miraBTotalIncorrectSampleSize, miraBTotalH0, 
				miraBTotalH1, miraBTotalH0pValue, miraBTotalH1pValue, this.isCorrectConclusionH0, miraBH0pValueList, miraBH1pValueList);
		if(this.showBayesA) r.setBayesA(bayesianATotalSamples, bayesianATotalCorrect, bayesianATotalIncorrect);
		if(this.showBayesB) r.setBayesB(bayesianBTotalSamples, bayesianBTotalCorrect, bayesianBTotalIncorrect);
		return r;
	}
}
