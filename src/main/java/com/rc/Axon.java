package com.rc ;

public class Axon {
	private final double membraneTransmissionFactor ;
	private Neuron neuron ;

	public Axon( Neuron neuron ) {
		this( neuron, 1.0 ) ;
	}
	
	public Axon( Neuron neuron, double membraneTransmissionFactor ) {
		this.neuron = neuron ;
		this.membraneTransmissionFactor  = membraneTransmissionFactor ;
	}
	
	public double getTransmittedPotential() {
		return neuron.getPotential() * membraneTransmissionFactor ;
	}

	public double getMembraneTransmissionFactor() {
		return membraneTransmissionFactor;
	}

	public Neuron getNeuron() {
		return neuron;
	}
	public String toString() { return String.valueOf(neuron.getIndexInBrain()) +"->"+String.valueOf(membraneTransmissionFactor) ; } 

}

