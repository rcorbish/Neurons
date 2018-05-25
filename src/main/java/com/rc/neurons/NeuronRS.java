package com.rc.neurons ;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rc.Genome;


public class NeuronRS extends Neuron {

	private static final double A = 0.02 ;
	private static final double B = 0.2 ;
	private static final double C = -65 ;
	private static final double D = 8 ;
	
	public NeuronRS( int id ) {
		super( id, A, B, C, D ) ;
	}

	public NeuronRS( Genome genome, int id ) {
		super( genome, id ) ;
	}

	@Override
	public boolean isInhibitor() { return false ; }

	@Override
	public NeuronType getType() {
		return NeuronType.RS ;
	}
}


