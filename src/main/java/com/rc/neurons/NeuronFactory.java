package com.rc.neurons;

import java.util.Random;

public class NeuronFactory {

	final static private Random rng = new Random( 128 ) ;
	
	final static private double chProb  = .10 ;
	final static private double rsProb  = .40 ;
	final static private double fsProb  = .20 ;
	final static private double ibProb  = .15 ;
	final static private double ltsProb = .05 ;
	final static private double rzProb  = .05 ;
	final static private double tcProb  = .05 ;
	
	static public Neuron getNeuron( int id ) {
		double r  = rng.nextDouble() ;
		
		r -= chProb ;  if( r<0 ) return new NeuronCH(id) ;
		r -= rsProb ;  if( r<0 ) return new NeuronRS(id) ;
		r -= fsProb ;  if( r<0 ) return new NeuronFS(id) ;
		r -= ltsProb ; if( r<0 ) return new NeuronLTS(id) ;
		r -= rzProb ;  if( r<0 ) return new NeuronRZ(id) ;
		r -= ibProb ;  if( r<0 ) return new NeuronIB(id) ;
		return new NeuronTC(id) ;
	}
}
