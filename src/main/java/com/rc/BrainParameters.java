package com.rc;

import java.util.BitSet;
import java.util.StringJoiner;

public class BrainParameters  {
	
	public double inhibitorRatio = 0.25 ;
	public double spikeThreshold = 0.70 ;
	public double restingPotential = -.1 ;

	public double spikeProfile [] = { 1.00, .70, .30, -.25, -.20, -.15  } ;
	public int numInputs ;
	public int numOutputs ;
	public int dimensions [] = { 3, 3 } ;	
	
	final public static int SPIKE_PROFILE_SIZE = 6 ; 
	final public static int BITS_PER_NUMBER = 6 ; 
	final public static double NUMBER_GRANULARITY = 1 << BITS_PER_NUMBER ;
	final public static int GENOME_SIZE = BITS_PER_NUMBER * (3+SPIKE_PROFILE_SIZE) ;

	public BrainParameters() {}
	
	public BrainParameters( BitSet genome ) {
			
		int bitIndex = SPIKE_PROFILE_SIZE * BITS_PER_NUMBER ;
		inhibitorRatio     = getValue( genome,  bitIndex++, BITS_PER_NUMBER ) / NUMBER_GRANULARITY ; 
		spikeThreshold     = ( getValue( genome,  bitIndex++, BITS_PER_NUMBER ) / (0.5*NUMBER_GRANULARITY) ) - 1.0  ; 
		restingPotential   = ( getValue( genome,  bitIndex++, BITS_PER_NUMBER ) / NUMBER_GRANULARITY ) - 0.5 ; 
	/*	
		spikeProfile = new double[SPIKE_PROFILE_SIZE+1] ;
		spikeProfile[0] = restingPotential ; //		
		for( int i=1 ; i<spikeProfile.length ; i++ ) {
			 spikeProfile[i] = spikeProfile[i-1] + 
					( getValue( genome, BITS_PER_NUMBER*(i-1), BITS_PER_NUMBER ) / 
					(2*NUMBER_GRANULARITY) ) - 0.25 ;
			 if( spikeProfile[i]> 1.0 ) spikeProfile[i] = 1.0 ; 
			 if( spikeProfile[i]< -1.0 ) spikeProfile[i] = -1.0 ; 
			}
			*/
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
		StringJoiner sj = new StringJoiner(" ") ;
		for( double d : spikeProfile ) sj.add( String.valueOf(d) ) ;

		sb
		.append( "\ninhibitorRatio\t\t" ).append( inhibitorRatio )
		.append( "\nspikeThreshold\t\t" ). append( spikeThreshold )
		.append( "\nrestingPotential\t" ).append( restingPotential )
		.append( "\nspikeProfile\t\t" ).append( sj )
		.append( "\nnumInputs\t\t" ).append( numInputs )
		.append( "\nnumOutputs\t\t" ).append( numOutputs )
		;
		
		return sb.toString() ;		
	}
}
