package com.rc ;

public class Edge {
	public int source ;
	public double weight ;
	
	public Edge( int sourceIndex, double weight ) {
		this.source = sourceIndex ;
		this.weight = weight ;
	}
	
	public Edge( Genome genome ) {
		this.weight = genome.getDouble( 0 ) ;
	}
	
	public Genome toGenome() {
		Genome rc = new Genome( 1 ) ;
		rc.set( weight, 0 ) ;
		return rc ;
	}
}

