package com.rc ;

public class Neuron  {
	
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
		this.decay = 1 ;
		this.name = name ;
	}


	public void setPotential( double currentPotential ) {
		if( spikeIndex < 0 ) {
			this.currentPotential *= decay ;
			this.currentPotential += currentPotential ;
			if( currentPotential>threshold ) {
				spikeIndex = 1 ;
				this.currentPotential = spike[spikeIndex] ;
			}
//			this.currentPotential = restingPotential ;
		} else {
			this.currentPotential = spike[spikeIndex] ;
			spikeIndex++ ;
			if( this.spikeIndex >= spike.length ) {
				spikeIndex = -1 ;
			}
		}
	}
	
	public String getName() { return name ; }
	public double getPotential() { 	return currentPotential ;}

}


