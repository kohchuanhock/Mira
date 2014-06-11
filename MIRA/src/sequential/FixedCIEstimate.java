package sequential;

import org.apache.commons.math.distribution.NormalDistributionImpl;

public class FixedCIEstimate extends Algorithm{
	private double ciHalfWidth;
	
	public FixedCIEstimate(String syntax, double confidence, double cihalfWidth, int maxSamples){		
		/*
		 * This is for =? for fixed CI Half Width
		 * Note that delta is used as CI Half Width here
		 */		
		super(syntax, confidence, maxSamples);
		if(cihalfWidth <= 0.0) throw new Error(syntax + "\r\nCI in P=?(confidence, CI, max) cannot be <= 0");		
		if(cihalfWidth >= 1.0) throw new Error(syntax + "\r\nCI in P=?(confidence, CI, max) cannot be >= 1");
		this.ciHalfWidth = cihalfWidth;
	}
	
	@Override
	protected boolean obtainAnotherSampleSpecific(){
		//Delta is used as CI Half Width
		try{
			NormalDistributionImpl normal = new NormalDistributionImpl();								
			double z = normal.inverseCumulativeProbability(this.alpha/2.0) * -1;
			double mathPowerZ2 = Math.pow(z, 2);
			return !(this.computeCIHalfWidth(z, mathPowerZ2) <= this.ciHalfWidth);
		}catch(Exception e){e.printStackTrace(); throw new Error();}
	}
	
	private double computeCIHalfWidth(double z, double mathPowerZ2){
		/*
		 * Compute the Confidence Interval
		 * Note: Used the Wilson Score Interval (Google it for more information)
		 */
		try{					
			double p = (this.trueSamples + 0.0) / this.totalSamples;
			double innerLeftHand = (p * (1 - p) / this.totalSamples);
			double innerRightHand = mathPowerZ2 / (4 * this.totalSamples * this.totalSamples);
			double rightHandTop = z * Math.sqrt(innerLeftHand + innerRightHand);
			double rightHandBottom = (1 + (mathPowerZ2 / this.totalSamples));
			return rightHandTop / rightHandBottom;
		}catch(Exception e){e.printStackTrace(); throw new Error("Problem with Computing CI Half Width");}
	}
	
	@Override
	public Conclusion getConclusion() {
		throw new Error("Should not call getConclusion for FixedCIEstimate!");
	}
}
