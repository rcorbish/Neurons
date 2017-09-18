package com.rc ;

public class OutputNeuron extends Neuron {
	
	
	public OutputNeuron( Brain brain, int indexInBrain, BrainParameters parameters ) {
		super( brain, indexInBrain, parameters ) ;
	}
	
	@Override
	public NeuronType getType() { return NeuronType.OUTPUT ; }
}
