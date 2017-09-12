package com.rc ;

import java.util.Random;

import org.apache.log4j.Level;

public class Main {
	static Random rng = new Random( 10 );

	public static void main(String[] args) {
		final int INPUT_COUNT = 2 ;
		final int OUTPUT_COUNT = 6 ;

		try {
			Brain brain = new Brain( 
					-0.35, //Brain.STANDARD, 
					INPUT_COUNT, 
					OUTPUT_COUNT, 
					new int[] { 10, 10, 10 }	//  network size 
					) ;

			/*RestServer server = */new WebServer( brain ) ;
			double inputs[] = new double[INPUT_COUNT] ;
			
			int clk = 0 ;
			for( ; ; ) {
				clk++ ;
				for( int i=0 ; i<INPUT_COUNT ; i++ ) {
					inputs[i] = rng.nextDouble()  ;
				}
				inputs[0] = Math.sin( clk / Math.PI ) ;
				inputs[1] = Math.cos( clk / Math.PI ) ;
				inputs[1] *= inputs[1] ;
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
				Thread.sleep(50);
			}
		} catch( Throwable t ) {  
			t.printStackTrace();
		}
	}


	static {
		org.apache.log4j.LogManager.getLogger("org.eclipse.jetty").setLevel(Level.WARN);
		final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("org.eclipse.jetty");
		if ((logger instanceof ch.qos.logback.classic.Logger)) {
			ch.qos.logback.classic.Logger logbackLogger = (ch.qos.logback.classic.Logger) logger;
			logbackLogger.setLevel(ch.qos.logback.classic.Level.WARN);
		}
	}
}
