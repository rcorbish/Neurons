package local.rac.custom;

public class InputNeuron extends Neuron {
	
	public InputNeuron( Brain brain, int indexInBrain ) {
		super( brain, indexInBrain ) ;
	}
	public void addInput( Axon axon ) { 
		// not supported for inputs to have other inputs		
	}
	public NeuronType getType() { return NeuronType.INPUT ; }
	
	public void clock() {
		// No need to do anything - an input neuron just has a constant potential
	}
}
