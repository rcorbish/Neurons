package com.rc;

import java.util.BitSet;

public class BrainParameters  {
	
	public double connectivityFactor = 0.75 ;	
	public double inhibitorRatio = 0.25 ;
	public double spikeThreshold = 0.75 ;
	public double transmissionFactor = 0.85 ;
	public double restingPotential = 0 ;

	public double spikeProfile [] = { 1.00, .70, .30, .20, 0, -.10, -.08, -.08, -.05, -.05 ,-.03, -.02  } ;
	public int numInputs ;
	public int numOutputs ;
	public int dimensions [] = { 10, 10 } ;
	
	
	final public static int SPIKE_PROFILE_SIZE = 10 ; 
	final public static int BITS_PER_NUMBER = 6 ; 
	final public static double NUMBER_GRANULARITY = 1 << BITS_PER_NUMBER ;
	final public static int GENOME_SIZE = BITS_PER_NUMBER * (5+SPIKE_PROFILE_SIZE) ;

	public static BrainParameters fromBits( BitSet genome ) {
		BrainParameters bp = new BrainParameters() ;
		
		bp.connectivityFactor = getValue( genome,  BITS_PER_NUMBER*0, BITS_PER_NUMBER ) / NUMBER_GRANULARITY ; 
		bp.inhibitorRatio     = getValue( genome,  BITS_PER_NUMBER*1, BITS_PER_NUMBER ) / NUMBER_GRANULARITY ; 
		bp.spikeThreshold     = getValue( genome,  BITS_PER_NUMBER*2, BITS_PER_NUMBER ) / NUMBER_GRANULARITY ; 
		bp.transmissionFactor = getValue( genome,  BITS_PER_NUMBER*3, BITS_PER_NUMBER ) / NUMBER_GRANULARITY ; 
		bp.restingPotential   = ( getValue( genome,  BITS_PER_NUMBER*4, BITS_PER_NUMBER ) / NUMBER_GRANULARITY ) - 0.5 ; 
		
		bp.spikeProfile = new double[SPIKE_PROFILE_SIZE] ;
		for( int i=0 ; i<bp.spikeProfile.length ; i++ ) {
			 bp.spikeProfile[i] = getValue( genome,  BITS_PER_NUMBER*(i+5), BITS_PER_NUMBER ) / NUMBER_GRANULARITY ;
		}
		return bp ;
	}
	
	public static int getValue( BitSet bits, int start, int length ) {
		int rc = 0 ;
		for( int i=0 ; i<length ; i++ ) {
			rc += bits.get(start+i) ? 0 : 1<<i  ;
		}
		return rc ;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder() ;
		
		sb
		.append( "\nconnectivityFactor\t" ).append( connectivityFactor )	
		.append( "\ninhibitorRatio\t\t" ).append( inhibitorRatio )
		.append( "\nspikeThreshold\t\t" ). append( spikeThreshold )
		.append( "\ntransmissionFactor\t" ).append( transmissionFactor )
		.append( "\nrestingPotential\t" ).append( restingPotential )
		.append( "\nnumInputs\t\t" ).append( numInputs )
		.append( "\nnumOutputs\t\t" ).append( numOutputs )
		;
		
		return sb.toString() ;
		
	}
}
