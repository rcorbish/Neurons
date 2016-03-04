package local.rac.custom;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Neuron implements Iterable<NeuronWeight> {
	
	final private List<NeuronWeight> inputNeurons ;	
	final private int indexInBrain ;
	
	private NeuronType type ;
	
	// Things that can be adjusted during learning
	private double membraneTransmissionFactor ;

	// Immediate attributes as a result of stimulii
	private double futurePotential ;
	private double [] currentPotential ;
	
	private int clock ;
	
	public Neuron( int indexInBrain ) {
		
		this.indexInBrain = indexInBrain ;
		this.clock = 0 ;
		
		// 		Other Constants
		this.membraneTransmissionFactor = 0.75 ;	// input weight

		// instance data
		this.currentPotential = new double[100] ;		// starting potentials
		this.futurePotential = 0 ;		
		
		this.inputNeurons = new ArrayList<NeuronWeight>() ;
		
		this.setType( NeuronType.INTERNAL ) ;
	}
	

	public void connectNeuron( Neuron inputNeuron, double weight ) {
		this.inputNeurons.add( new NeuronWeight(inputNeuron, weight ) ) ;
	}
	

	public void lockOutput() {
		clock++ ;
		if( clock>=currentPotential.length ) clock=0 ; 
		currentPotential[clock] = futurePotential ;
	}
	
	public void clock() {		
		futurePotential = 0.0 ;
		for( NeuronWeight nw : inputNeurons ) {
			futurePotential += nw.getTransmittedPotential() ;
		}

		futurePotential *= this.membraneTransmissionFactor ;
	}
	
	public void futureClock() {		
		futurePotential = 0.0 ;
		for( NeuronWeight nw : inputNeurons ) {
			futurePotential += nw.getFutureTransmittedPotential() ;
		}

		futurePotential *= this.membraneTransmissionFactor ;
	}
	
	
	public double getFuturePotential() { return futurePotential ; }
	public double getPotential() { return currentPotential[clock] ; }
	public void setPotential(double potential) { this.futurePotential = potential; lockOutput(); }
	
	public int size() { return inputNeurons.size() ; } 
	public Iterator<NeuronWeight> iterator() { return this.inputNeurons.iterator() ; }

	public double getMembraneTransmissionFactor() {	return membraneTransmissionFactor; }
	public void adjustMembraneTransmissionFactor(double membraneTransmissionFactorFactor ) {
		this.membraneTransmissionFactor += membraneTransmissionFactorFactor;
		if( this.membraneTransmissionFactor <= -2.0 ) {
			this.membraneTransmissionFactor = -2.0 ;
		}
		if( this.membraneTransmissionFactor >= 2.0 ) {
			this.membraneTransmissionFactor = 2.0 ;
		}
		if( Math.abs(this.membraneTransmissionFactor) < 0.00001  ) {
			this.membraneTransmissionFactor = 0.0001 * Math.signum(0.0000001 + this.membraneTransmissionFactor);
		}
	}
	


	public int getIndexInBrain() { return indexInBrain; }
	
	public NeuronType getType() { return type; }
	public void setType(NeuronType type) { this.type = type; }
	
	public String toString() { return "[" + String.valueOf(getIndexInBrain()) + "]" + String.valueOf( getPotential() ) ; } 
}


class NeuronWeight {
	private double membraneTransmissionFactor ;
	private Neuron neuron ;

	public NeuronWeight( Neuron neuron ) {
		this( neuron, 1.0 ) ;
	}
	
	public NeuronWeight( Neuron neuron, double membraneTransmissionFactor ) {
		this.neuron = neuron ;
		this.membraneTransmissionFactor  = membraneTransmissionFactor ;
	}
	
	public double getTransmittedPotential() {
		return neuron.getPotential() * membraneTransmissionFactor ;
	}

	public double getFutureTransmittedPotential() {
		return neuron.getFuturePotential() * membraneTransmissionFactor ;
	}

	public double getMembraneTransmissionFactor() {
		return membraneTransmissionFactor;
	}
	public void setMembraneTransmissionFactor( double membraneTransmissionFactor ) {
		this.membraneTransmissionFactor = membraneTransmissionFactor ;
	}
	public void adjustMembraneTransmissionFactor(double membraneTransmissionFactorFactor ) {
		this.membraneTransmissionFactor += membraneTransmissionFactorFactor;
		if( this.membraneTransmissionFactor <= -2.0 ) {
			this.membraneTransmissionFactor = -2.0 ;
		}
		if( this.membraneTransmissionFactor >= 2.0 ) {
			this.membraneTransmissionFactor = 2.0 ;
		}
		if( Math.abs(this.membraneTransmissionFactor) < 0.00001  ) {
			this.membraneTransmissionFactor = 0.0001 * Math.signum(0.0000001 + this.membraneTransmissionFactor);
		}
	}

	public Neuron getNeuron() {
		return neuron;
	}
	public String toString() { return String.valueOf(neuron.getIndexInBrain()) +"->"+String.valueOf(membraneTransmissionFactor) ; } 

}


enum NeuronType {
	INPUT, OUTPUT, INTERNAL ;
}