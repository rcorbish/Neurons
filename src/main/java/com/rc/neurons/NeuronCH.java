package com.rc.neurons ;


/**
 * A Chattering neuron
 */
public class NeuronCH extends Neuron {

	private static final double A = 0.02 ;
	private static final double B = 0.2 ;
	private static final double C = -50 ;
	private static final double D = 2 ;
	
	public NeuronCH( int id ) {
		super( id, A, B, C, D ) ;
	}

	@Override
	public boolean isInhibitor() { return false ; }

	@Override
	public NeuronType getType() {
		return NeuronType.CH ;
	}

}


