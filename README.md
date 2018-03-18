# Neurons

## Spiking Neural Network

This implements a SNN with [spike timing dependent plasticity](https://en.wikipedia.org/wiki/Spike-timing-dependent_plasticity) learning. 

There's a lot of work needed, but I wanted to get something working. A lot of the research
is hard to reproduce.

### Building

Checkout then use gradle to build

```gradle build```

or maven

```mvn build```


### Running

```./run.sh```


open a browser to the server address to see the network

[open this link](http://localhost:8111)

* click on a node to see its waveform output
* select *train* to start training
* change the input pattern ( top left input ) from 0 to 8

#### Command line options

Use --help to see all active options 