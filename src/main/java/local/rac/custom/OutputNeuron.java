package local.rac.custom;

public class OutputNeuron extends Neuron {

	public OutputNeuron( Brain brain, int indexInBrain ) {
		super( brain, indexInBrain ) ;
	}

	public NeuronType getType() { return NeuronType.OUTPUT ; }
}
