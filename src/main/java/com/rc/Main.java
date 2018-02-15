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
				brain = Brain.load( parameterFile ) ;
			} else {
				brain = new Brain( dims ) ;
				if( parameterFile != null ) {
					brain.save( parameterFile ) ;
				}
			}

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

			for( int i=0 ; i<TestPatterns.length ; i++ ) {
				double sum = 0 ;
				for( int j=0 ; j<TestPatterns[i].length ; j++ ) {
					sum += TestPatterns[i][j] ;
				}
				double factor = 0.25 / sum ;
				for( int j=0 ; j<TestPatterns[i].length ; j++ ) {
					TestPatterns[i][j] *= factor ;
				}
			}
			Monitor m = new Monitor( brain ) ;
			m.start();
			double inputs[] = new double[ dims[0] ] ;

			int clk = 0 ;
			int patternCount = 0 ;
			int patternIndex = 0 ;
			double testPattern[] = null ;
			long lastSentTime = 0 ;
			for( ; ; ) {
				clk++ ;

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
					brain.train( patternIndex ) ;
				}

				long deltaTime = System.currentTimeMillis() - lastSentTime ;
				if( !train || deltaTime > DELAY_INTERVAL ) {
					lastSentTime = System.currentTimeMillis() ;
					m.sendBrainData( patternIndex, clk ) ; 
				}
				if( !train ) {
					Thread.sleep( DELAY_INTERVAL ) ;
				}
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
	Genome genome ;
	Brain brain ;
	double score ;
	
	public BrainData( Brain b ) {
		this.genome = b.toGenome() ;
		this.brain = b ;
	}
	@Override
	public int compareTo(BrainData o) {
		double diff = o.score - brain.getScore()  ;
		return diff>0 ? 1 : ( diff==0 ? 0 : -1 ) ;
	}
	
}