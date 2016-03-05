package local.rac.custom;

public class InputNeuron extends Neuron {
	
	public InputNeuron( Brain brain, int indexInBrain ) {
		super( brain, indexInBrain ) ;
	}
	
	@Override
	public void addInput( Axon axon ) { 
		// not supported for inputs to have other inputs		
	}
	@Override
	public NeuronType getType() { return NeuronType.INPUT ; }
	
	@Override
	public void updatePotential() {
		// No need to do anything - an input neuron just has a constant potential
	}
}
