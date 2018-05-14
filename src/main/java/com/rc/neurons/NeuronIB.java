package com.rc.neurons ;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rc.Genome;


public class NeuronIB extends Neuron {

	final static Logger log = LoggerFactory.getLogger( NeuronIB.class ) ;

	private static final double A = 0.02 ;
	private static final double B = 0.2 ;
	private static final double C = -55 ;
	private static final double D = 4 ;

	public NeuronIB( int id ) {
		super( id, A, B, C, D, false ) ;
	}

	public NeuronIB( Genome genome, int id ) {
		super( genome, id ) ;
	}	
}


