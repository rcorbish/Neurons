package com.rc.neurons ;


public class NeuronRS extends Neuron {

	private static final double A = 0.02 ;
	private static final double B = 0.2 ;
	private static final double C = -65 ;
	private static final double D = 8 ;
	
	public NeuronRS( int id ) {
		super( id, A, B, C, D ) ;
	}

	@Override
	public boolean isInhibitor() { return false ; }

	@Override
	public NeuronType getType() {
		return NeuronType.RS ;
	}
}


