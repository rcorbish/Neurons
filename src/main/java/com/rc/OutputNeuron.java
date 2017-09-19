package com.rc ;

public class OutputNeuron extends Neuron {
	
	private final double outputHistory[] ;
	private int historyIndex = 0 ;

	public OutputNeuron( Brain brain, int indexInBrain, BrainParameters parameters ) {
		super( brain, indexInBrain, parameters ) ;
		outputHistory = new double[Brain.HISTORY_LENGTH] ;
	}

	@Override
	public void clock() {
		super.clock(); 
		setPotential( getFuturePotential() )  ;
		this.outputHistory[historyIndex] = getPotential() ;
		historyIndex++ ;
		if( historyIndex >= outputHistory.length ) {
			historyIndex = 0 ;
		}
	}

	public double getHistory( int stepsBefore ) {
		int step = historyIndex - stepsBefore ;
		if( step < 0 ) {
			step += outputHistory.length ;
		}
		return outputHistory[step] ;
	}
	@Override
	public NeuronType getType() { return NeuronType.OUTPUT ; }
}
