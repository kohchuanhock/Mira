package sequential;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math.special.Gamma;

public class Mira extends Algorithm{
	private int minSamples;
	//Alpha - Type1 error threshold (False-Positive)
	//Beta - Type2 error threshold (False-Negative)
	private double delta;//Dynamic Indifference Region
	private double gamma;
	//H0: p > theta - True probability greater than theta 
	//H1: p < theta - True probability lesser than theta
	private Double h0pValue = -1.0;
	private Double h1pValue = -1.0;
	
	public double getLowestPValue(){
		double h0 = this.getH0pValue();
		double h1 = this.getH1pValue();
		if(h0 < h1) return h0;
		else return h1;
	}
	
	public double getH0pValue(){
		if(this.h0pValue == -1.0){
			this.computePValues();
		}
		return this.h0pValue;
	}
	
	public double getH1pValue(){
		if(this.h1pValue == -1.0){
			this.computePValues();
		}
		return this.h1pValue;
	}
	
	public Mira(double theta, double alpha, double beta, int maxSamples){
		this("", Operator.GREATER, theta, alpha, beta, maxSamples);
	}
	
	public Mira(String syntax, Operator operator, double theta, double alpha, double beta, int maxSamples){
		super(syntax, operator, theta, alpha, beta, maxSamples);
		if(minSamples > maxSamples && maxSamples != 0) throw new Error("Min Samples cannot be greater than Max Samples");
		this.delta = 1.0;
		//this.minSamples = minSamples;
		//this.minSamples = (int)Math.max(Math.log(alpha) / Math.log(1 - theta), Math.log(beta) / Math.log(theta)) + 1;
		//Proven Mathematically that Gamma needs to be less than or equal to alpha and beta to ensure error rate of alpha and beta
		this.gamma = Math.min(alpha, beta);
		this.adjustAndCheck(this.gamma);
	}

	@Override
	protected boolean obtainAnotherSampleSpecific() {
		/*
		 * My proposed algorithm
		 * No indifference region given && Without Undecided Results
		 * Note: this.dynamicDelta is true
		 */					
		if(this.minSamples != 0 && this.totalSamples < this.minSamples) return true;
		/*if(this.isThetaWithinP(1 - Math.min(this.alpha, this.beta))){
			//Continue sampling if theta is within P
			return true;
		}*/
		if(this.theta == 1.0){
			if(this.totalSamples - this.trueSamples > 0) return false;
			return true;
		}
		
		double sharedConstant = this.computeSharedConstant();		
		if((this.acceptH0(sharedConstant, this.delta, this.gamma) && this.acceptH0prime(sharedConstant, this.delta, this.gamma)) || 
				(this.acceptH1(sharedConstant, this.delta, this.gamma) && this.acceptH1prime(sharedConstant, this.delta, this.gamma))){
			return false;
		}else{
			/*
			 * This condition imply that "true" value likely to be inside the indifference region					 
			 */
			if((this.acceptH0(sharedConstant, this.delta, this.gamma) || this.acceptH1(sharedConstant, this.delta, this.gamma)) && 
					(this.acceptH0prime(sharedConstant, this.delta, this.gamma) || this.acceptH1prime(sharedConstant, this.delta, this.gamma))){
				/*
				 * Reduce delta
				 * Current reduce strategy is to divide by half
				 */
				this.delta = this.delta * 0.5;
				return obtainAnotherSample();
			}else{
				return true;//continue sampling
			}
		}
	}
	
	private double computeSharedConstant(){
		if(this.theta == 1.0){
			return this.trueSamples * Math.log(this.theta) + (this.totalSamples - this.trueSamples) ;
		}else{
			return this.trueSamples * Math.log(this.theta) + (this.totalSamples - this.trueSamples) * Math.log(1 - this.theta);
		}
	}
	
	public boolean concludeByOSMA(){
		if(this.obtainAnotherSample() == true) throw new Error();
		Conclusion c = this.getConclusion();
		if(c.equals(Conclusion.H0) || c.equals(Conclusion.H1)) return true;
		else if(c.equals(Conclusion.pValueH0) || c.equals(Conclusion.pValueH1)) return false;
		else throw new Error();
	}

