package com.rc.neurons ;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rc.Genome;


public class NeuronIB extends Neuron {

	private static final double A = 0.02 ;
	private static final double B = 0.2 ;
	private static final double C = -55 ;
	private static final double D = 4 ;

	public NeuronIB( int id ) {
		super( id, A, B, C, D ) ;
	}

	public NeuronIB( Genome genome, int id ) {
		super( genome, id ) ;
	}

	@Override
	public boolean isInhibitor() { return true ; }

	@Override
	public NeuronType getType() {
		return NeuronType.IB ;
	}

}


