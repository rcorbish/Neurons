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
	private final static int GENOME_CAPACITY = 3 ;

	private double 	currentPotential ;
	private int 	spikeIndex ;

	public final String name ;
	public final double spike[] ;

	// These must be 0 .. 1  ( to map to a genome )
	public final double threshold  ;
	public final double decay ;
	// This is -0.5 .. +0.5
	public final double restingPotential ;

	public Neuron( String name, BrainParameters parameters ) {
		this.spikeIndex = -1 ;
		this.restingPotential = parameters.restingPotential ;
		this.threshold = parameters.spikeThreshold ;
		this.spike = parameters.spikeProfile ;
		this.decay = rng.nextDouble() / 8.0 + 0.85 ;
		this.name = name ;
	}

	public Neuron( String name, Genome genome ) {
		this.name = name ;

		this.threshold = genome.getValue( GENOME_INDEX_THRESHOLD ) ;
		this.restingPotential = genome.getValue( GENOME_INDEX_RESTING ) - 0.5 ;
		this.decay = genome.getValue( GENOME_INDEX_DECAY ) ;

		this.spike = new double[]{ 1.00, .70, .30, -.25, -.20, -.15  } ;
	}

	public void setPotential( double currentPotential ) {
		if( spikeIndex < 0 ) {
			this.currentPotential *= decay ;
			this.currentPotential += currentPotential ;
			if( this.currentPotential < restingPotential ) {
				this.currentPotential = restingPotential ;
			}
			if( this.currentPotential>threshold ) {
				spikeIndex = 1 ;
			}
		} else {
			this.currentPotential = restingPotential ;
			spikeIndex++ ;
			if( this.spikeIndex >= spike.length ) {
				spikeIndex = -1 ;
			}
		}
	}
	
	public String getName() { return name ; }
	
	public double getPotential() {
		double rc = restingPotential ;
		if( spikeIndex >= 0 ) {
			rc = spike[spikeIndex] ;
		}
		return rc ;
	}


	public Genome toGenome() {
		Genome rc = new Genome( GENOME_CAPACITY  ) ;
		rc.setValue( threshold, GENOME_INDEX_THRESHOLD ) ;
		rc.setValue( restingPotential + 0.5 , GENOME_INDEX_RESTING ) ;
		rc.setValue( decay, GENOME_INDEX_DECAY ) ;

		return rc ;
	}
}


