package com.example.slf4jmdcdemo;

import reactor.core.publisher.Flux;
import reactor.core.publisher.ParallelFlux;

public interface FluxParalleliser {

    <T> ParallelFlux<T> parallelise(Flux<T> flux);
}
