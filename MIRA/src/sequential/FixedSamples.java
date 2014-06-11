package sequential;


public class FixedSamples extends Algorithm{
	
	public FixedSamples(String syntax, double confidence, int maxSamples){
		/*
		 * This is for =? with fixed number of samples
		 */
		super(syntax, confidence, maxSamples);
		if((int)maxSamples <= 0){
			throw new Error(syntax + "\r\nmax in P=?(confidence, max) cannot be <= 0 when CI is not given");
		}
	}
	
	@Override
	protected boolean obtainAnotherSampleSpecific(){
		return true;//continue sampling until max number of samples is reached
	}

	@Override
	public Conclusion getConclusion() {
		throw new Error("Should not call getConclusion for FixedSampleEstimate!");
	}
}
