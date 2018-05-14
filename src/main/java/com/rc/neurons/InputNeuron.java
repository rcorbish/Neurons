package com.rc.neurons ;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rc.Brain;
import com.rc.Genome;


public class InputNeuron extends NeuronRS {

	final static Logger log = LoggerFactory.getLogger( InputNeuron.class ) ;
	final static Random rng = new Random() ;
	
	final double clockOffset ;
	double nextSpikeTime ;
	
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
		isSpiking = false ;
		if( nextSpikeTime <= clock ) {
			spike( clock ) ;
			this.currentPotential = threshold ;
			double clockDrift = clock - nextSpikeTime ;
					
			// 	  potential => 1kHz
			// 	+ clock starting from now
			// 	+ ( clock - nextSpikeTime ) to keep track of fractions
			
			nextSpikeTime = (1.3 - potential) / 200 + clock ; 
			nextSpikeTime -= clockDrift ;
		} else {
			this.currentPotential = c ;
		}
	}

	
	@Override
	public void train( Brain brain, double clock ) {
	// don't train inputs
	}
}


