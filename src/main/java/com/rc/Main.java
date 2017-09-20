package com.rc ;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Main {
	final static Logger logger = LoggerFactory.getLogger( Monitor.class ) ;

	final static Random rng = new Random( 660 );
	final static int INPUT_COUNT = 5 ;
	final static int OUTPUT_COUNT = 5 ;

	public static void main(String[] args) {

		String parameterFile = args.length > 0 ? args[0] : null ;

		try {
			BrainParameters parameters = new BrainParameters() ;
			parameters.numInputs = INPUT_COUNT ;
			parameters.numOutputs = OUTPUT_COUNT ;
			parameters.connectivityFactor = 0.75 ;
			parameters.inhibitorRatio = .5 ;
			parameters.dimensions = new int[]{ 25, 25 } ;
			parameters.spikeThreshold = 0.6 ;
			parameters.transmissionFactor = 1 ;
			parameters.spikeProfile = new double[]{ 0, 0.5, 1, 0.4, 0, -0.1, -0.17, -0.16, -0.15 } ;
			parameters.restingPotential = -.10 ;
			
			Brain brain = parameterFile==null ? 
							new Brain(parameters) : 
							Brain.load( parameterFile ) ;
							
			brain = evolve() ;
			if( parameterFile != null ) {
				brain.save( parameterFile ) ;
			}

			Monitor m = new Monitor( brain ) ;
			m.start();
			double inputs[] = new double[parameters.numInputs] ;

			int clk = 0 ;
			for( ; ; ) {
				clk++ ;
				for( int i=0 ; i<inputs.length ; i++ ) {
					inputs[i] =  rng.nextInt( 1+(clk % 4) )==0 ? 1 : 0 ;
				}
				// inputs[0] = 1 / ( (clk % 10) + 1 ) ;
				// inputs[0] = Math.abs( Math.sin( clk / Math.PI ) ) ;
				// inputs[1] = Math.cos( 3 * clk / Math.PI ) ;
				// inputs[1] *= inputs[1] + rng.nextDouble()/10;
				brain.step( inputs ) ;
				brain.updateScores() ;
				m.sendBrainData(); 
				Thread.sleep(150);
			}
		} catch( Throwable t ) {  
			t.printStackTrace();
		}
	}
	
	public static Brain evolve() throws Exception {
		BrainData brainData[] = new BrainData[ 5_000 ] ;
		
		for( int i=0 ; i<brainData.length ; i++ ) {
			BitSet bs = new BitSet( BrainParameters.GENOME_SIZE ) ;
			for( int j=0 ; j<BrainParameters.GENOME_SIZE ; j++ ) {
				if( rng.nextBoolean() ) {
					bs.set(j) ; 
				}
			}
			brainData[i] = new BrainData( bs ) ;
		}
		
		double inputs[] = new double[ INPUT_COUNT ] ;
		
		logger.info( "Runing epochs ..."  );
		
		ExecutorService tpool = Executors.newFixedThreadPool(6) ;
		for( int e=0 ; e<100 ; e++ ) {			
			for( int s=0 ; s<3_000 ; s++ ) {
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
							//logger.info( "CDL: {}", cdl.getCount() ) ; 
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
			for( int i=survivingIndex / 2 ; i<brainData.length ; i++ ) {
				int ix1 = rng.nextInt( survivingIndex ) ;
				int ix2 = rng.nextInt( survivingIndex ) ;
				
				// Make sure ix1 is higher score than ix2 ( brainData is sorted )
				if( ix1 > ix2 ) {
					int tmp = ix2 ;
					ix2 = ix1 ;
					ix1 = tmp ;
				}
				BitSet p1 = brainData[ ix1 ].genome;
				BitSet p2 = brainData[ ix2 ].genome;
				
				BitSet bs = new BitSet( BrainParameters.GENOME_SIZE ) ;
				
				// Inheritance
				for( int b=0 ; b<BrainParameters.GENOME_SIZE ; b++ ) {
					bs.set( b,  rng.nextInt(3)==0 ? p1.get(b) : p2.get(b) ) ;
				}
				// Mutation = 8%
				for( int b=0 ; b<BrainParameters.GENOME_SIZE ; b++ ) {
					if( rng.nextDouble() < 0.08 ) {
						bs.set( b,  rng.nextBoolean()  ) ;
					}
				}
				
				brainData[i] = new BrainData( bs ) ;
			}
			logger.info( "Epoch {} - best score {}", e, brainData[0].brain.getScore() ) ;
		}
		
		tpool.shutdown();
		boolean oops = tpool.awaitTermination( 10, TimeUnit.MINUTES ) ;
		if( !oops  ) {
			logger.warn( "OMG - too late "); 
		}
		
		BrainParameters bp = BrainParameters.fromBits( brainData[0].genome ) ;
		bp.numInputs = Main.INPUT_COUNT ;
		bp.numOutputs = Main.OUTPUT_COUNT ;
		logger.info( "Best bp = {}", bp ) ;
		return brainData[0].brain ;
	}
}

class BrainData implements Comparable<BrainData>{
	BitSet genome ;
	Brain brain ;
	double score ;
	
	public BrainData( BitSet genome ) {
		this.genome = genome ;
		BrainParameters bp = BrainParameters.fromBits( genome ) ;
		bp.numInputs = Main.INPUT_COUNT ;
		bp.numOutputs = Main.OUTPUT_COUNT ;
		this.brain = new Brain( bp ) ;
	}
	@Override
	public int compareTo(BrainData o) {
		double diff = o.score - brain.getScore()  ;
		return diff>0 ? 1 : ( diff==0 ? 0 : -1 ) ;
	}
	
}