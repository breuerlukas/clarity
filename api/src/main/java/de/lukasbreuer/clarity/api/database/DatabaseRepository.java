package de.lukasbreuer.clarity.api.database;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.scheduling.annotation.Async;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

@NoRepositoryBean
public interface DatabaseRepository<T, ID extends Serializable>
  extends Repository<T, ID> {
  @Async
  CompletableFuture<Optional<T>> findById(ID id);

  @Async
  CompletableFuture<Boolean> existsById(ID id);

  @Async
  CompletableFuture<List<T>> findAll();

  @Async
  CompletableFuture<T> save(T entity);

  @Async
  CompletableFuture<List<T>> saveAll(Iterable<T> entities);

  @Async
  CompletableFuture<Void> deleteById(ID id);

  @Async
  CompletableFuture<Void> delete(T entity);

  default CompletableFuture<ID> generateAvailableId(Callable<ID> generator) {
    try {
      var futureResponse = new CompletableFuture<ID>();
      var id = generator.call();
      existsById(id).thenApply(exists -> exists ?
        generateAvailableId(generator).thenApply(futureResponse::complete) :
        CompletableFuture.completedFuture(futureResponse.complete(id)));
      return futureResponse;
    } catch (Exception exception) {
      return CompletableFuture.completedFuture(null);
    }
  }
}