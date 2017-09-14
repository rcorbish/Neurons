package com.rc ;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Neuron implements Iterable<Axon> {
	final private int indexInBrain ;
	final Brain brain ;

	final public static double THRESHOLD_TO_SPIKE = 0.75 ;
		
	final static double spike[] = { 1.00, .70, .30, .20, 0, -.10, -.08, -.08, -.05, -.05 ,-.03, -.02  } ;

	private final List<Axon> inputs ;

	// Things that can be adjusted during learning
	private double membraneTransmissionFactor ;

	private double currentPotential ;
	private double futurePotential ;
	
	private int spikeIndex ;

	public Neuron( Brain brain, int indexInBrain ) {
		this.brain = brain ;
		this.indexInBrain = indexInBrain ;
		this.spikeIndex = 0 ;

		this.inputs = new ArrayList<Axon>() ;

		// 		Other Constants
		this.membraneTransmissionFactor = 0.750 ;	// input weight

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
	


	public void setPotential( double currentPotential ) { 
		this.currentPotential = currentPotential ; 
		if( this.currentPotential > 1.0 ) this.currentPotential = 1.0 ;
		if( this.currentPotential < -.20 ) this.currentPotential = -.20 ;
	}

	public void clock() {
		if( spikeIndex == 0 && futurePotential > THRESHOLD_TO_SPIKE ) spikeIndex = 1 ;
		if( spikeIndex > 0 ) { 
			setPotential( spike[spikeIndex] ) ;
			spikeIndex++ ;
			if( spikeIndex == spike.length ) spikeIndex = 0 ;
		} else {
			setPotential( 0 )  ;
		}
	//	setPotential( futurePotential ) ;
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