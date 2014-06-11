package sequential;


public class YounesB extends Algorithm {
	private double delta;
	private double gamma;
	
	public YounesB(String syntax, Operator operator, double theta, double delta, double alpha, double beta, double gamma, int maxSamples){
		/*
		 * With undecided results	 
		 */
		super(syntax, operator, theta, alpha, beta,maxSamples);
		
		this.delta = delta;
		this.gamma = gamma;
		this.adjustAndCheck(this.gamma);
	}

	@Override
	protected boolean obtainAnotherSampleSpecific() {
		/*
		 * Younes Algorithm B
		 * Given indifference Region && With Undecided Results
		 * Note: this.dynamicDelta is false
		 */
		double sharedConstant = this.trueSamples * Math.log(this.theta) + (this.totalSamples - this.trueSamples) * Math.log(1 - this.theta);
		if((this.acceptH0(sharedConstant, this.delta, this.gamma) && this.acceptH0prime(sharedConstant, this.delta, this.gamma)) || 
				(this.acceptH1(sharedConstant, this.delta, this.gamma) && this.acceptH1prime(sharedConstant, this.delta, this.gamma))){
			return false;//decided results
		}else if((this.acceptH0(sharedConstant, this.delta, this.gamma) || this.acceptH1(sharedConstant, this.delta, this.gamma)) && 
				(this.acceptH0prime(sharedConstant, this.delta, this.gamma) || this.acceptH1prime(sharedConstant, this.delta, this.gamma))){
			return false;//undecided results
		}else{
			return true;//continue sampling
		}
	}

	@Override
	public Conclusion getConclusion() {
		double sharedConstant = this.trueSamples * Math.log(this.theta) + (this.totalSamples - this.trueSamples) * Math.log(1 - this.theta);
		if(this.acceptH0(sharedConstant, this.delta, this.gamma) && this.acceptH0prime(sharedConstant, this.delta, this.gamma)) return Conclusion.H0;
		if(this.acceptH1(sharedConstant, this.delta, this.gamma) && this.acceptH1prime(sharedConstant, this.delta, this.gamma)) return Conclusion.H1;
		if(this.maxSamples == this.totalSamples) return Conclusion.MAXSAMPLESIZE;
		return Conclusion.UNDECIDED;//Undecided
	}	
}
