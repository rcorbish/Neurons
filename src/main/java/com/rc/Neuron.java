package com.rc ;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Neuron implements Iterable<Axon> {
	final private int indexInBrain ;
	final Brain brain ;
	
	final double spike[] ;

	private final List<Axon> inputs ;

	private double membraneTransmissionFactor ;
	private double currentPotential ;
	private double futurePotential ;
	private double threshold  ;
	private double restingPotential ;
	private int spikeIndex ;

	public Neuron( Brain brain, int indexInBrain, BrainParameters parameters ) {
		this.brain = brain ;
		this.indexInBrain = indexInBrain ;
		this.spikeIndex = 0 ;
		this.restingPotential = parameters.restingPotential ;
		this.threshold = parameters.spikeThreshold ;
		this.spike = parameters.spikeProfile ;
		this.membraneTransmissionFactor = parameters.transmissionFactor ;	// input weight
		this.inputs = new ArrayList<Axon>() ;
	}

	protected void removeReferencesTo( Neuron dead ) {
		for( Iterator<Axon> i=inputs.iterator() ; i.hasNext() ; ) {
			Axon a = i.next() ;
			if( a.getNeuron() == dead ) {
				i.remove(); 
			}
		}
	}

	public boolean isDead() {
		return this.inputs.isEmpty() || ( this.inputs.size()==1 && this.inputs.get(0).getNeuron()==this ) ;
	}

	// Based on all current inputs at T0 - set the T1 output value of the neuron
	public void updatePotential() {			
		futurePotential = 0.0 ;
		for( Axon axon : this ) {
			futurePotential += axon.getTransmittedPotential() ;
		}
		futurePotential *= this.membraneTransmissionFactor ;
	}



	public double getPotential() { 
		return currentPotential ; 
	}
	public double getFuturePotential() { 
		return futurePotential ; 
	}
	


	public void setPotential( double currentPotential ) { 
		this.currentPotential = currentPotential ; 
		// if( this.currentPotential > 1.20 ) this.currentPotential = 1.20 ;
		// if( this.currentPotential < -.20 ) this.currentPotential = -.20 ;
	}

	public void clock() {
		if( spikeIndex == 0 && futurePotential > threshold ) spikeIndex = 1 ;
		if( spikeIndex > 0 ) { 
			setPotential( spike[spikeIndex] ) ;
			spikeIndex++ ;
			if( spikeIndex == spike.length ) spikeIndex = 0 ;
		} else {
			setPotential( this.restingPotential )  ;
		}
	}

	
	public void addInput( Axon axon ) { inputs.add(axon) ; }
	public Iterator<Axon> iterator() { return inputs.iterator() ; }

	public double getMembraneTransmissionFactor() {	return membraneTransmissionFactor; }
	public void adjustMembraneTransmissionFactor(double membraneTransmissionFactorFactor ) {
		//this.membraneTransmissionFactor += membraneTransmissionFactorFactor;
		if( this.membraneTransmissionFactor <= -1.0 ) {
			this.membraneTransmissionFactor = -1.0 ;
		}
		if( this.membraneTransmissionFactor >= 1.0 ) {
			this.membraneTransmissionFactor = 1.0 ;
		}
	}


	public int getIndexInBrain() { return indexInBrain; }
	public String getName() { return getType().toString() + indexInBrain; }
	public Neuron getNeuronByIndex( int ix ) { return brain.getNeuronByIndex(ix); }

	public NeuronType getType() { return NeuronType.LIQUID ; }

	public String toString() {
		StringBuilder sb = new StringBuilder( getType().toString().substring(0,1) ) ; 
		sb
		.append( String.valueOf(getIndexInBrain() ) )
		.append( " <" )
		;  

		for( Axon axon : inputs ) {
			sb
			.append( " " ) 
			.append( axon.getNeuron().getType().toString().substring(0,1) )
			.append( axon.getNeuron().getIndexInBrain() ) ;
		}

		sb
		.append( " > ")
		.append( String.valueOf( getPotential() ) ) 
		; 
		
		return sb.toString() ;
	} 
}



enum NeuronType {
	INPUT, OUTPUT, LIQUID ;
}