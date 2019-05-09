package com.rc ;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Evolution { 
	final static Logger log = LoggerFactory.getLogger( Evolution.class ) ;

	final static Random rng = new Random( 660 );

	final double clockTick ;
	final double mutationRate ;
	final int    simulations ;
	final int    population ;
	final int 	 epochs ;
	final int	 batchSize ;

	public Evolution( 
			double  clockTick,
			int     simulations ,
			double  mutationRate ,
			int 	epochs ,
			int     population,
            int     batchSize
			) {
		this.clockTick = clockTick ;
		this.mutationRate = mutationRate ;
		this.epochs = epochs ;
		this.simulations = simulations ;
		this.population = population ;
		this.batchSize = batchSize ;
	}	


	public Brain evolve( double patterns[][], double tickPeriod, int numInputs, int numOutputs, int rows, int cols ) throws Exception {
		log.info( "Evolution starts..." ) ;

		BrainData brainData[] = new BrainData[ population ] ;

		for( int i=0 ; i<brainData.length ; i++ ) {
			Brain b = new Brain( tickPeriod, numInputs, numOutputs, rows, cols ) ;
			brainData[i] = new BrainData( b ) ;
		}

		log.info( "Population created." ) ;

		double inputs[] = new double[ numInputs ] ;

		ExecutorService tpool = Executors.newFixedThreadPool(7) ;

		for( int e=0 ; e<epochs ; e++ ) {	// A3 = run each independent brain for 1 generation

			for( int i=0 ; i<brainData.length ; i++ ) {
				brainData[i].resetScore(); 
				brainData[i].brain.resetNeurons() ;
			}

			int p = 0 ;		// pattern to display
			for( int simulation=0 ; simulation<simulations ; simulation++ ) {

				// A1 = rotate through patterns
				for( int i=0 ; i<patterns[p].length ; i++ ) {
					inputs[i] = patterns[p][i] ;
				}
				p++ ;
				if( p>=patterns.length ) {
					p = 0 ;
				}
				// A1

				// A2 = threads for 1 step & 1 train
				final CountDownLatch cdl = new CountDownLatch( brainData.length ) ;
				for( int i=0 ; i<brainData.length ; i++ ) {
					final Brain brain = brainData[i].brain ;
					final int testPattern = p ;
					tpool.submit( () -> {
						try {
							for( int b=0 ; b<batchSize ; b++ ) {
								brain.step(inputs);
								brain.train(testPattern);
							}
						} catch (Throwable t) {
							log.error("Failed to step", t);
						} finally {
							cdl.countDown();
						}
					}) ;
				}
				cdl.await();  // wait for all brains to complete one step and scoring
				// A2
			} // end of the simulations

			for( int i=0 ; i<brainData.length ; i++ ) {
				brainData[i].updateScore() ;
			}

			Arrays.sort( brainData ) ;
			
			// Euthanize the lower ( weakest scores ) half 
			int numNewBrains = brainData.length / 2 ;

			for( int i=0 ; i<numNewBrains ; i++ ) {
				// Prefer smarter brains 
				// if we can choose a high score ( from beginning of the sorted population )
				int ix1 = rng.nextInt( Math.max(numNewBrains/2, i) ) ;
				int ix2 = rng.nextInt( Math.max(numNewBrains/2, i) ) ;

				Genome p1 = brainData[ ix1 ].genome() ; 
				Genome p2 = brainData[ ix2 ].genome() ;

				Genome child = new Genome( p1, p2, mutationRate, Options.FIXED_PARAMS ) ;
				
				brainData[brainData.length-i-1] = new BrainData( new Brain( tickPeriod, child ) ) ;
			}	
			log.info( "Epoch {} - best score {}", e, brainData[0].score ) ;
		} // A3 - all generations have been tested

		tpool.shutdown();
		boolean oops = tpool.awaitTermination( 10, TimeUnit.MINUTES ) ;
		if( !oops  ) {
			log.warn( "OMG - too late "); 
		}

		BrainData bd =  brainData[0] ;
		log.info( "Best score = {}", bd.score ) ;
		return bd.brain ;
	}	


	class BrainData implements Comparable<BrainData>{
		final Brain brain ;
		double score ;

		public BrainData( Brain b ) {
			this.brain = b ;
		}

		public Genome genome() {
			return brain.toGenome() ;
		}

		public void resetScore() {
			score = 0 ;
		}
		
		public void updateScore() {
			score += brain.getSummaryScore() ;
			brain.resetSummaryScore() ;
			log.debug( "Scored {}", score ) ;
		}

		
		// Sorts large to small ( inverse to usual order )
		@Override
		public int compareTo(BrainData o) {		
			log.debug( "Comparing {} with {}", score, o.score ) ;
			return (score > o.score ) ? -1 : ( (score < o.score) ? 1 : 0 ) ;
		}
	}
}