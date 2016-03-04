package local.rac.custom;

public class Axon {
	private double membraneTransmissionFactor ;
	private Neuron neuron ;

	public Axon( Neuron neuron ) {
		this( neuron, 1.0 ) ;
	}
	
	public Axon( Neuron neuron, double membraneTransmissionFactor ) {
		this.neuron = neuron ;
		this.membraneTransmissionFactor  = membraneTransmissionFactor ;
	}
	
	public double getTransmittedPotential() {
		return neuron.getPotential() * membraneTransmissionFactor ;
	}

	public double getMembraneTransmissionFactor() {
		return membraneTransmissionFactor;
	}
	public void setMembraneTransmissionFactor( double membraneTransmissionFactor ) {
		this.membraneTransmissionFactor = membraneTransmissionFactor ;
	}
	public void adjustMembraneTransmissionFactor(double membraneTransmissionFactorFactor ) {
		this.membraneTransmissionFactor += membraneTransmissionFactorFactor;
		if( this.membraneTransmissionFactor <= -1.0 ) {
			this.membraneTransmissionFactor = -1.0 ;
		}
		if( this.membraneTransmissionFactor >= 1.0 ) {
			this.membraneTransmissionFactor = 1.0 ;
		}
	}

	public Neuron getNeuron() {
		return neuron;
	}
	public String toString() { return String.valueOf(neuron.getIndexInBrain()) +"->"+String.valueOf(membraneTransmissionFactor) ; } 

}

