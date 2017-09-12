package com.rc ;

public class OutputNeuron extends Neuron {
	
	
	public OutputNeuron( Brain brain, int indexInBrain ) {
		super( brain, indexInBrain ) ;
	}
	
	@Override
	public NeuronType getType() { return NeuronType.OUTPUT ; }
}
