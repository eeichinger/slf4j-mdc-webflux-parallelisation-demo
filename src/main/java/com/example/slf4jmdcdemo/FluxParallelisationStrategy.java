package com.example.slf4jmdcdemo;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.function.Function;

/**
 * Encapsulates the strategy how to process an input flux in parallel
 *
 * e.g.
 *
 * <code>flux
*      .parallel()
*      .runOn(scheduler, parallelRails)
*      .flatMap(mapper)
*      .sequential();
 * </code>
 */
@FunctionalInterface
public interface FluxParallelisationStrategy<I,O> {

    static <I,O> FluxParallelisationStrategy<I,O> IDENTITY_PARALLELISER() { return  (ids, mapper) -> ids.flatMap(mapper); }

    Flux<O> parallelise(Flux<I> flux, Function<I, Publisher<O>> mapper);
}
