package com.rc ;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.StringJoiner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.util.ConcurrentArrayQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import joptsimple.OptionParser;
import joptsimple.OptionSet;


public class Main {
	final static Logger logger = LoggerFactory.getLogger( Monitor.class ) ;

	final static Random rng = new Random( 660 );
	final static int INPUT_COUNT = 5 ;
	final static int OUTPUT_COUNT = 5 ;
	
	static int POPULATION = 5_000 ;
	static int EPOCHS = 200 ;
	static int SIMULATIONS = 2000 ;
	static double MUTATION = 0.1 ;
	
	public static void main(String[] args) {
		try {
			
			OptionParser parser = new OptionParser();
			
			parser.acceptsAll( asList("f", "file") , "The parameters in json format" ).withRequiredArg().ofType( String.class ) ;
			parser.acceptsAll( asList("e", "evolve") , "Whether to run the evolution step" ) ;
			parser.accepts( "epochs" , "Number of epochs to run" ).withRequiredArg().ofType( Integer.class ) ; 
			parser.accepts( "clear" , "Delete existing parameters" ) ; 
			parser.accepts( "simulations" , "Number of simulations for each brain" ).withRequiredArg().ofType( Integer.class ) ; 
			parser.accepts( "population" , "Number of brains in the population" ).withRequiredArg().ofType( Integer.class ) ; 
			parser.accepts( "mutation" , "Mutation amount 0.0 - 1.0" ).withRequiredArg().ofType( Double.class ) ; 
			parser.nonOptions( "Network dimensions  (e.g. 3 4 )" ).ofType( Integer.class ) ; 
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
			StringJoiner sj = new StringJoiner( " x " ) ;
			for( int d : dims ) {
				sj.add( String.valueOf(d) ) ;
			}
			logger.info("Network size  : {}", sj );

			
			boolean evolve = options.has( "e" ) ;
			
			// BrainParameters parameters = new BrainParameters() ;
			// parameters.numInputs  = INPUT_COUNT ;
			// parameters.numOutputs = OUTPUT_COUNT ;
			
			boolean fileExists = false ;
			if( parameterFile !=null ) {
				File f = new File( parameterFile ) ;
				fileExists = f.canRead() ;
			}
			Brain brain = fileExists && !options.has("clear") ? 
							Brain.load( parameterFile, dims ) :
							new Brain( INPUT_COUNT, OUTPUT_COUNT, dims ) ;
			if( evolve ) {
				logger.info("Epochs        : {}", EPOCHS );
				logger.info("Population    : {}", POPULATION );
				logger.info("Simulations   : {}", SIMULATIONS );
				logger.info("Mutation Rate : {}", MUTATION );
				brain = evolve( dims ) ;
				
				if( parameterFile != null ) {
					brain.save( parameterFile ) ;
				}
			}

			Monitor m = new Monitor( brain ) ;
			m.start();
			double inputs[] = new double[INPUT_COUNT] ;

			int clk = 0 ;
			for( ; ; ) {
				clk++ ;
				for( int i=0 ; i<inputs.length ; i++ ) {
					inputs[i] =  rng.nextInt( 1+(clk % 4) )==0 ? 1 : 0 ;
					inputs[i] =  Math.cos(i * clk / Math.PI ) ;
					inputs[i] =  0.1 + 1/(i+1) ;
				}
				// inputs[0] = 1 / ( (clk % 10) + 1 ) ;
				// inputs[0] = Math.abs( Math.sin( clk / Math.PI ) ) ;
				// inputs[1] = Math.cos( 3 * clk / Math.PI ) ;
				// inputs[1] *= inputs[1] + rng.nextDouble()/10;
				brain.step( inputs ) ;
				brain.updateScores() ;
				m.sendBrainData() ; 
				Thread.sleep( 100 ) ;
			}
		} catch( Throwable t ) {  
			t.printStackTrace();
		}
	}
	
