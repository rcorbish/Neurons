package com.rc.neurons ;

import java.lang.reflect.Constructor;

public enum NeuronType {
    CH( "chattering", NeuronCH.class ),
    FS( "fast spiking", NeuronFS.class ),
    IB( "inhibitor", NeuronIB.class ),
    LTS( "long term spiking", NeuronLTS.class ),
    RS( "regular spiking", NeuronRS.class ),
    RZ( "resonator", NeuronRZ.class ),
    TC( "thalmo-cortical", NeuronTC.class ),
    IN( "input", InputNeuron.class )
    ;

    private final Class<? extends Neuron> clazz ;
    private final String friendlyName ;
    private final Constructor<? extends Neuron> constructor ;

    private NeuronType( String friendlyName, Class<? extends Neuron> clazz ) {
        this.friendlyName = friendlyName ;
        this.clazz = clazz ;
        Constructor<? extends Neuron> tmpConstructor = null ;
        try {
            tmpConstructor = clazz.getConstructor(int.class);
        } catch (Exception e ) {
        }
        this.constructor = tmpConstructor ;
    }

    public String toString() {
        return friendlyName ;
    }

    public Neuron create( int id ) throws Exception {
        return constructor.newInstance( id ) ;
    }
}