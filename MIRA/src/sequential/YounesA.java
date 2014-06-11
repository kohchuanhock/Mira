package sequential;


public class YounesA extends Algorithm{
	private double delta;//Half-width or half indifference Region
	
	public YounesA(Operator operator, double theta, double delta, double alpha, double beta, int maxSamples) {
		this("", operator, theta, delta, alpha, beta, maxSamples);
	}
	
	public YounesA(String syntax, Operator operator, double theta, double delta, double alpha, double beta, int maxSamples) {
		super(syntax, operator, theta, alpha, beta, maxSamples);
		this.delta = delta;
		this.adjustAndCheck(-1);
	}

	@Override
	protected boolean obtainAnotherSampleSpecific() {
		/* 
		 * Younes Algorithm A
		 * Given indifference region && Without Undecided Results
		 * Note: gamma should be -1
		 */				
		double leftHandSide = this.computeLeftHandSide();
		double sharedRightHand = this.computeSharedRightHandSide();
		
		if(this.acceptH0ForYounesA(leftHandSide, sharedRightHand) || this.acceptH1ForYounesA(leftHandSide, sharedRightHand)) return false;
		else return true;	
	}
	
	@Override
	public Conclusion getConclusion() {
		double leftHandSide = this.computeLeftHandSide();
		double sharedRightHand = this.computeSharedRightHandSide();
		
		if(this.acceptH1ForYounesA(leftHandSide, sharedRightHand)) return Conclusion.H1;
		if(this.acceptH0ForYounesA(leftHandSide, sharedRightHand)) return Conclusion.H0;
		//Max Sample Size Reached
		if(this.maxSamples == this.totalSamples) return Conclusion.MAXSAMPLESIZE;
		else{
//			return Conclusion.UNDECIDED;
			throw new Error("Unhandled Conclusion: " + this.maxSamples + "\t" + this.totalSamples);
		}
	}
	
	private double computeLeftHandSide(){
		//return this.trueSamples * Math.log(this.theta - this.delta) + (this.totalSamples - this.trueSamples) * Math.log(1 - this.theta + this.delta);
		return this.leftHandValue(false, this.delta);
	}
	
	private double computeSharedRightHandSide(){
		double a = this.theta + this.delta;
		if(a > 1.0) a = 1.0;
		double b = 1 - this.theta - this.delta;
		if(b < 0.0) b = 0.0;
		return this.trueSamples * Math.log(a) + (this.totalSamples - this.trueSamples) * Math.log(b);
	}
	
	private boolean acceptH1ForYounesA(double leftHandSide, double sharedRightHand){		
		//This method is for Younes A
		double h1Constant = Math.log(1 - this.beta) - Math.log(this.alpha) + sharedRightHand;
		return leftHandSide >= h1Constant;
	}
	
	private boolean acceptH0ForYounesA(double leftHandSide, double sharedRightHand){
		//This method is for Younes A
		double h0Constant = Math.log(this.beta) - Math.log(1 - this.alpha) + sharedRightHand;
		return leftHandSide <= h0Constant;
	}
}
