package dk.dma.ais.coverage.persistence;

import dk.dma.ais.coverage.data.ICoverageData;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

public class PersisterServiceTest {

    @Test
    public void whenStart_thenSaveIsInvokedOnDatabaseInstanceAtInterval() throws InterruptedException {
        DatabaseInstance databaseInstance = mock(DatabaseInstance.class);
        when(databaseInstance.save(anyMap())).thenReturn(PersistenceResult.success(1));
        ICoverageData coverageData = mock(ICoverageData.class);
        PersisterService persisterService = new PersisterService(databaseInstance, coverageData);
        ScheduledExecutorService executor = new RunImmediatelyFiveTimesExecutorServive();
        persisterService.setExecutor(executor);

        persisterService.start();
        persisterService.stop();

        verify(databaseInstance, times(5)).save(anyMap());
    }

    @Test
    public void whenNewInstance_thenIntervalInMinutesDefaultsTo60() {
        DatabaseInstance databaseInstance = mock(DatabaseInstance.class);
        ICoverageData coverageData = mock(ICoverageData.class);
        PersisterService persisterService = new PersisterService(databaseInstance, coverageData);

        assertThat(persisterService.getIntervalInMinutes(), is(equalTo(60L)));
    }

    private static class RunImmediatelyFiveTimesExecutorServive implements ScheduledExecutorService {
        private static final String NOT_IMPLEMENTED = "Not implemented";

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            throw new NotImplementedException(NOT_IMPLEMENTED);
        }

        @Override
        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            throw new NotImplementedException(NOT_IMPLEMENTED);
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            for (int i = 0; i < 5; i++) {
                command.run();
            }
            return null;
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
            throw new NotImplementedException(NOT_IMPLEMENTED);
        }

        @Override
        public void shutdown() {

        }

        @Override
        public List<Runnable> shutdownNow() {
            return null;
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return false;
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            throw new NotImplementedException(NOT_IMPLEMENTED);
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            throw new NotImplementedException(NOT_IMPLEMENTED);
        }

        @Override
        public Future<?> submit(Runnable task) {
            throw new NotImplementedException(NOT_IMPLEMENTED);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            throw new NotImplementedException(NOT_IMPLEMENTED);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
            throw new NotImplementedException(NOT_IMPLEMENTED);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            throw new NotImplementedException(NOT_IMPLEMENTED);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            throw new NotImplementedException(NOT_IMPLEMENTED);
        }

        @Override
        public void execute(Runnable command) {
            throw new NotImplementedException(NOT_IMPLEMENTED);
        }
    }
}
