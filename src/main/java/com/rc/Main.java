package com.rc ;

import java.io.File;
import java.util.Random;

import com.rc.web.Monitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Main {
	final static Logger log = LoggerFactory.getLogger( Main.class ) ;

	final static Random rng = new Random( 660 ) ;
	
	public static void main(String[] args) {
		try {
			Options.parseCommandLine( args ) ;			
			
			boolean fileExists = false ;
			if( Options.parameterFile !=null ) {
				File f = new File( Options.parameterFile ) ;
				fileExists = f.canRead() ;
			}
			
			Brain brain ;
			
			if( fileExists && !Options.clearFile ) {
				brain = Brain.load( Options.TICK_PERIOD, Options.parameterFile ) ;
				if( brain.getRows() != Options.dims[0] || brain.getColumns() != Options.dims[1] ) {
					log.warn( "Incompatible size loaded: {} x {}", brain.getColumns(), brain.getRows() ) ;
				}
			} else {
				brain = new Brain( Options.TICK_PERIOD, 6, 10, Options.dims[0], Options.dims[1] ) ;
				if( Options.parameterFile != null ) {
					brain.save( Options.parameterFile ) ;
				}
			}

			if( Options.evolve ) {
				final Evolution evolution = new Evolution( Options.TICK_PERIOD, Options.SIMULATIONS, Options.MUTATION, Options.EPOCHS, Options.POPULATION, Options.BATCH_SIZE ) ;
				brain = evolution.evolve( Options.TestPatterns, Options.TICK_PERIOD, 6, 10, Options.dims[0], Options.dims[1] ) ;
				
				if( Options.parameterFile != null ) {
					brain.save( Options.parameterFile ) ;
				}
			}

			@SuppressWarnings("resource")
			Monitor m = new Monitor( brain ) ;
			m.start();
			
			double inputs[] = new double[ brain.getNumInputs() ] ;

			int patternCount = 0 ;
			int patternIndex = 0 ;
			double testPattern[] = null ;
			long lastSentTime = 0 ;
			for( ; ; ) {
				// Display a pattern for a few cycles, then change
				patternCount-- ;
				if( patternCount<0) {
					patternCount = 50 ;

					//patternIndex = rng.nextInt(TestPatterns.length) ;
					patternIndex = Options.train ? 
							rng.nextInt(Options.TestPatterns.length) : 
							m.getPatternId() ;
					
					if( patternIndex >=0 && patternIndex < Options.TestPatterns.length ) {
						testPattern = Options.TestPatterns[ patternIndex ] ;
					}

					for( int i=0 ; i<inputs.length ; i++ ) {
						inputs[i] = testPattern[i] ;
					}
				}
				
				// Do one clock period - full network traversal
				brain.step( inputs ) ;
				
				// If necessary - update history for GUI
				brain.follow() ;
				if( Options.train ) {
					brain.train( patternIndex ) ;
				}
				// -------------------------------------------------
				// If we're training, 
				// 	- send periodic updates
				// 	- operate machine at full speed
				// If we're not training 
				// 	- send info each step
				// 	- operate machine slowly
				if( Options.train ) {
					long sinceLastMessage = System.currentTimeMillis() - lastSentTime ;
					if( sinceLastMessage > Options.DELAY_INTERVAL ) {
						lastSentTime = System.currentTimeMillis() ;
						m.sendBrainData( brain.clock() ) ; 
					}
				} else {
					lastSentTime = System.currentTimeMillis() ;
					m.sendBrainData( brain.clock() ) ; 
					if( Options.DELAY_INTERVAL>0 ) {
						Thread.sleep( Options.DELAY_INTERVAL ) ;
					}
				}
			}
		} catch( Throwable t ) {  
			t.printStackTrace();
		}
	}		
}
