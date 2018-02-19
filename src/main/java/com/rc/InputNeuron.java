package com.rc ;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class InputNeuron extends Neuron {

	final static Logger log = LoggerFactory.getLogger( InputNeuron.class ) ;
	final static Random rng = new Random() ;
	
	final double clockOffset ;
	double nextSpikeTime ;
	
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
		if( nextSpikeTime <= clock ) {
			double clockDrift = clock - nextSpikeTime ;
					
			// 1.0 => 1 kHz
			//
			// 	  potential * 1000 = kHz
			// 	+ clock starting from now
			// 	+ ( clock - nextSpikeTime ) to keep track of fractions
			nextSpikeTime = (1.0-potential) / 1_000 + clock - clockDrift ;
			this.currentPotential = spikeValue ;
		} else {
			this.currentPotential = restingPotential ;
		}
	}

	@Override
	public boolean isSpiking() {
		return this.currentPotential > restingPotential ;
	}
}


