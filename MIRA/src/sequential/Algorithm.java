/*
============================================================================
	                     MIRA - Statistical Online Model Checker
	                   http://sourceforge.net/projects/mira/
============================================================================
	  Copyright (C) 2010 by Chuan Hock Koh
	
	  This program is free software; you can redistribute it and/or
	  modify it under the terms of the GNU Lesser General Public
	  License as published by the Free Software Foundation; either
	  version 3 of the License, or (at your option) any later version.
	
	  This program is distributed in the hope that it will be useful,
	  but WITHOUT ANY WARRANTY; without even the implied warranty of
	  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
	  Lesser General Public License for more details.
	  	
	  You should have received a copy of the GNU General Public License
	  along with this program.  If not, see <http://www.gnu.org/licenses/>.
===========================================================================
*/
package sequential;

import org.apache.commons.math.distribution.NormalDistributionImpl;

/*
 * This class implements the statistical hypothesis testing and statistical estimation
 */
public abstract class Algorithm {
	
	public static enum RuleStatus {
		TRUE, FALSE, UNKNOWN, APNOTSATISFIED
	}
	
	public static enum Operator {
		GREATER_EQUAL, GREATER, LESSER_EQUAL, LESSER, QUESTION, NOT_P
	}
	
	public static enum Conclusion {
		//H0: p > theta, 
		//H1: p < theta
		H0, H1, UNDECIDED, MAXSAMPLESIZE, pValueH0, pValueH1
	}
	
	private RuleStatus lastStatus = null;//Latest status of the Rule
	public RuleStatus getLastStatus(){return this.lastStatus;}
	
	private boolean skipRule = false;
	protected Operator operator;    //Operator of the Rule
	private String syntax;          //Syntax of the Rule
	protected int totalSamples = 0; //Total number of samples so far
	protected int trueSamples = 0;  //Total number of true samples so far
	protected int maxSamples;       //Maximum number of samples to run
	protected double theta;         //Threshold probability 
	protected double alpha;         //Type1 error threshold (False-Positive) for Younes and Mirach or as Priors for Bayesian
	protected double beta;          //Type2 error threshold (False-Negative) for Younes and Mirach or as Priors for Bayesian
	
	//This is used to ensure that v will be >0 and < 1, Math.ln(v)
	private final double EPSILON = 0.00000001;
	
	private double confidence;
	
	public String getSyntax() { return this.syntax; }
	public int getTotalSamples() { return this.totalSamples; }
	public int getTrueSamples() { return this.trueSamples; }
	public double getAlpha() { return this.alpha; }
	public double getTheta() { return this.theta; }
	public boolean getSkipRule() { return this.skipRule; }
	public Operator getOperator() { return this.operator; }
	
	protected abstract boolean obtainAnotherSampleSpecific();
	public abstract Conclusion getConclusion();
	
	public Algorithm(String syntax, Operator operator, double theta, double alpha, double beta, int maxSamples) {
		//This constructor is for Younes A, Younes B, Mirach, Bayesian A and Bayesian B
		this.syntax = syntax;
		this.operator = operator;
		this.theta = theta;
		this.alpha = alpha;
		this.beta = beta;
		this.maxSamples = maxSamples;
	}
	
	public Algorithm(String syntax, double confidence, int maxSamples) {
		//This constructor is for Estimate (Fixed CI and Fixed Samples)
		this.syntax = syntax;
		this.operator = Operator.QUESTION;
		this.maxSamples = maxSamples;
		this.confidence = confidence;
		if(this.confidence <= 0 || this.confidence >= 1) throw new Error("Confidence should not be <= 0 or >= 1");
	}
	
