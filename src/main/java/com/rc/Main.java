package com.rc ;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;
import java.util.StringJoiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import joptsimple.OptionParser;
import joptsimple.OptionSet;


public class Main {
	final static Logger logger = LoggerFactory.getLogger( Monitor.class ) ;

	final static Random rng = new Random( 660 );
	
	static int POPULATION = 5_000 ;
	static int EPOCHS = 200 ;
	static int SIMULATIONS = 2000 ;
	static double TICK_PERIOD = 1e-4 ;   // each clock tick in seconds - default 100uS
	static double MUTATION = 0.1 ;
	static long DELAY_INTERVAL  = 150 ;
	
	static double [][] TestPatterns = {
		{ 0.5, 0.3, 0.6, 0.1, 0.9, 0.9 },
		{ 0.9, 0.9, 0.9, 0.1, 0.1, 0.1 },
		{ 0.9, 0.5, 0.1, 0.1, 0.5, 0.9 },
		{ 0.1, 0.5, 0.9, 0.9, 0.5, 0.1 },
		{ 0.9, 0.9, 0.9, 0.9, 0.5, 0.1 },
		{ 0.1, 0.5, 0.9, 0.9, 0.9, 0.9 },
		{ 0.5, 0.5, 0.5, 0.5, 0.5, 0.5 },
		{ 0.1, 0.1, 0.1, 0.1, 0.1, 0.1 },
		{ 0.9, 0.9, 0.9, 0.9, 0.9, 0.9 }
	} ;

	public static void main(String[] args) {
		try {
			
			OptionParser parser = new OptionParser();
			
			parser.acceptsAll( asList("f", "file") , "The parameters in json format" ).withRequiredArg().ofType( String.class ) ;
			parser.acceptsAll( asList("e", "evolve") , "Whether to run the evolution step" ) ;
			parser.acceptsAll( asList("p", "period") , "Each clock step is this long (uS)" ).withRequiredArg().ofType( Integer.class ) ;
			parser.acceptsAll( asList("u", "update-delay" ) , "Interval between updates 0-200 mS" ).withRequiredArg().ofType( Long.class ) ; 
			parser.accepts( "epochs" , "Number of epochs to run" ).withRequiredArg().ofType( Integer.class ) ; 
			parser.accepts( "clear" , "Delete existing parameters" ) ; 
			parser.accepts( "train" , "Train the network" ) ; 
			parser.accepts( "simulations" , "Number of simulations for each brain" ).withRequiredArg().ofType( Integer.class ) ; 
			parser.accepts( "population" , "Number of brains in the population" ).withRequiredArg().ofType( Integer.class ) ; 
			parser.accepts( "mutation" , "Mutation amount 0.0 - 1.0" ).withRequiredArg().ofType( Double.class ) ; 
			parser.nonOptions( "Network dimensions ( up to 3 ) (e.g. 3 4, 2 2 2 )" ).ofType( Integer.class ) ; 
			parser.accepts( "help", "This help" ).forHelp();
			
	        OptionSet options = parser.parse( args ) ;
	
			if( options.has( "help")  ) {
				try {
					parser.printHelpOn( System.out );
					System.exit(1) ;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

	        if( options.has( "simulations" ) ) 	{ SIMULATIONS = (int) options.valueOf("simulations") ; }
	        if( options.has( "population" ) ) 	{ POPULATION = (int) options.valueOf("population") ; }
	        if( options.has( "epochs" ) ) 		{ EPOCHS = (int) options.valueOf("epochs") ; }
	        if( options.has( "mutation" ) ) 	{ MUTATION = (double) options.valueOf("mutation") ; }
	        if( options.has( "update-delay" ) ) { DELAY_INTERVAL = (long) options.valueOf("update-delay") ; }
	        if( options.has( "period" ) ) 		{ TICK_PERIOD = (int) options.valueOf("period") ; }
	        boolean clearFile = options.has("clear") ;
	        boolean evolve    = options.has("evolve") ;
	        boolean train	  = options.has("train") ;
	        
	        String parameterFile = options.has("f") ? options.valueOf( "f" ).toString() : null ;

			int dims[] ;
			List<?> dimArgs = options.nonOptionArguments() ;
			if( dimArgs.size() > 0 ) {
				dims = new int[ dimArgs.size() ] ;
				for( int i=0 ; i<dims.length ; i++ ) {
					dims[i] =  Integer.parseInt( dimArgs.get(i).toString() ) ;
				}
			} else {
				dims = new int[]{ 3, 3 } ;	// default if no size given
			}
			StringJoiner sj = new StringJoiner( ", " ) ;
			for( int d : dims ) {
				sj.add( String.valueOf(d) ) ;
			}
			logger.info("Layers        : {}", sj ) ;
			logger.info("Delay         : {}", DELAY_INTERVAL ) ;
			
			boolean fileExists = false ;
			if( parameterFile !=null ) {
				File f = new File( parameterFile ) ;
				fileExists = f.canRead() ;
			}
			
			Brain brain ;
			if( fileExists && !clearFile ) {
				brain = Brain.load( TICK_PERIOD, parameterFile ) ;
			} else {
				brain = new Brain( TICK_PERIOD, dims ) ;
				if( parameterFile != null ) {
					brain.save( parameterFile ) ;
				}
			}

			if( evolve ) {
				logger.info("Epochs        : {}", EPOCHS );
				logger.info("Population    : {}", POPULATION );
				logger.info("Simulations   : {}", SIMULATIONS );
				logger.info("Mutation Rate : {}", MUTATION );
				final Evolution evolution = new Evolution(TICK_PERIOD, SIMULATIONS, MUTATION, EPOCHS, POPULATION ) ;
				brain = evolution.evolve( TICK_PERIOD, dims ) ;
				
				if( parameterFile != null ) {
					brain.save( parameterFile ) ;
				}
			}

			Monitor m = new Monitor( brain ) ;
			m.start();
			double inputs[] = new double[ dims[0] ] ;

			double clock = 0 ;
			int patternCount = 0 ;
			int patternIndex = 0 ;
			double testPattern[] = null ;
			long lastSentTime = 0 ;
			for( ; ; ) {
				clock += TICK_PERIOD ;

				patternCount-- ;
				if( patternCount<0) {
					patternCount = 100 ;
					patternIndex = rng.nextInt(TestPatterns.length) ;
					testPattern = TestPatterns[ patternIndex ] ;

					for( int i=0 ; i<inputs.length ; i++ ) {
						inputs[i] =  testPattern[i] ;
					}
				}

				brain.step( inputs ) ;
				if( train ) {
					brain.train() ;
				}

				long deltaTime = System.currentTimeMillis() - lastSentTime ;
				if( !train || deltaTime > DELAY_INTERVAL ) {
					lastSentTime = System.currentTimeMillis() ;
					m.sendBrainData( patternIndex, clock ) ; 
				}
				// if( !train ) {
					Thread.sleep( DELAY_INTERVAL ) ;
				// }
			}
		} catch( Throwable t ) {  
			t.printStackTrace();
		}
	}
		
	private static List<String> asList( String ... strings ) {
		List<String> rc = new ArrayList<String>() ;
		for( String string : strings ) rc.add( string ) ;
		return rc ;
	}
}
