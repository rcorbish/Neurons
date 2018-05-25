package com.rc.neurons ;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rc.Brain;
import com.rc.Genome;


public class InputNeuron extends NeuronRS {

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
	
	final static private double MAX_FREQUENCY = 200 ;
	@Override
	public void step( double potential, double clock ) {
		isSpiking = false ;
		if( nextSpikeTime <= clock && potential > 1e-6 ) {
			spike( clock ) ;
			this.currentPotential = getThreshold() ;
			double clockDrift = clock - nextSpikeTime ;
					
			// 	  1.0 potential => MAX_FREQUENCY Hz
			// 	  0.5 potential => MAX_FREQUENCY / 2 Hz etc.
			// 	+ clock starting from now
			// 	+ ( clock - nextSpikeTime ) to keep track of fractions
			nextSpikeTime = 1.0 / ( potential * MAX_FREQUENCY ) + clock ; 
			nextSpikeTime -= clockDrift ;			
		} else {
			this.currentPotential = c / 1000 ;
		}
	}

	
	@Override
	public void train( Brain brain, double clock ) {
	// don't train inputs
	}

	@Override
	public NeuronType getType() {
		return NeuronType.IN ;
	}

}


