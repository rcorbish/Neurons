package com.rc.neurons ;

import org.ejml.data.DMatrixSparseCSC;

import com.rc.Brain;


public class InputNeuron extends NeuronRS {

	final double clockOffset ;
	double nextSpikeTime ;
	
	public InputNeuron( int id ) {		
		super( id ) ;
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
	public void train( Brain brain, double clock, DMatrixSparseCSC training  ) {
		// don't train inputs
	}

	@Override
	public NeuronType getType() {
		return NeuronType.IN ;
	}

}


