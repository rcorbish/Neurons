package com.rc ;

public class Edge {
	static public int GENOME_SIZE = 3 ;

	private int source ;
	private double weight ;
	
	public Edge( int sourceIndex, double weight ) {
		this.source = sourceIndex ;
		this.weight = weight ;
	}
	
	public Edge( Genome genome ) {
		this.source = genome.getInt( 0 ) ;
		this.weight = genome.getDouble( 1 ) * ( genome.getInt( 2 )==0 ? -1 : 1 ) ;
	}
	
	public Genome toGenome() {
		Genome rc = new Genome() ;
		rc.set( source, 0 ) ;
		rc.set( Math.abs(weight), 1 ) ;
		int s = Math.signum(weight) < 0 ? 0 : 1 ;
		rc.set( s, 2 ) ;
		return rc ;
	}
	
	public int source() {
		return source ;
	}
	public double weight() {
		return weight ;
	}
	public void addWeight( double addition ) {
		weight += addition ;
		if( weight > 0.85 ) weight = 0.85 ;
		if( weight < -0.85 ) weight = -0.85 ;
	}
}

