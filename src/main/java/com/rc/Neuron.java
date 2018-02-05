package com.rc ;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Neuron  {

	final static Logger log = LoggerFactory.getLogger( Neuron.class ) ;

	private final static Random rng = new Random()  ;
	private final static int GENOME_INDEX_THRESHOLD = 0 ;
	private final static int GENOME_INDEX_RESTING = 1 ;
	private final static int GENOME_INDEX_DECAY = 2 ;
	private final static int GENOME_INDEX_ID = 3 ;
	private final static int GENOME_INDEX_LEARNING_RATE = 4 ;
	public  final static int GENOME_SIZE = 5 ;

	private final int spikeDuration ;
	
	private double 	learningRate ;
	private double 	currentPotential ;
	private int 	spikeIndex ;
	public  int 	lastSpikeTime ;

	public final int index ;

	// These must be 0 .. 1  ( to map to a genome )
	public final double threshold  ;
	public final double decay ;
	// This is -0.5 .. +0.5
	public final double restingPotential ;

	public Neuron( int index ) {
		this.spikeIndex = -1 ;
		this.restingPotential = 0 ; //parameters.restingPotential ;
		this.threshold = 0.70 + rng.nextDouble() / 10.0 ;
		this.decay = 0.25 + rng.nextDouble() / 10.0 ;
		this.learningRate = rng.nextDouble() / 15.0 ;
		this.index = index ;
		lastSpikeTime = 0 ;
		spikeDuration = (int)Math.floor( Math.log(0.1) / -decay ) ;
	}

	public Neuron( Genome genome ) {
		this.threshold = genome.getDouble( GENOME_INDEX_THRESHOLD ) ;
		this.restingPotential = genome.getDouble( GENOME_INDEX_RESTING ) - 0.5 ;
		this.decay = genome.getDouble( GENOME_INDEX_DECAY ) ;
		this.index = genome.getInt( GENOME_INDEX_ID ) ;
		this.learningRate = genome.getDouble( GENOME_INDEX_LEARNING_RATE ) ;
		spikeDuration = (int)Math.floor( Math.log(0.1) / -decay ) ;
	}
	
	public Genome toGenome() {
		Genome rc = new Genome() ;
		rc.set( threshold, GENOME_INDEX_THRESHOLD ) ;
		rc.set( restingPotential + 0.5 , GENOME_INDEX_RESTING ) ;
		rc.set( decay, GENOME_INDEX_DECAY ) ;
		rc.set( index, GENOME_INDEX_ID ) ;
		rc.set( learningRate, GENOME_INDEX_LEARNING_RATE ) ;

		return rc ;
	}

	public void decay() {
		this.currentPotential -= decay * this.currentPotential ;
	}

	
	public void setPotential( double currentPotential ) {
		lastSpikeTime++ ;
		if( spikeIndex < 0 ) {
			this.currentPotential += currentPotential ;
			if( this.currentPotential < restingPotential ) {
				this.currentPotential = restingPotential ;
			}
			if( this.currentPotential>threshold ) {
				this.currentPotential = 1.0 ;
				lastSpikeTime = 0 ;
				spikeIndex = spikeDuration ;
			}
		} else {
			spikeIndex--;
//			if( spikeIndex==0 ) { 
				this.currentPotential = restingPotential ;
//			} 
		}
	}
	
	public void train( Brain brain ) {
		// if just fired - find all inputs 
		// and when they fired, update weights positively 
		// for neurons that have fired recently
		if( lastSpikeTime == 0 ) {
			EdgeList edges = brain.getEdgeList( index ) ;
			for( Edge e : edges ) {
				Neuron source = brain.getNeuron( e.source() ) ;
				int timeSinceFired = source.lastSpikeTime ;
				if( timeSinceFired > 0 ) {		// same step - can't be the cause !
					double increase = learningRate / ( timeSinceFired * timeSinceFired ) ;
					e.addWeight( increase ) ;
				} else {
					// 
					double increase = learningRate * ( timeSinceFired * timeSinceFired ) ;
					if( increase > 0.05 ) increase = 0.05 ;
					e.addWeight( -increase ) ;
				}
			}
		}
	}
	
	public int getIndex() { return index ; }
	public double getPotential() { return currentPotential ; }


}


