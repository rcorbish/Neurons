package com.rc.neurons ;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rc.Genome;


public class NeuronCH extends Neuron {

	private static final double A = 0.02 ;
	private static final double B = 0.2 ;
	private static final double C = -50 ;
	private static final double D = 2 ;
	
	public NeuronCH( int id ) {
		super( id, A, B, C, D ) ;
	}

	public NeuronCH( Genome genome, int id ) {
		super( genome, id ) ;
	}

	@Override
	public boolean isInhibitor() { return false ; }

	@Override
	public NeuronType getType() {
		return NeuronType.CH ;
	}

}


