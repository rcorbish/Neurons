package com.rc.neurons ;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rc.Genome;


public class NeuronLTS extends Neuron {

	final static Logger log = LoggerFactory.getLogger( NeuronLTS.class ) ;
	
	private static final double A = 0.02 ;
	private static final double B = 0.25 ;
	private static final double C = -65 ;
	private static final double D = 2 ;
	
	public NeuronLTS( int id ) {
		super( id, A, B, C, D, true ) ;
	}

	public NeuronLTS( Genome genome, int id ) {
		super( genome, id ) ;
	}	
}