	protected void adjustAndCheck(double gamma) {
		/*
		 * Ensure that the settings are of proper range
		 */
		if (this.isAStatisticalRule()) {
			if (this.alpha <= 0 || this.alpha >= 1 || this.beta <= 0 || this.beta >= 1 || 
					((gamma <= 0 || gamma >= 1) && gamma != -1)) {
				System.err.println("alpha: " + this.alpha);
				System.err.println("beta: " + this.beta);
				System.err.println("gamma: " + gamma);
				throw new Error("alpha, beta and gamma MUST be greater than 0 and lesser than 1.");
			}	
		}
		if(this.theta > 1.0 || this.theta < 0.0) {
			throw new Error("theta MUST be greater than or equal to 0 and lesser than or equal to 1.");
		}
		if(this.theta < this.EPSILON) {
			//Cannot allow theta to be smaller than epsilon by too much
			this.theta = this.EPSILON / 2;
		}
		/* EPSILON allows these constraints to be lifted
		 * if(this.theta - this.delta >= 1 || this.theta - this.delta <= 0){
			throw new Error("this.theta - this.delta >= 1 or <= 0 is not allowed. Please adjust their values. " +
					"Theta = " + this.theta + " Delta = " + this.delta);
		}
		if(this.theta + this.delta >= 1 || this.theta - this.delta <= 0){
			throw new Error("this.theta + this.delta >= 1 or <= 0 is not allowed. Please adjust their values. " +
					"Theta = " + this.theta + " Delta = " + this.delta);
		}*/		
	}
	
	public RuleStatus getConclusionInRuleStatus() {
		//H0, H1, UNDECIDED, MAXSAMPLESIZE, pValueH0, pValueH1
		//GREATER_EQUAL, GREATER, LESSER_EQUAL, LESSER, QUESTION, NOT_P
		//H0: p > theta, 
		//H1: p < theta
		Conclusion c = getConclusion();
		switch(c){
		case H0: case pValueH0: 
			if(this.operator == Operator.GREATER || this.operator == Operator.GREATER_EQUAL){
				return RuleStatus.TRUE;
			}else{
				return RuleStatus.FALSE;
			}
		case H1: case pValueH1:
			if(this.operator == Operator.LESSER || this.operator == Operator.LESSER_EQUAL){
				return RuleStatus.TRUE;
			}else{
				return RuleStatus.FALSE;
			}
		case UNDECIDED: case MAXSAMPLESIZE: return RuleStatus.UNKNOWN;
		default: throw new Error("Unhandled case: " + c);
		}
	}
	
	public void skipRule(){
		this.skipRule = true;
	}
	
	public void update(boolean isTrue, RuleStatus status){
		/*
		 * Set the status of the rule
		 * Increment the total samples
		 * Increment the total true samples if isTrue
		 */
		this.lastStatus = status;		
		this.totalSamples++;
		if(isTrue) this.trueSamples++;		
	}
	
	public void update(boolean isTrue){
		this.update(isTrue, RuleStatus.APNOTSATISFIED);
	}
	
	protected double leftHandValue(boolean isPrime, double delta){
		/*
		 * returns leftHandSide value
		 * H0 && H1 = [trueSamples * Math.ln(theta - delta)] + [falseSamples * Math.ln(1 - theta + delta)]
		 * H0prime && H1prime = [trueSamples * Math.ln(theta + delta)] + [falseSamples * Math.ln(1 - theta - delta)]
		 */		
		int falseSamples = this.totalSamples - this.trueSamples;
		if(delta < 0){
			//delta = this.EPSILON;
			throw new Error("Does not make sense for delta to be < 0");			
		}
		Double peak_1 = 0.0;
		if (this.trueSamples > 0) {
			double v;
			if (isPrime) v = this.theta + delta;
			else v = this.theta - delta;
			//if(v <= 0) v = this.EPSILON;
			//else if(v >= 1) v = 1 - this.EPSILON;	
			if(v < 0) v = 0;
			else if(v > 1) v = 1;
			peak_1 = this.trueSamples * Math.log(v);			
		}
		Double peak_2 = 0.0;
		if(falseSamples > 0){
			double v;
			if(isPrime) v = 1 - this.theta - delta;				
			else v = 1 - this.theta + delta;				
			//if(v <= 0) v = this.EPSILON;
			//else if(v >= 1) v = 1 - this.EPSILON;	
			if(v < 0) v = 0;
			else if(v > 1) v = 1;
			peak_2 = falseSamples * Math.log(v);			
		}				
		return peak_1 + peak_2;		
	}
	
