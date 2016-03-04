package local.rac.custom;

public class OutputNeuron extends Neuron {

	public OutputNeuron( int indexInBrain ) {
		super( indexInBrain ) ;
	}

	public NeuronType getType() { return NeuronType.OUTPUT ; }
}
