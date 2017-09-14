package com.rc ;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Main {
	final static Logger logger = LoggerFactory.getLogger( Monitor.class ) ;
	final static Random rng = new Random( 10 );

	public static void main(String[] args) {
		final int INPUT_COUNT = 2 ;
		final int OUTPUT_COUNT = 4 ;

		try {
			Brain brain = new Brain( 
					Brain.STANDARD, 
					INPUT_COUNT, 
					OUTPUT_COUNT, 
					new int[] { 40, 50 }	//  network size 
					) ;

			Monitor m = new Monitor( brain ) ;
			m.start();
			double inputs[] = new double[INPUT_COUNT] ;
			
			int clk = 0 ;
			for( ; ; ) {
				clk++ ;
				for( int i=0 ; i<INPUT_COUNT ; i++ ) {
					inputs[i] = rng.nextDouble()  ;
				}
				//inputs[0] = 1 / ( (clk % 10) + 1 ) ;
				// inputs[0] = Math.sin( clk / Math.PI ) ;
				// inputs[1] = Math.cos( 3 * clk / Math.PI ) ;
				// inputs[1] *= inputs[1] + rng.nextDouble()/10;
				brain.step( inputs ) ;
/*
				for( Neuron neuron : brain.getInputs() ) {
					System.out.println( neuron.toString() ) ;
				}
				for( Neuron neuron : brain ) {
					System.out.println( neuron.toString() ) ;
				}
				for( Neuron neuron : brain.getOutputs() ) {
					System.out.println( neuron.toString() ) ;
				}
*/		
				m.sendBrainData(); 
				Thread.sleep(100);
			}
		} catch( Throwable t ) {  
			t.printStackTrace();
		}
	}
}