	public boolean obtainAnotherSample(){
		/*
		 * Is another sample needed to make a decision?
		 */
		//Skip rule? Maybe by user request or due to unknown id/name in rule
		if(this.skipRule) return false;
		//Of course have to at least obtain one sample		
		if(this.totalSamples == 0) return true;
		//Max samples limit reached
		if(this.maxSamples > 0 && this.totalSamples >= this.maxSamples) return false;
		//Run the specific algorithm for whether to continue
		return this.obtainAnotherSampleSpecific();		
	}
	
	protected boolean acceptH1(double sharedConstant, double delta, double gamma){
		//This method is for Younes B and MIRACH
		double leftHandValue = this.leftHandValue(false, delta);				
		double h1Constant = Math.log(1 - gamma) - Math.log(this.alpha) + sharedConstant;		
		return leftHandValue >= h1Constant;
	}

	protected boolean acceptH0(double sharedConstant, double delta, double gamma){
		//This method is for Younes B and MIRACH
		double leftHandValue = this.leftHandValue(false, delta);		
		double h0Constant = Math.log(gamma) - Math.log(1 - this.alpha) + sharedConstant;		
		return leftHandValue <= h0Constant;
	}
	
	protected boolean acceptH1prime(double sharedConstant, double delta, double gamma){
		//This method is for Younes B and MIRACH
		double leftHandValue = this.leftHandValue(true, delta);
		double h1PrimeConstant = Math.log(gamma) - Math.log(1 - this.beta) + sharedConstant;		
		return leftHandValue <= h1PrimeConstant;
	}
	
	protected boolean acceptH0prime(double sharedConstant, double delta, double gamma){
		//This method is for Younes B and MIRACH
		double leftHandValue = this.leftHandValue(true, delta);
		double h0PrimeConstant = Math.log(1 - gamma) - Math.log(this.beta) + sharedConstant;		
		return leftHandValue >= h0PrimeConstant;
	}
	
	private boolean isAStatisticalRule(){
		/*
		 * Returns whether the rule is a statistical/probabilistic or deterministic rule
		 * TRUE if it is a statistical/probabilistic rule
		 * FALSE if it is a deterministic rule
		 */
		return (this.operator != Operator.NOT_P);
	}
	
	
	
	public boolean isThetaWithinP(double confidence){
		double[] wilsonInterval = this.computeWilsonInterval(confidence);
		if(this.theta >= wilsonInterval[0] - wilsonInterval[1] && this.theta <= wilsonInterval[0] + wilsonInterval[1]){
			return true;
		}else{
			return false;
		}
	}
	
	public double[] computeWilsonInterval(){
		return computeWilsonInterval(this.confidence);
	}
	
	private double[] computeWilsonInterval(double confidence){
		/*
		 * Compute the Wilson confidence interval
		 */
		try{
			double[] wilsonInterval = new double[2];
			NormalDistributionImpl normal = new NormalDistributionImpl();								
			double z = normal.inverseCumulativeProbability(confidence/2.0) * -1;
			double mathPowerZ2 = Math.pow(z, 2);
			wilsonInterval[0] = this.computeMidPoint(z, mathPowerZ2);
			wilsonInterval[1] = this.computeCI(z, mathPowerZ2);
			return wilsonInterval;
		}catch(Exception e){e.printStackTrace(); throw new Error();}
	}
	
	private double computeMidPoint(double z, double mathPowerZ2){		
		double p = (this.trueSamples + 0.0) / this.totalSamples;				
		return (p +  mathPowerZ2 / (2 * this.totalSamples) ) / (1 + mathPowerZ2 / this.totalSamples);
	}
	
	private double computeCI(double z, double mathPowerZ2){
		try{					
			double p = (this.trueSamples + 0.0) / this.totalSamples;
			double innerLeftHand = (p * (1 - p) / this.totalSamples);
			double innerRightHand = mathPowerZ2 / (4 * this.totalSamples * this.totalSamples);
			double rightHandTop = z * Math.sqrt(innerLeftHand + innerRightHand);
			double rightHandBottom = (1 + (mathPowerZ2 / this.totalSamples));
			return rightHandTop / rightHandBottom;
		}catch(Exception e){e.printStackTrace(); return 199;}
	}
}

