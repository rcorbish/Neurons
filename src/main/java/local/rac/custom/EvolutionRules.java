package local.rac.custom;

import java.util.Random;

public class EvolutionRules {
	Random rng ;

	public EvolutionRules() {
		rng = new Random( 125 ) ;
	}


	public void evolve( double error, Neuron neuron, boolean []visitedNeuronIndex ) {

		if( visitedNeuronIndex[neuron.getIndexInBrain()] || ( Math.abs(error) < 0.0001 ) ) {
			return ;
		}
		visitedNeuronIndex[neuron.getIndexInBrain()] = true ;

		for( Axon nw : neuron ) {
			nw.adjustMembraneTransmissionFactor( error * rng.nextDouble() / 100000.0 ) ;
			evolve( error * nw.getMembraneTransmissionFactor(), nw.getNeuron(), visitedNeuronIndex ) ;
		}			
	}
}
