package local.rac.custom;

import java.util.Random;

public class EvolutionRules {
	Random rng ;

	public EvolutionRules() {
		rng = new Random( 125 ) ;
	}

	
	public void evolve( double error, Neuron neuron, boolean []visitedNeuronIndex ) {
		
		if( visitedNeuronIndex[neuron.getIndexInBrain()] ) {
			return ;
		}
		visitedNeuronIndex[neuron.getIndexInBrain()] = true ;

		if( error > 0 ) {  // output is too small - increase it 
			neuron.adjustDecayRate( rng.nextDouble() / 100.0 ) ;
			neuron.adjustMembraneTransmissionFactor( -rng.nextDouble() / 100.0 ) ;			
			neuron.adjustThreshold( -rng.nextDouble() / 100.0 ) ;			
			for( NeuronWeight nw : neuron ) {
				nw.adjustMembraneTransmissionFactor( -rng.nextDouble() / 100.0 ) ;
				evolve( error, nw.getNeuron(), visitedNeuronIndex ) ;
			}			
		} else {
			neuron.adjustDecayRate( -rng.nextDouble() / 100.0 ) ;
			neuron.adjustMembraneTransmissionFactor( rng.nextDouble() / 100.0 ) ;			
			neuron.adjustThreshold( rng.nextDouble() / 100.0 ) ;			
			for( NeuronWeight nw : neuron ) {
				nw.adjustMembraneTransmissionFactor( rng.nextDouble() / 100.0 ) ;
				evolve( error, nw.getNeuron(), visitedNeuronIndex ) ;
			}
		}
	}
}
