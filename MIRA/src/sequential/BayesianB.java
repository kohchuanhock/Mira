package sequential;


public class BayesianB extends Algorithm{
	private double threshold;
	
	public BayesianB(String syntax, Operator operator, double theta, double alpha, double beta, 
			double threshold, int maxSamples){
		super(syntax, operator, theta, alpha, beta, maxSamples);
		
		this.threshold = threshold;
	}
	
	private double computeVariance(){
		return ((this.alpha + this.trueSamples) * (this.totalSamples - this.trueSamples + this.beta)) /
		(Math.pow(this.alpha + this.totalSamples + this.beta, 2.0) * (this.alpha + this.totalSamples + this.beta + 1));
	}
	
	@Override
	protected boolean obtainAnotherSampleSpecific(){
		double variance = this.computeVariance();
		if(variance < this.threshold) return false;
		else return true;//continue sampling
	}

	@Override
	public Conclusion getConclusion() {
		if(this.totalSamples == 0) return null;
		double variance = this.computeVariance();
		if(variance < this.threshold){
			//Estimate
			double p = (this.trueSamples + this.alpha) / (this.alpha + this.beta + this.totalSamples);
			if(p >= this.theta) return Conclusion.H0;
			else return Conclusion.H1;
		}else if(this.totalSamples == this.maxSamples){
			return Conclusion.MAXSAMPLESIZE;
		}else throw new Error("Unhandled Conclusion");
	}
}
