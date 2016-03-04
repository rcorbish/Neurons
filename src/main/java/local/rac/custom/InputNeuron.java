package local.rac.custom;

public class InputNeuron extends Neuron {

	public InputNeuron( int indexInBrain ) {
		super( indexInBrain ) ;
	}
	public void addInput( Axon axon ) { 
		// not supported for inputs to have other inputs		
	}
	public NeuronType getType() { return NeuronType.INPUT ; }
}
