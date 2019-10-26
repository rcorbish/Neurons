package com.rc.neurons ;

import org.ejml.data.DMatrixSparseCSC;

import com.rc.Brain;


public class OutputNeuron extends NeuronFS {
	private boolean supervisedTestingFire ;
	private boolean testingMode ;

	public OutputNeuron( int id ) {		
		super( id ) ;
		testingMode = false ;
		supervisedTestingFire = false ;
	}

	public boolean isTesting() { return testingMode ; }
	public void setTesting( boolean isTest ) { testingMode = isTest ; }
	public void setSupervisedFiring( boolean isFire ) { supervisedTestingFire = isFire ; }


	@Override
	public void train( Brain brain, DMatrixSparseCSC training  ) { 
		if( isSpiking() ) {
			Neuron sources[] = brain.getInputsTo( id ) ;
			for( Neuron src : sources ) {
				double dt = supervisedTestingFire ? 0.001 : -0.001 ;
				if( dt <= 0 && -dt < learningWindowLTD  ) {
					double dw = learningRate * Math.exp( -dt / learningRateTauLTD ) ;
					brain.addTraining( src.id, id, -dw ) ;
				} else if ( dt>0 && dt < learningWindowLTP  ) {
					double dw = learningRate * Math.exp( dt / learningRateTauLTP ) ;
					brain.addTraining( src.id, id, dw ) ;
				}
			}
		}
	}

	@Override
	public NeuronType getType() {
		return NeuronType.OUT ;
	}
}


