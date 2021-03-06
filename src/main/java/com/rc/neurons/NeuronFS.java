package com.rc.neurons ;


/**
 * A fast spiking neuron
 */
public class NeuronFS extends Neuron {

	private static final double A = 0.1 ;
	private static final double B = 0.2 ;
	private static final double C = -65 ;
	private static final double D = 2 ;
	
	public NeuronFS( int id ) {
		super( id, A, B, C, D ) ;
	}

	@Override
	public boolean isInhibitor() { return false ; }

	@Override
	public NeuronType getType() {
		return NeuronType.FS ;
	}

	@Override
	public double getSpikeValue() { return -super.getSpikeValue() ; }
}


