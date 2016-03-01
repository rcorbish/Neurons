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
	private double thresholdPotential ;	
	final private int repolarizationCount;
	final private int activationCount ;

	// Things that can be adjusted during learning
	private double membraneTransmissionFactor ;
	private double decayRate ;

	// Immediate attributes as a result of stimulii
	private double futurePotential ;
	private double [] currentPotential ;
	private int repolarizationClock ;
	private int activationClock ;
	
	private int clock ;
	
	public Neuron( int indexInBrain ) {
		
		this.indexInBrain = indexInBrain ;
		this.clock = 0 ;
		
		// 		Potentials	(The resting potential is 0mV instead of -70mV)
		this.activePotential = 100 ;	// largest potential after triggering
		this.thresholdPotential = 35 ;	// threshold above which an activation spike occurs
		this.repolarizationCount = 25 ;	// number of clock before another spike can occur
		
		// 		Other Constants
		this.membraneTransmissionFactor = 0.75 ;	// input weight
		this.decayRate = 0.30 ;						// charge leaks at this rate 
		this.activationCount = 2 ;					// How long to wait before firing a spike

		// instance data
		this.currentPotential = new double[100] ;		// starting potentials
		this.futurePotential = 0 ;		
		
		this.inputNeurons = new ArrayList<NeuronWeight>() ;
		
		this.setType( NeuronType.INTERNAL ) ;
	}
	

	public void connectNeuron( Neuron inputNeuron, double weight ) {
		this.inputNeurons.add( new NeuronWeight(inputNeuron, weight ) ) ;
	}
	

	public void clock() {
		clock++ ;
		if( clock>=currentPotential.length ) clock=0 ; 
		currentPotential[clock] = futurePotential ;
		
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
				futurePotential -= inputExcitement ;		
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
	
	
	public double getPotential() { return currentPotential[clock] ; }
	public void setPotential(double potential) { if( repolarizationClock==0 ) this.futurePotential = potential; }
	
	public int size() { return inputNeurons.size() ; } 
	public Iterator<NeuronWeight> iterator() { return this.inputNeurons.iterator() ; }

	public double getMembraneTransmissionFactor() {	return membraneTransmissionFactor; }
	public void adjustMembraneTransmissionFactor(double membraneTransmissionFactorFactor ) {
		this.membraneTransmissionFactor += membraneTransmissionFactorFactor ;
		if( this.membraneTransmissionFactor <= 0.1 ) {
			this.membraneTransmissionFactor = 0.1 ;
		}
		if( this.membraneTransmissionFactor >= 0.9 ) {
			this.membraneTransmissionFactor = 0.9 ;
		}
	}
	
	public double getDecayRate() { return decayRate; }
	public void adjustDecayRate(double decayRateFactor ) { 
		this.decayRate += decayRateFactor ;
		if( this.decayRate <= 0.1 ) {
			this.decayRate = 0.1 ;
		}
		if( this.decayRate >= 1.0 ) {
			this.decayRate = 1.0 ;
		}
	}
	public void adjustThreshold( double thresholdFactor ) { 
		this.thresholdPotential += thresholdFactor ;
		if( this.thresholdPotential <= 10.0 ) {
			this.thresholdPotential = 10.0 ;
		}
		if( this.thresholdPotential >= 75.0 ) {
			this.thresholdPotential = 75.0 ;
		}
	}

	public int getIndexInBrain() { return indexInBrain; }
	
	public NeuronType getType() { return type; }
	public void setType(NeuronType type) { this.type = type; }
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
	public void adjustMembraneTransmissionFactor(double membraneTransmissionFactorFactor ) {
		this.membraneTransmissionFactor += membraneTransmissionFactorFactor;
		if( this.membraneTransmissionFactor <= 0.1 ) {
			this.membraneTransmissionFactor = 0.1 ;
		}
		if( this.membraneTransmissionFactor >= 1.0 ) {
			this.membraneTransmissionFactor = 1.0 ;
		}
	}

	public Neuron getNeuron() {
		return neuron;
	}
}


enum NeuronType {
	INPUT, OUTPUT, INTERNAL ;
}