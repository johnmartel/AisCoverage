package dk.dma.ais.coverage.persistence;

import java.time.ZonedDateTime;
import java.util.List;

import dk.dma.ais.coverage.data.Cell;

/**
 * Implementations are responsible for marshalling and unmarshalling coverage data to and from a database typing system.
 */
interface CoverageDataMarshaller<T> {

    T marshall(List<Cell> coverageData, ZonedDateTime dataTimestamp);

    List<Cell> unmarshall(T coverageData);
}
