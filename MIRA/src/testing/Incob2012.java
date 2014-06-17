package testing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import sequential.Algorithm;
import sequential.Algorithm.RuleStatus;
import sequential.YounesA;
import sequential.YounesB;
import afc.graphing.r.R;
import afc.graphing.r.RGraph;
import afc.graphing.r.RPlotGraph;

public class Incob2012 {
	/*
	 * To generate the graphs of expected samples size at varying theta for different delta values
	 */
	
	@Test
	public void test(){
		run(true,  "YounesA");
		run(false, "YounesB");
	}
	
	
	private void run(boolean isA, String filename) {
		final int repeats          = 100;                   // Number of repeats to run
		final double thetaStart    = 0.0;                   // Start point
		final double thetaEnd      = 1.0;                   // End point
		final double thetaInterval = 0.01;                  // Interval
		final double alpha         = 0.01;                  // Type I
		final double beta          = alpha;                 // Type II
		final double gamma         = Math.min(alpha, beta); // Controls 'I don't know'  
		final double delta[]       = {0.1, 0.05, 0.02};     // Indifference region
		final double trueThreshold = 0.7;                   // Real P
		
		List<Double> thetaList = new ArrayList<Double>();
		for (BigDecimal theta = new BigDecimal(thetaStart + ""); 
				theta.doubleValue() <= thetaEnd; 
				theta = theta.add (new BigDecimal(thetaInterval + ""))) {
			thetaList.add(theta.doubleValue());
		}
		
		List<int[]> samplesUsedAtDifferentTheta = new ArrayList<int[]>();
		for (double theta:thetaList) {
			System.out.println("Theta: " + theta);
			int[] totalSamples = new int[3];
			for (int i = 0; i < repeats; i++) {
				Random rand             = new Random(i);
				boolean[] anotherSample = {true, true, true};
				int[] samples           = {0, 0, 0};
				Algorithm algorithm[]   = new Algorithm[3];
				
				if(isA){
					algorithm[0] = new YounesA("", Algorithm.Operator.GREATER, theta, delta[0], alpha, beta, 0);
					algorithm[1] = new YounesA("", Algorithm.Operator.GREATER, theta, delta[1], alpha, beta, 0);
					algorithm[2] = new YounesA("", Algorithm.Operator.GREATER, theta, delta[2], alpha, beta, 0);
				}else{
					algorithm[0] = new YounesB("", Algorithm.Operator.GREATER, theta, delta[0], alpha, beta, gamma, 0);
					algorithm[1] = new YounesB("", Algorithm.Operator.GREATER, theta, delta[1], alpha, beta, gamma, 0);
					algorithm[2] = new YounesB("", Algorithm.Operator.GREATER, theta, delta[2], alpha, beta, gamma, 0);
				}
				
				/*
				 * Simulation
				 */
				boolean continueToLoop = true; 
				while (continueToLoop) {
					continueToLoop = false;
					boolean isTrue = false;
					
					// Sample true/false
					if(rand.nextDouble() < trueThreshold) isTrue = true; 
					
					for (int s = 0; s < anotherSample.length; s++) {
						if (anotherSample[s]) {
							samples[s]++;
							algorithm[s].update(isTrue, RuleStatus.APNOTSATISFIED);
							anotherSample[s] = algorithm[s].obtainAnotherSample();	
							if (anotherSample[s]) continueToLoop = true;
						}
					}
				}//End of while loop
				
				for(int s = 0; s < samples.length; s++){
					totalSamples[s] += samples[s];//Sum them
				}
			}//End of for loop
			for(int s = 0; s < totalSamples.length; s++){
				totalSamples[s] /= repeats;//Compute mean
			}
			samplesUsedAtDifferentTheta.add(totalSamples);
		}
		
		/*
		 * Graph Results
		 */
		List<RGraph> graphList    = new ArrayList<RGraph>();
		List<RGraph> graphLogList = new ArrayList<RGraph>();
		for (int i = 0; i < delta.length; i++) {
			
			List<Double> yList    = new ArrayList<Double>();
			List<Double> yLogList = new ArrayList<Double>();
			
			for (int j = 0; j < thetaList.size(); j++) {
				yLogList.add( Math.log(samplesUsedAtDifferentTheta.get(j)[i]) / Math.log(2) );
				yList.add( (double) samplesUsedAtDifferentTheta.get(j)[i] );
			}
			
			RGraph graph = RGraph.init(yList, thetaList).legendTitle(delta[i]+"").xLabel("expression(theta)").yLabel("Expected Sample Size").build();
			RGraph logGraph = RGraph.init(yLogList, thetaList).legendTitle(delta[i]+"").xLabel("expression(theta)").yLabel("Log Expected Sample Size").build();
			graphList.add(graph);
			graphLogList.add(logGraph);
		}
		
		final String fileOutputLocation = "./graphs/Incob2012/";
		StringBuffer s = RPlotGraph.init(graphList, fileOutputLocation + filename + ".pdf").build().draw();
		R r = new R();
		r.runCode(s, false);
		
		s = RPlotGraph.init(graphLogList, fileOutputLocation + filename + "Log.pdf").build().draw();
		r.runCode(s, false);
	}
}














