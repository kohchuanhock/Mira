package testing;

import java.util.Random;

import org.junit.Test;

import sequential.Algorithm;
import sequential.Algorithm.Conclusion;
import sequential.Algorithm.RuleStatus;
import sequential.Mira;
import sequential.YounesA;
import sequential.YounesB;

public class WithoutBayesian {
	@Test
	public void pointTest(){
		/*
		 * Used to test the behavior of this class
		 * 100 - 0, 16
		 * 0 - 4,
		 * 15 - 1, 16
		 * 12 - 1, 16
		 * 21 - 1, 16
		 * 220912 - 1, 12 => Used this for InCob paper
		 */				
		Random rand = new Random(220912);
		
		/*
		 * Settings
		 */
		final Algorithm.Operator operator = Algorithm.Operator.GREATER_EQUAL;
		final int repeats = 1000;
		final double theta = 0.28; //Threshold probability
		final double delta = 0.025;//Half-Width or Indifference region
		final double alpha = 0.01;//FP threshold
		final double beta = alpha;//FN threshold		
		final double gamma = Math.min(alpha, beta);//Undecided probability - Used by Younes B Only
		final int maxSamples = 1000000;//maximum number of samples for each run for each algorithm
		final int maxSamplesForMiraB = 3000; //maximum number of samples for Mira B
		double trueThreshold = 0.25;//Below this will be true
		Conclusion correctConclusion;//H0: p > theta, H1: p < theta
		if(trueThreshold < theta) correctConclusion = Conclusion.H1;
		else if(trueThreshold > theta) correctConclusion = Conclusion.H0;
		else throw new Error("trueThreshold cannot be the same as theta");
		
		/*
		 * Counters
		 */
		int younesATotalSamples = 0;
		int younesATotalCorrect = 0;
		int younesATotalIncorrect = 0;
		int younesATotalMaxSample = 0;
		
		int younesBTotalSamples = 0;
		int younesBTotalCorrect = 0;
		int younesBTotalIncorrect = 0;
		int younesBTotalUndecided = 0;
		int younesBTotalMaxSample = 0;

		int miraATotalSamples = 0;
		int miraATotalCorrect = 0;
		int miraATotalIncorrect = 0;		

		int miraBTotalSamples = 0;
		int miraBTotalCorrect = 0;
		int miraBTotalIncorrect = 0;
		int miraBTotalPValueCorrect = 0;
		int miraBTotalPValueIncorrect = 0;
		int miraBTotalPValueH0 = 0;
		int miraBTotalPValueH1 = 0;
		
		double h0pValue = 0.0;
		double h1pValue = 0.0;
		for(int i = 0; i < repeats; i++){
			/*
			 * Note that the Rule.Operator is not important in the next three constructor since I base on conclusion directly
			 */
			if(i % 100 == 0) System.out.println(i);
			//Without undecided results - YOUNES Algorithm A
			YounesA younesARule = new YounesA("", operator, theta, delta, alpha, beta, maxSamples);
			//With undecided results - YOUNES Algorithm B
			YounesB younesBRule = new YounesB("", operator, theta, delta, alpha, beta, gamma, maxSamples);
			//Without indifference region - MIRA A Algorithm 
			Mira miraARule = new Mira("", operator, theta, alpha, beta, maxSamples);
			//Without indifference region - MIRA B Algorithm
			Mira miraBRule = new Mira("", operator, theta, alpha, beta, maxSamplesForMiraB);

			boolean younesABoolean = true;
			boolean younesBBoolean = true;
			boolean miraABoolean = true;
			boolean miraBBoolean = true;
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
				if(miraABoolean){
					miraATotalSamples++;
					miraARule.update(isTrue, RuleStatus.APNOTSATISFIED);
					miraABoolean = miraARule.obtainAnotherSample();
				}		
				if(miraBBoolean){
					miraBTotalSamples++;
					miraBRule.update(isTrue, RuleStatus.APNOTSATISFIED);
					miraBBoolean = miraBRule.obtainAnotherSample();
				}
			}while(younesABoolean || younesBBoolean || miraABoolean || miraBBoolean);
			
			/*
			 * Obtain the conclusion of each algorithm
			 */
			Conclusion c = younesARule.getConclusion();	
			if(correctConclusion == c) younesATotalCorrect++;
			else if(c == Conclusion.MAXSAMPLESIZE) younesATotalMaxSample++;
			else younesATotalIncorrect++;
			
			c = younesBRule.getConclusion();
			if(correctConclusion == c) younesBTotalCorrect++;
			else if(c == Conclusion.MAXSAMPLESIZE) younesBTotalMaxSample++;
			else if(c == Conclusion.UNDECIDED) younesBTotalUndecided++;
			else younesBTotalIncorrect++;
			
			c = miraARule.getConclusion();
			if(correctConclusion == c) miraATotalCorrect++;
			else if(c == Conclusion.pValueH0 || c == Conclusion.pValueH1) throw new Error("MIRA should not have pValue");
			else if(c == Conclusion.UNDECIDED) throw new Error("MIRA should not have Undecided");			
			else miraATotalIncorrect++;
			
			c = miraBRule.getConclusion();
			if(correctConclusion == c) miraBTotalCorrect++;
			else if(c == Conclusion.pValueH0){
				miraBTotalPValueH0++;
				h0pValue += miraBRule.getH0pValue();
				if(correctConclusion == Conclusion.H0) miraBTotalPValueCorrect++;
				else miraBTotalPValueIncorrect++;
			}else if(c == Conclusion.pValueH1){
				miraBTotalPValueH1++;
				h1pValue += miraBRule.getH1pValue();
				if(correctConclusion == Conclusion.H1) miraBTotalPValueCorrect++;
				else miraBTotalPValueIncorrect++;
			}
			else if(c == Conclusion.UNDECIDED) throw new Error("MIRA should not have Undecided");			
			else miraBTotalIncorrect++;
		}		
		System.out.println("======================================================================");
		System.out.println("Total Runs: " + repeats);		
		System.out.println("Theta: " + theta);
		System.out.println("Delta: " + delta);
		System.out.println("Alpha: " + alpha);
		System.out.println("Beta: " + beta);
		System.out.println("Gamma: " + gamma);
		System.out.println("MaxSamples: " + maxSamples);		
		System.out.println("True Threshold: " + trueThreshold);
		System.out.println("Correct Conclusion: " + correctConclusion);
		System.out.println("======================================================================");
		System.out.println("Younes A Total Samples: " + younesATotalSamples / (repeats + 0.0));
		System.out.println("Younes A Total Correct: " + younesATotalCorrect);
		System.out.println("Younes A Total Incorrect: " + younesATotalIncorrect);
		System.out.println("Younes A Total MaxSample: " + younesATotalMaxSample);
		System.out.println();
		System.out.println("Younes B Total Samples: " + younesBTotalSamples / (repeats + 0.0));
		System.out.println("Younes B Total Correct: " + younesBTotalCorrect);
		System.out.println("Younes B Total Incorrect: " + younesBTotalIncorrect);
		System.out.println("Younes B Total MaxSample: " + younesBTotalMaxSample);
		System.out.println("Younes B Total Undecided: " + younesBTotalUndecided);
		System.out.println();
		System.out.println("MIRA A Total Samples: " + miraATotalSamples / (repeats + 0.0));
		System.out.println("MIRA A Total Correct: " + miraATotalCorrect);
		System.out.println("MIRA A Total Incorrect: " + miraATotalIncorrect);
		System.out.println();
		System.out.println("MIRA B Total Samples: " + miraBTotalSamples / (repeats + 0.0));
		System.out.println("MIRA B Total Correct: " + miraBTotalCorrect);
		System.out.println("MIRA B Total Incorrect: " + miraBTotalIncorrect);
		System.out.println("MIRA B Total Pvalue Correct: " + miraBTotalPValueCorrect);
		System.out.println("MIRA B Total Pvalue Incorrect: " + miraBTotalPValueIncorrect);
		System.out.println("MIRA B Total PValueH0: " + miraBTotalPValueH0);
		System.out.println("MIRA B H0 PValue Average: " + h0pValue / miraBTotalPValueH0);
		System.out.println("MIRA B Total PValueH1: " + miraBTotalPValueH1);
		System.out.println("MIRA B H1 PValue Average: " + h1pValue / miraBTotalPValueH1);
		System.out.println();
		System.out.println("======================================================================");
	}
}