	public static Brain evolve( final int ... dims ) throws Exception {
		logger.info( "Evolution starts..." ) ;
		
		BrainData brainData[] = new BrainData[ POPULATION ] ;
		/*
		for( int i=0 ; i<brainData.length ; i++ ) {
			BitSet bs = new BitSet( BrainParameters.GENOME_SIZE ) ;
			for( int j=0 ; j<BrainParameters.GENOME_SIZE ; j++ ) {
				if( rng.nextBoolean() ) {
					bs.set(j) ; 
				}
			}
			brainData[i] = new BrainData( bs, dims ) ;
		}

		logger.info( "Population created." ) ;

		double inputs[] = new double[ INPUT_COUNT ] ;
		
		ExecutorService tpool = Executors.newFixedThreadPool(6) ;
		
		for( int e=0 ; e<EPOCHS ; e++ ) {			
			for( int s=0 ; s<SIMULATIONS ; s++ ) {
				for( int i=0 ; i<inputs.length ; i++ ) {
					inputs[i] = rng.nextDouble() ;
				}
				final CountDownLatch cdl = new CountDownLatch( brainData.length ) ;
				for( int i=0 ; i<brainData.length ; i++ ) {
					final Brain brain = brainData[i].brain ;
					tpool.submit( new Thread() {
						public void run() {
							brain.step( inputs ) ;
							brain.updateScores() ;
							cdl.countDown();
						}
					} ) ;
				}
				cdl.await();  // wait for all brains to complete one step and scoring
			}
			
			for( int i=0 ; i<brainData.length ; i++ ) {
				brainData[i].score = brainData[i].brain.getScore() ;
			}
			Arrays.sort( brainData ) ;
			int survivingIndex = brainData.length ;
			
			int numNewBrains = brainData.length / 2 ;
			CountDownLatch cdl = new CountDownLatch(numNewBrains) ;
			
			final Queue<BrainData> newBrains = new ConcurrentArrayQueue<>() ;
			
			for( int i=0 ; i<numNewBrains ; i++ ) {
				int ix1 = rng.nextInt( Math.max(numNewBrains/4, i) ) ;
				int ix2 = rng.nextInt( Math.max(numNewBrains/4, i) ) ;
				
				// Make sure ix1 is higher score than ix2 ( brainData is sorted )
				if( ix1 > ix2 ) {
					int tmp = ix2 ;
					ix2 = ix1 ;
					ix1 = tmp ;
				}
				BitSet p1 = brainData[ ix1 ].genome;
				BitSet p2 = brainData[ ix2 ].genome;
				
				final BitSet bs = new BitSet( BrainParameters.GENOME_SIZE ) ;
				
				// Inheritance
				for( int b=0 ; b<BrainParameters.GENOME_SIZE ; b++ ) {
					bs.set( b,  rng.nextInt(2)==0 ? p2.get(b) : p1.get(b) ) ;
				}
				// Mutation = x%
				for( int b=0 ; b<BrainParameters.GENOME_SIZE ; b++ ) {
					if( rng.nextDouble() < MUTATION ) {
						bs.set( b,  rng.nextBoolean()  ) ;
					}
				}
				Runnable t = new Runnable() {
					public void run() {
						try {
							BrainData bd = new BrainData( bs, dims ) ;
							newBrains.add( bd ) ;
						} catch( Throwable t ) {
							logger.warn( "WTF - brainless.", t ) ;
						}
						cdl.countDown();
					}
				} ;
				tpool.submit( t ) ;
			}
			cdl.await() ; 
			
			for( int i=0 ; i<newBrains.size() ; i++ ) {
				BrainData bd = newBrains.remove() ;
				brainData[brainData.length - i - 1] = bd ;				
			}
			
			logger.info( "Epoch {} - best score {}", e, brainData[0].brain.getScore() ) ;
		}
		
		tpool.shutdown();
		boolean oops = tpool.awaitTermination( 10, TimeUnit.MINUTES ) ;
		if( !oops  ) {
			logger.warn( "OMG - too late "); 
		}
		
		BrainData bd =  brainData[0] ;
		BrainParameters bp = new BrainParameters( bd.genome ) ;
		bp.numInputs = Main.INPUT_COUNT ;
		bp.numOutputs = Main.OUTPUT_COUNT ; 
		logger.info( "Best bp = {}\nScore = {}", bp, bd.score ) ;

		BrainData bdw =  brainData[brainData.length-1] ;
		bp = new BrainParameters( bdw.genome ) ;
		bp.numInputs = Main.INPUT_COUNT ;
		bp.numOutputs = Main.OUTPUT_COUNT ; 
		
		logger.info( "Worst bp = {}\nScore = {}", bp, bdw.score ) ;
		return bd.brain ;
		*/
		return null ;
	}
	
	private static List<String> asList( String ... strings ) {
		List<String> rc = new ArrayList<String>() ;
		for( String string : strings ) rc.add( string ) ;
		return rc ;
	}
}

class BrainData implements Comparable<BrainData>{
	BitSet genome ;
	Brain brain ;
	double score ;
	
	public BrainData( BitSet genome, int ...dims ) {
		this.genome = genome ;
		this.brain = new Brain( 1 ,1, dims ) ;
	}
	@Override
	public int compareTo(BrainData o) {
		double diff = o.score - brain.getScore()  ;
		return diff>0 ? 1 : ( diff==0 ? 0 : -1 ) ;
	}
	
}