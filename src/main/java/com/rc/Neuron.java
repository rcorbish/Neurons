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
	private final static int GENOME_INDEX_SPIKE_VALUE = 5 ;
	public  final static int GENOME_SIZE = 6 ;

	private final int spikeDuration ;
	
	public double 	learningRate ;
	protected double 	currentPotential ;
	public  int 	spikeIndex ;

	public final int index ;

	// These must be 0 .. 1  ( to map to a genome )
	public final double threshold  ;
	public final double decay ;
	public final double spikeValue ;
	// This is -0.5 .. +0.5
	public final double restingPotential ;

	public Neuron( int index ) {
		this.restingPotential = 0 ; // ;
		this.threshold = 0.70 + rng.nextDouble() / 10.0 ;
		this.decay = 0.001 ; 		// rng.nextDouble() / 10.0 ;
		this.learningRate = rng.nextDouble() / 100.0 ;
		this.spikeValue = 0.01 + rng.nextDouble() ;
		this.index = index ;

		this.currentPotential = rng.nextDouble() ;
		spikeDuration = 5 ;
		spikeIndex = 1 ;
	}

	public Neuron( Genome genome ) {
		this.threshold = genome.getDouble( GENOME_INDEX_THRESHOLD ) ;
		this.restingPotential = genome.getDouble( GENOME_INDEX_RESTING ) - 0.5 ;
		this.decay = genome.getDouble( GENOME_INDEX_DECAY ) ;
		this.index = genome.getInt( GENOME_INDEX_ID ) ;
		this.learningRate = genome.getDouble( GENOME_INDEX_LEARNING_RATE ) ;
		this.spikeValue = genome.getDouble( GENOME_INDEX_SPIKE_VALUE ) ;


		this.currentPotential = rng.nextDouble() ;
		spikeDuration = 5 ;
		spikeIndex = 1 ;
	}
	
	public Genome toGenome() {
		Genome rc = new Genome() ;
		rc.set( threshold, GENOME_INDEX_THRESHOLD ) ;
		rc.set( restingPotential + 0.5, GENOME_INDEX_RESTING ) ;
		rc.set( decay, GENOME_INDEX_DECAY ) ;
		rc.set( index, GENOME_INDEX_ID ) ;
		rc.set( learningRate, GENOME_INDEX_LEARNING_RATE ) ;
		rc.set( spikeValue, GENOME_INDEX_SPIKE_VALUE ) ;

		return rc ;
	}

	public void absolutePotential( double potential ) {
		this.currentPotential = potential ;
	}
	
	public void setPotential( double potential ) {
		if( spikeIndex < spikeDuration ) {
			this.currentPotential = restingPotential ;
		} else {
			if( this.currentPotential>threshold ) {
				spikeIndex = -1 ;
				this.currentPotential += spikeValue ;
			} else {
				this.currentPotential += potential ;
				this.currentPotential -= decay ;
			}
			if( this.currentPotential < restingPotential ) {
				this.currentPotential = restingPotential ;
			} 
		}
		spikeIndex++ ;
	}
	
	public void spike() {
		spikeIndex = 0 ;
		this.currentPotential += spikeValue ;
	}
	
	
	public void train( Brain brain ) {
		
		// Look for pre-synaptic spikes (we received a spike before we spiked)
		// and post-synaptic spikes (we spiked before receiving a spike)
		EdgeList edges = brain.getIncomingEdges(index) ;
		for( Edge e : edges ) {			
			Neuron source = brain.getNeuron( e.source() ) ;
			int deltaFiredTime = source.spikeIndex - spikeIndex ;
			// pre-synaptic spike occurs before ( spikeIndex > ours )
			if( spikeIndex < 5 && deltaFiredTime > 0 ) {
				//reinforce
				double delta = learningRate / ( 1 + deltaFiredTime * deltaFiredTime ) ;
				e.addWeight( delta ) ;
			} else if( spikeIndex < 5 && deltaFiredTime < 1 ) {
				//suppress
				double delta = learningRate / ( 1 + deltaFiredTime * deltaFiredTime ) ;
				e.addWeight( -delta ) ;
			}
		}
	}
	
	public int getIndex() { return index ; }
	public double getPotential() { return currentPotential ; }
	public boolean isSpiking() { return spikeIndex == 0 ; }
}