	@Override
	public Conclusion getConclusion() {
		/*
		 * H0: p > theta - True probability greater than theta
		 * H1: p < theta - True probability lesser than theta 
		 */
		double sharedConstant = this.computeSharedConstant();
		if(this.theta == 1.0){
			if(this.totalSamples - this.trueSamples > 0) return Conclusion.H1;
			return decideByPValue();
		}else{
			if(this.acceptH0(sharedConstant, this.delta, this.gamma) && this.acceptH0prime(sharedConstant, this.delta, this.gamma)) return Conclusion.H0;
			if(this.acceptH1(sharedConstant, this.delta, this.gamma) && this.acceptH1prime(sharedConstant, this.delta, this.gamma)) return Conclusion.H1;
			return decideByPValue();
		}
	}
	
	private void computePValues(){
		List<Double> rArrayList = this.binocdf(this.trueSamples, this.totalSamples, this.theta);
		Double pValueH0 = 1.0 - rArrayList.get(0);						
		Double pValueH1;
		if(rArrayList.size() > 1){
			pValueH1 = rArrayList.get(1);
			this.h0pValue = pValueH0;
			this.h1pValue = pValueH1;
		}else{
			throw new Error("Unable to properly compute PValue");
		}
	}
	
	private Conclusion decideByPValue(){
		//Decide by PValue
		this.computePValues();
		if(this.h1pValue.compareTo(this.h0pValue) < 0) return Conclusion.pValueH1;
		else if(this.h1pValue.compareTo(this.h0pValue) > 0) return Conclusion.pValueH0;
		else{
			System.err.println("Total Samples: " + this.totalSamples);
			System.err.println("True Samples: " + this.trueSamples);
			System.err.println("h0pValue: " + this.h0pValue);
			System.err.println("h1pValue: " + this.h1pValue);
			throw new Error("Equal PValue - This is rare");
		}
	}
	
	/* 
	 * Binomial cumulative distribution function.
	 * Gotten the pseudo code from Dr Younes via personal communication 
	 */	
	private List<Double> binocdf(int c, int n, double theta) {
		if (c < 0 || c > n || n < 1 || theta < 0.0 || theta > 1.0) {
			throw new Error("c or n or theta");
		} else {
			List<Double> rArrayList = new ArrayList<Double>();
			double q = 1.0 - theta;
			double lp = Math.log(theta);
			double lq = Math.log(q);
			double y = Math.pow(q, n);
			for (int i = 1; i <= c; i++) {
				if(i == c){
					rArrayList.add(Math.min(1.0, y));
				}
				double lc = Gamma.logGamma(n + 1) - Gamma.logGamma(n - i + 1) - Gamma.logGamma(i + 1);
				y += Math.exp(lc + i*lp + (n - i)*lq);
			}
			rArrayList.add(Math.min(1.0, y));
			if(rArrayList.size() == 1){
				rArrayList.add(rArrayList.get(0).doubleValue());
			}
			return rArrayList;
		}
	}
	
	public static void main(String[] args){
		try{
			if(args.length != 4){
				throw new Error("Need four arguments.\n 1) theta (double) \n 2) alpha (double) \n 3) beta (double) " +
						"\n 4) textfile with each line being true or false (String)");
			}
			double theta = Double.parseDouble(args[0]);
			double alpha = Double.parseDouble(args[1]);
			double beta = Double.parseDouble(args[2]);
			File file = new File(args[3]);
			
			String line;
			List<Boolean> booleanList = new ArrayList<Boolean>();
			BufferedReader input = new BufferedReader(new FileReader(file));
			while((line = input.readLine()) != null){
				booleanList.add(Boolean.parseBoolean(line));
			}
			input.close();
			Mira mira = new Mira(theta, alpha, beta, booleanList.size());
			for(int i = 0; i < booleanList.size() && mira.obtainAnotherSample(); i++){
				mira.update(booleanList.get(i));
			}
			System.out.println("Parameters: theta = " + theta + ", alpha = " + alpha + ", beta = " + beta + ", inputFile = " + file.getAbsolutePath());
			System.out.println("Samples Provided: " + booleanList.size());
			System.out.println("Samples Used: " + mira.getTotalSamples());
			System.out.println("Conclusion: " + mira.getConclusion());
		}catch(Exception e){e.printStackTrace();}
	}
}
