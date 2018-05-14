package com.rc.neurons ;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rc.Genome;


public class NeuronTC extends Neuron {

	final static Logger log = LoggerFactory.getLogger( NeuronTC.class ) ;
	
	private static final double A = 0.02 ;
	private static final double B = 0.25 ;
	private static final double C = -65 ;
	private static final double D = 0.05 ;
	
	public NeuronTC( int id ) {
		super( id, A, B, C, D, false ) ;
	}

	public NeuronTC( Genome genome, int id ) {
		super( genome, id ) ;
	}	
}


