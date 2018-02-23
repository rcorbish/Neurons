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


public class Evolution { 
	final static Logger logger = LoggerFactory.getLogger( Evolution.class ) ;

	final static Random rng = new Random( 660 );

	final double clockTick ;
	final double mutationRate ;
	final double lifespan ;
	final int    population ;
	final int 	 generations ;

	public Evolution( 
		double  clockTick,
		double  lifespan ,
		double  mutationRate ,
		int 	generations ,
		int     population 
	) {
		this.clockTick = clockTick ;
		this.mutationRate = mutationRate ;
		this.generations = generations ;
		this.lifespan = lifespan ;
		this.population = population ;
	}	


	public Brain evolve( double tick, final int ... layers ) throws Exception {
		logger.info( "Evolution starts..." ) ;
		
		BrainData brainData[] = new BrainData[ population ] ;
		
		for( int i=0 ; i<brainData.length ; i++ ) {
			brainData[i] = new BrainData( new Brain( tick, layers ) ) ;
		}

		logger.info( "Population created." ) ;

		double inputs[] = new double[ layers[0] ] ;
		
		ExecutorService tpool = Executors.newFixedThreadPool(6) ;
		
		for( int g=0 ; g<generations ; g++ ) {			
			for( double clock=0 ; clock<lifespan ; clock += clockTick ) {
				for( int i=0 ; i<inputs.length ; i++ ) {
					inputs[i] = rng.nextDouble() ;
				}
				final CountDownLatch cdl = new CountDownLatch( brainData.length ) ;
				for( int i=0 ; i<brainData.length ; i++ ) {
					final Brain brain = brainData[i].brain ;
					tpool.submit( new Thread() {
						public void run() {
							brain.step( inputs ) ;
							brain.train() ;
							brain.updateScores() ;
							cdl.countDown();
						}
					} ) ;
				}
				cdl.await();  // wait for all brains to complete one step and scoring
			}

			Arrays.sort( brainData ) ;
			int survivingIndex = brainData.length ;
			
			int numNewBrains = brainData.length / 2 ;
						
			for( int i=0 ; i<numNewBrains ; i++ ) {
				int ix1 = rng.nextInt( Math.max(numNewBrains/4, i) ) ;
				int ix2 = rng.nextInt( Math.max(numNewBrains/4, i) ) ;
				
				Genome p1 = brainData[ ix1 ].genome() ; 
				Genome p2 = brainData[ ix2 ].genome() ;
				
				Genome child = new Genome( p1, p2, mutationRate ) ;
				brainData[i] = new BrainData( new Brain( tick, child ) ) ;
			}	
			logger.info( "Generation {} - best score {}", g, brainData[0].brain.getScore() ) ;
		}
		
		tpool.shutdown();
		boolean oops = tpool.awaitTermination( 10, TimeUnit.MINUTES ) ;
		if( !oops  ) {
			logger.warn( "OMG - too late "); 
		}
		
		BrainData bd =  brainData[0] ;
		logger.info( "Best score = {}", bd.score() ) ;
		return bd.brain ;
	}	
}

class BrainData implements Comparable<BrainData>{
	final Brain brain ;
	
	public BrainData( Brain b ) {
		this.brain = b ;
	}

	public double score() {
		return brain.getScore() ;
	}
	public Genome genome() {
		return brain.toGenome() ;
	}

	@Override
	public int compareTo(BrainData o) {
		double diff = o.score() - score()  ;
		return diff>0 ? 1 : ( diff==0 ? 0 : -1 ) ;
	}
	
}