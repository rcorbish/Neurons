package com.rc;

import java.io.Serializable;
import java.util.BitSet;

public class BrainParameters implements Serializable {

	public double connectivityFactor = 0.75 ;	
	public double inhibitorRatio = 0.25 ;
	public double spikeThreshold = 0.75 ;
	public double transmissionFactor = 0.85 ;
	public double restingPotential = 0 ;

	public double spikeProfile [] = { 1.00, .70, .30, .20, 0, -.10, -.08, -.08, -.05, -.05 ,-.03, -.02  } ;
	public int numInputs ;
	public int numOutputs ;
	public int dimensions [] = { 10, 10 } ;
	
	final public static int GENOME_SIZE = 26 ;
	public static BrainParameters fromBits( BitSet genome ) {
		BrainParameters bp = new BrainParameters() ;
		
		bp.connectivityFactor = 1.0 / ( 1 + getValue( genome,  0, 7 ) ) ; 
		bp.inhibitorRatio     = 1.0 / ( 1 + getValue( genome,  7, 4 ) ) ; 
		bp.spikeThreshold     = 1.0 / ( 1 + getValue( genome, 11, 5 ) ) ; 
		bp.transmissionFactor = 1.0 / ( 1 + getValue( genome, 16, 5 ) ) ; 
		bp.restingPotential   = 1.0 / ( 1 + getValue( genome, 21, 5 ) ) ; 
		
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
