package sequential;

import org.apache.commons.math.distribution.BetaDistributionImpl;

/*
 * A Bayesian Approach to Model Checking Biological Systems CMSB2009
 * Sumit K Jha et al
 * 
 * Hypothesis testing based Bayesian statistical model checking
 */
public class BayesianA extends Algorithm{
	private double bayesFactorThreshold;

	public BayesianA(String syntax, Operator operator, double theta, double alpha, double beta, double bayesFactorThreshold, int maxSamples){
		super(syntax, operator, theta, alpha, beta, maxSamples);
		this.bayesFactorThreshold = bayesFactorThreshold;
	}
	
	private double computeBayesFactor(){
		try{
			BetaDistributionImpl beta = new BetaDistributionImpl(this.trueSamples + this.alpha, this.totalSamples - this.trueSamples + this.beta);
			double f = beta.cumulativeProbability(this.theta);
			return (1 / f) - 1;
		}catch(Exception e){e.printStackTrace(); throw new Error("Error in computing BayesFactor");}
	}

	@Override
	protected boolean obtainAnotherSampleSpecific() {
		double bayesFactor = this.computeBayesFactor();
		if(bayesFactor > this.bayesFactorThreshold || bayesFactor < (1 / this.bayesFactorThreshold) ){
			return false;
		}
		return true;
	}

	@Override
	public Conclusion getConclusion() {
		if(this.totalSamples == 0) return null;
		double bayesFactor = this.computeBayesFactor();
		if(bayesFactor > this.bayesFactorThreshold) return Conclusion.H0;
		else if(bayesFactor < (1 / this.bayesFactorThreshold)) return Conclusion.H1;
		else if(this.maxSamples == this.totalSamples) return Conclusion.MAXSAMPLESIZE;
		else{
			throw new Error("Unhandled Conclusion");
		}
	}

}
