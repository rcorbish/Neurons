package com.rc ;

import java.util.Random;

public class Neuron  {
	
	private final static Random rng = new Random()  ;
	private final double spike[] ;
	private double 	currentPotential ;
	private double 	threshold  ;
	private double 	restingPotential ;
	private int 	spikeIndex ;
	private final 	String  name ;
	private final   double decay ;

	public Neuron( String name, BrainParameters parameters ) {
		this.spikeIndex = -1 ;
		this.restingPotential = parameters.restingPotential ;
		this.threshold = parameters.spikeThreshold ;
		this.spike = parameters.spikeProfile ;
		this.decay = rng.nextDouble() / 4.0 + 0.72 ;
		this.name = name ;
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
				this.currentPotential = spike[spikeIndex] ;
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

}


