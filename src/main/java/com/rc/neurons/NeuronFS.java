package com.rc.neurons ;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rc.Genome;


public class NeuronFS extends Neuron {

	private static final double A = 0.1 ;
	private static final double B = 0.2 ;
	private static final double C = -65 ;
	private static final double D = 2 ;
	
	public NeuronFS( int id ) {
		super( id, A, B, C, D ) ;
	}

	public NeuronFS( Genome genome, int id ) {
		super( genome, id ) ;
	}

	@Override
	public boolean isInhibitor() { return true ; }

	@Override
	public NeuronType getType() {
		return NeuronType.FS ;
	}

}


