package org.texttechnologylab.uce.common.services;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

public final class SparqlChunkParallelizer {
    @FunctionalInterface
    public interface ChunkTask<I, O> {
        O apply(I input) throws IOException;
    }

    private final ExecutorService executor;
    private final int maxInFlight;

    public SparqlChunkParallelizer(ExecutorService executor, int maxInFlight) {
        this.executor = executor;
        this.maxInFlight = Math.max(1, maxInFlight);
    }

    public <I, O> List<O> runAll(List<I> chunks, ChunkTask<I, O> task) throws IOException {
        if (chunks == null || chunks.isEmpty()) return List.of();

        var semaphore = new Semaphore(maxInFlight);
        var futures = new ArrayList<CompletableFuture<O>>(chunks.size());

        for (var chunk : chunks) {
            var future = CompletableFuture.supplyAsync(() -> {
                try {
                    semaphore.acquire();
                    try {
                        return task.apply(chunk);
                    } finally {
                        semaphore.release();
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new CompletionException(ex);
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            }, executor);
            futures.add(future);
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            var results = new ArrayList<O>(futures.size());
            for (var future : futures) {
                results.add(future.join());
            }
            return results;
        } catch (CompletionException ex) {
            if (ex.getCause() instanceof UncheckedIOException uio) {
                throw uio.getCause();
            }
            if (ex.getCause() instanceof InterruptedException iex) {
                Thread.currentThread().interrupt();
                throw new IOException("Parallel SPARQL execution interrupted.", iex);
            }
            throw ex;
        }
    }
}
