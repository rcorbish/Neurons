package com.rc.neurons ;

import com.rc.Genome;


public class NeuronRZ extends Neuron {

	private static final double A = 0.1 ;
	private static final double B = 0.26 ;
	private static final double C = -65 ;
	private static final double D = 2 ;
	
	public NeuronRZ( int id ) {
		super( id, A, B, C, D ) ;
	}

    @Override
	public boolean isInhibitor() { return false ; }

    @Override
    public NeuronType getType() {
        return NeuronType.RZ ;
    }

}


