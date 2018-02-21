package com.rc ;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class InputNeuron extends Neuron {

	final static Logger log = LoggerFactory.getLogger( InputNeuron.class ) ;
	final static Random rng = new Random() ;
	
	final double clockOffset ;
	double nextSpikeTime ;
	double potential ;
	
	public InputNeuron( int id ) {		
		super( id ) ;
		clockOffset = rng.nextDouble() ;
		nextSpikeTime = 0 ;
	}

	public InputNeuron( Genome genome ) {
		super( genome ) ;
		clockOffset = rng.nextDouble() ;
		nextSpikeTime = 0 ;
	}
	
	@Override
	public void setPotential( double potential, double clock ) {
		this.potential = potential ;
		if( nextSpikeTime <= clock ) {
			spike( clock ) ;
			double clockDrift = clock - nextSpikeTime ;
					
			// 	  potential => 1kHz
			// 	+ clock starting from now
			// 	+ ( clock - nextSpikeTime ) to keep track of fractions
			
			nextSpikeTime = (1.2-potential) / 1000 + clock ; //- clockDrift ;
		} else {
			this.rest( clock ) ;
		}
	}

//	@Override
//	public double potential( double clock ) {
//		return potential / 1000.0  ;
//	}
}


