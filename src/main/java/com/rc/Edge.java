package com.rc ;

public class Edge {
	static public int GENOME_SIZE = 5 ;
	
	private final int id ;
	private final int source ;
	private final int target ;
	private double weight ;
	
	public Edge( int sourceIndex, int targetIndex, double weight, int id ) {
		this.target = targetIndex ;
		this.source = sourceIndex ;
		this.weight = weight ;
		this.id = id ;
	}
	
	public Edge( Genome genome ) {
		this.source = genome.getInt( 0 ) ;
		this.target = genome.getInt( 1 ) ;
		this.id = genome.getInt( 2 ) ;
		this.weight = genome.getDouble( 3 ) * ( genome.getInt( 4 )==0 ? -1 : 1 ) ;
	}
	
	public Genome toGenome() {
		Genome rc = new Genome() ;
		rc.set( source, 0 ) ;
		rc.set( target, 1 ) ;
		rc.set( id, 2 ) ;
		rc.set( Math.abs(weight), 3 ) ;
		int s = Math.signum(weight) < 0 ? 0 : 1 ;
		rc.set( s, 4 ) ;
		return rc ;
	}
	
	public int source() {
		return source ;
	}
	public int target() {
		return target ;
	}
	public double weight() {
		return weight ;
	}
	public int id() {
		return id ;
	}
	public void addWeight( double addition ) {
		double factor = addition > 0 ? (0.85 - weight ) : ( weight + 0.85 ) ; 
		weight += factor * addition ;
		if( weight > 0.85 ) weight = 0.85 ;
		if( weight < -0.85 ) weight = -0.85 ;
	}
}

