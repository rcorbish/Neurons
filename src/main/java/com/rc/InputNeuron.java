package com.rc ;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class InputNeuron extends Neuron {

	final static Logger log = LoggerFactory.getLogger( InputNeuron.class ) ;
	
	public InputNeuron( int index ) {
		super( index ) ;
	}

	public InputNeuron( Genome genome ) {
		super( genome ) ;
	}
		
	public void setPotential( double potential ) {
		this.currentPotential = potential ;
	}
	
}


