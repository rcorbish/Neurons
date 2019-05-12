package com.rc.neurons;

import java.util.Random;

public class NeuronFactory {

	final static private Random rng = new Random( 128 ) ;
	
	final static private double chProb  = .10 ;
	final static private double rsProb  = .50 ;
	final static private double fsProb  = .10 ;
	final static private double ibProb  = .20 ;

	final static private double rzProb  = .05 ;
	final static private double ltsProb = .05 ;
	@SuppressWarnings("unused")
	final static private double tcProb  = .05 ;
	
	static public Neuron getNeuron( int id ) {
		double r  = rng.nextDouble() ;
		
		r -= rsProb ;  if( r<0 ) return new NeuronRS(id) ;
		r -= fsProb ;  if( r<0 ) return new NeuronFS(id) ;
		r -= rzProb ;  if( r<0 ) return new NeuronRZ(id) ;
		r -= chProb ;  if( r<0 ) return new NeuronCH(id) ;
		r -= ibProb ;  if( r<0 ) return new NeuronIB(id) ;
		r -= ltsProb ; if( r<0 ) return new NeuronLTS(id) ;
		return new NeuronTC(id) ;
	}

	static public Neuron getNeuron( NeuronType type, int id ) throws Exception {
		return type.create(id) ;
	}

}
