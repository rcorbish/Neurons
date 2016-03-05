package local.rac.custom;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Neuron implements Iterable<Axon> {
	final private int indexInBrain ;
	final Brain brain ;
	
	private List<Axon> inputs ;

	// Things that can be adjusted during learning
	private double membraneTransmissionFactor ;

	// Immediate attributes as a result of stimulii
	private double [] currentPotential ;	

	public Neuron( Brain brain, int indexInBrain ) {
		this.brain = brain ;
		this.indexInBrain = indexInBrain ;

		this.inputs = new ArrayList<Axon>() ;

		// 		Other Constants
		this.membraneTransmissionFactor = 0.75 ;	// input weight

		// instance data
		this.currentPotential = new double[Brain.HISTORY_LENGTH] ;		// starting potentials
	}



	// Based on all current inputs at T0 - set the T1 output value of the neuron
	public void clock() {			
		double potential = 0.0 ;
		for( Axon axon : this ) {
			potential += axon.getTransmittedPotential() ;
		}
		potential *= this.membraneTransmissionFactor ;
		setPotential( potential ) ;
	}


	public double getPotential(int index) { return currentPotential[index] ; }
	public double getPotential() { return getPotential(brain.getClock() ) ; }
	public void setPotential(double potential) { this.currentPotential[brain.getClock()] = potential ; }

	public void addInput( Axon axon ) { inputs.add(axon) ; }
	public Iterator<Axon> iterator() { return inputs.iterator() ; }

	public double getMembraneTransmissionFactor() {	return membraneTransmissionFactor; }
	public void adjustMembraneTransmissionFactor(double membraneTransmissionFactorFactor ) {
		this.membraneTransmissionFactor += membraneTransmissionFactorFactor;
		if( this.membraneTransmissionFactor <= -1.0 ) {
			this.membraneTransmissionFactor = -1.0 ;
		}
		if( this.membraneTransmissionFactor >= 1.0 ) {
			this.membraneTransmissionFactor = 1.0 ;
		}
	}


	public int getIndexInBrain() { return indexInBrain; }

	public NeuronType getType() { return NeuronType.INTERNAL ; }

	public String toString() { return "[" + String.valueOf(getIndexInBrain()) + "]" + String.valueOf( getPotential() ) ; } 
}



enum NeuronType {
	INPUT, OUTPUT, INTERNAL ;
}