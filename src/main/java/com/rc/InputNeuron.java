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

	public InputNeuron( Genome genome, int id ) {
		super( genome, id ) ;
		clockOffset = rng.nextDouble() ;
		nextSpikeTime = 0 ;
	}
	
	@Override
	public void step( double potential, double clock ) {
		this.potential = potential ;
		if( nextSpikeTime <= clock ) {
			spike( clock ) ;
			this.currentPotential = 1.0 ;
			double clockDrift = clock - nextSpikeTime ;
					
			// 	  potential => 1kHz
			// 	+ clock starting from now
			// 	+ ( clock - nextSpikeTime ) to keep track of fractions
			
			nextSpikeTime = (1.0 - potential) / 200 + clock ; 
			nextSpikeTime -= clockDrift ;
		} else {
			this.currentPotential = 0 ;
		}
	}

	
//	@Override
//	public double potential( double clock ) {
//		return potential / 1000.0  ;
//	}
}


