package local.rac.custom;

public class OutputNeuron extends Neuron {
	
	private int [] pathFromOutput ;
	
	public OutputNeuron( Brain brain, int indexInBrain ) {
		super( brain, indexInBrain ) ;
	}
	
	@Override
	public NeuronType getType() { return NeuronType.OUTPUT ; }
	public void setBrainPathway( int[] indexPath ) {
		this.pathFromOutput = indexPath ;
	}
	
	public void visitPathwayToOutput( NeuronVisitor visitor ) {
		for( int i=0 ; i<pathFromOutput.length ; i++ ) {
			visitor.visit(  getNeuronByIndex(pathFromOutput[i] ) );
		}
	}
	public void visitPathwayFromOutput( NeuronVisitor visitor ) {
		for( int i=pathFromOutput.length ; i>0 ; i-- ) {
			visitor.visit(  getNeuronByIndex(pathFromOutput[i-1] ) );
		}
	}
}
