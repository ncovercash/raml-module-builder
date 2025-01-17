package org.folio.rest.persist;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.sqlclient.PreparedStatement;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowStream;
import io.vertx.sqlclient.Tuple;

/**
 * Extend a RowStream<Row> with a result {@link Future} that completes after
 * endHandler or exceptionHandler has been called.
 */
public class PreparedRowStream implements RowStream<Row> {
  private final PreparedStatement preparedStatement;
  private final RowStream<Row> rowStream;
  private Promise<Void> result = Promise.promise();
  private Handler<Throwable> exceptionHandler;
  private Handler<Void> endHandler;

  public PreparedRowStream(RowStream<Row> rowStream) {
    this(null, rowStream);
  }

  /**
   * Constructor with PreparedStatement and RowStream<Row>.
   *
   * @param preparedStatement the query to execute, it is closed when this PreparedRowStream is closed
   */
  public PreparedRowStream(PreparedStatement preparedStatement, RowStream<Row> rowStream) {
    this.preparedStatement = preparedStatement;
    this.rowStream = rowStream;
    rowStream.exceptionHandler(t -> {
      if (exceptionHandler != null) {
        exceptionHandler.handle(t);
      }
      result.tryFail(t);
    });
    rowStream.endHandler(end -> {
      if (endHandler != null) {
        endHandler.handle(end);
      }
      result.tryComplete();
    });
  }

  /**
   * Create a RowStream<Row> using the preparedStatement, fetch and tuple.
   *
   * @param preparedStatement the query to execute, it is closed when this PreparedRowStream is closed
   * @param fetch the cursor fetch size
   * @param tuple the arguments for preparedStatement
   */
  public PreparedRowStream(PreparedStatement preparedStatement, int fetch, Tuple tuple) {
    this(preparedStatement, preparedStatement.createStream(fetch, tuple));
  }

  /**
   * Returns a Future that succeeds after {@link RowStream} has called its {@code endHandler}
   * and fails after {@link RowStream} has called its {@code exceptionHandler}.
   */
  Future<Void> getResult() {
    return result.future();
  }

  @Override
  public RowStream<Row> fetch(long amount) {
    return rowStream.fetch(amount);
  }

  @Override
  public RowStream<Row> exceptionHandler(Handler<Throwable> exceptionHandler) {
    this.exceptionHandler = exceptionHandler;
    return this;
  }

  @Override
  public RowStream<Row> handler(Handler<Row> handler) {
    rowStream.handler(handler);
    return this;
  }

  @Override
  public RowStream<Row> pause() {
    rowStream.pause();
    return this;
  }

  @Override
  public RowStream<Row> resume() {
    rowStream.resume();
    return this;
  }

  @Override
  public RowStream<Row> endHandler(Handler<Void> endHandler) {
    this.endHandler = endHandler;
    return this;
  }

  /**
   * Close the RowStream and the PreparedStatement. Each PreparedStatement occupies
   * memory in the database that needs to be freed to avoid out-of-memory error.
   */
  @Override
  public Future<Void> close() {
    return rowStream.close().eventually(x -> {
      if (preparedStatement == null) {
        return Future.succeededFuture();
      }
      return preparedStatement.close();
    });
  }

  /**
   * Close the RowStream and the PreparedStatement. Each PreparedStatement occupies
   * memory in the database that needs to be freed to avoid out-of-memory error.
   */
  @Override
  public void close(Handler<AsyncResult<Void>> completionHandler) {
    close().onComplete(completionHandler);
  }
}
