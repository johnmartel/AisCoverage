package dk.dma.ais.coverage.persistence;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Map;

import dk.dma.ais.coverage.data.Cell;

/**
 * Implementations are responsible for marshalling and unmarshalling coverage data to and from a database typing system.
 */
interface CoverageDataMarshaller<T> {

    T marshall(Map<String, Collection<Cell>> coverageData, ZonedDateTime dataTimestamp);

    Map<String, Collection<Cell>> unmarshall(T coverageData);
}
