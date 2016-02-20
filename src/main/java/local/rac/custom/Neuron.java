package local.rac.custom;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Neuron implements Iterable<NeuronWeight> {
	
	final private List<NeuronWeight> inputNeurons ;	
	final private int indexInBrain ;
	
	private NeuronType type ;
	
	// Static data defining a neuron
	final private double activePotential ;
	final private double thresholdPotential ;	
	final private int repolarizationCount;
	final private int activationCount ;

	// Things that can be adjusted during learning
	private double membraneTransmissionFactor ;
	private double decayRate ;

	// Immediate attributes as a result of stimulii
	private double futurePotential ;
	private double currentPotential ;
	private int repolarizationClock ;
	private int activationClock ;
	
	public Neuron( int indexInBrain ) {
		
		this.indexInBrain = indexInBrain ;
		// These are to simulate a neuron. 
		
		// 		Potentials	(The resting potential is 0mV instead of -70mV)
		this.activePotential = 100 ;	// largest potential after triggering
		this.thresholdPotential = 15 ;	// threshold above which an activation spike occurs
		this.repolarizationCount = 50 ;	// number of clock before another spike can occur
		
		// 		Other Constants
		this.membraneTransmissionFactor = 0.65 ;	// input weight
		this.decayRate = 0.85 ;			// charge leaks at this rate 
		this.activationCount = 3 ;		// How long to wait before firing a spike

		// instance data
		this.currentPotential = 0 ;		// starting potentials
		this.futurePotential = 0 ;		
		
		this.inputNeurons = new ArrayList<NeuronWeight>() ;
		
		this.setType( NeuronType.INTERNAL ) ;
	}
	

	public void connectInhibitor( Neuron inputNeuron, double weight ) {
		this.inputNeurons.add( new NeuronWeight(inputNeuron, weight * -1.0) ) ;
	}
	
	public void connectExcitor( Neuron inputNeuron, double weight ) {
		this.inputNeurons.add( new NeuronWeight(inputNeuron, weight ) ) ;
	}
	

	public void clock() {
		currentPotential = futurePotential ;
		
		futurePotential *= decayRate ;
		
		// Can only receive stimuli when fully repolarized
		if( repolarizationClock==0 && activationClock==0 ) {
			double inputExcitement = 0.0 ;
			for( NeuronWeight inputNeuron : inputNeurons ) {
				inputExcitement += inputNeuron.getTransmittedPotential() ;
			}
	
			inputExcitement *= this.membraneTransmissionFactor ;
	
			futurePotential += inputExcitement ;		
			if( futurePotential<0.0 ) futurePotential = 0.0 ;
			
			if( futurePotential > thresholdPotential ) {	
				activationClock = activationCount ;
			}
		}
		if( activationClock > 0 ) { 
			if( --activationClock == 0 ) {
				repolarizationClock = repolarizationCount ; 
				futurePotential = activePotential ;
			}
		} 
		if( repolarizationClock > 0 ) { 
			repolarizationClock-- ;
		} 
	}
	
	
	public double getPotential() { return currentPotential ; }
	public void setPotential(double potential) { if( repolarizationClock==0 ) this.futurePotential = potential; }
	public int size() { return inputNeurons.size() ; } 
	public Iterator<NeuronWeight> iterator() { return this.inputNeurons.iterator() ; }


	public int getIndexInBrain() {
		return indexInBrain;
	}


	public NeuronType getType() {
		return type;
	}


	public void setType(NeuronType type) {
		this.type = type;
	}
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

	public double getMembraneTransmissionFactor() {
		return membraneTransmissionFactor;
	}

	public Neuron getNeuron() {
		return neuron;
	}
}


enum NeuronType {
	INPUT, OUTPUT, INTERNAL ;
}