/* Copyright (c) 2011 Danish Maritime Authority.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dk.dma.ais.coverage.data;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TimeSpan {
    private Map<String, Boolean> distinctShipsSat = new ConcurrentHashMap<String, Boolean>();
    private Map<String, Boolean> distinctShipsTerrestrial = new ConcurrentHashMap<String, Boolean>();
    private long firstMessage, lastMessage;
    private int messageCounterSat;
    private int messageCounterTerrestrial;
    private int missingSignals;
    private int messageCounterTerrestrialUnfiltered;
    private int vsiMessageCounter;
    private int averageSignalStrength;

    public int getMessageCounterTerrestrialUnfiltered() {
        return messageCounterTerrestrialUnfiltered;
    }

    public void incrementMessageCounterTerrestrialUnfiltered() {
        this.messageCounterTerrestrialUnfiltered++;
    }

    public void setMessageCounterTerrestrialUnfiltered(int number) {
        this.messageCounterTerrestrialUnfiltered = number;
    }

    public void addMessageCounterTerrestrialUnfiltered(int number) {
        this.messageCounterTerrestrialUnfiltered += number;
    }

    public void incrementMissingSignals() {
        missingSignals++;
    }

    public int getMissingSignals() {
        return missingSignals;
    }

    public void setMissingSignals(int missingSignals) {
        this.missingSignals = missingSignals;
    }

    public Map<String, Boolean> getDistinctShipsTerrestrial() {
        return distinctShipsTerrestrial;
    }

    public int getMessageCounterTerrestrial() {
        return messageCounterTerrestrial;
    }

    public void setMessageCounterTerrestrial(int messageCounterTerrestrial) {
        this.messageCounterTerrestrial = messageCounterTerrestrial;
    }

    public TimeSpan(Date firstMessage) {
        this.firstMessage = firstMessage.getTime();
        this.lastMessage = firstMessage.getTime();
    }

    public Map<String, Boolean> getDistinctShipsSat() {
        return distinctShipsSat;
    }

    public Date getFirstMessage() {
        return new Date(firstMessage);
    }

    public void setFirstMessage(Date firstMessage) {
        this.firstMessage = firstMessage.getTime();
    }

    public Date getLastMessage() {
        return new Date(lastMessage);
    }

    public void setLastMessage(Date lastMessage) {
        this.lastMessage = lastMessage.getTime();
    }

    public int getMessageCounterSat() {
        return messageCounterSat;
    }

    public void setMessageCounterSat(int messageCounter) {
        this.messageCounterSat = messageCounter;
    }

    public int getVsiMessageCounter() {
        return vsiMessageCounter;
    }

    public void setVsiMessageCounter(int vsiMessageCounter) {
        this.vsiMessageCounter = vsiMessageCounter;
    }

    public int getAverageSignalStrength() {
        return averageSignalStrength;
    }

    public void setAverageSignalStrength(int averageSignalStrength) {
        this.averageSignalStrength = averageSignalStrength;
    }

    public synchronized void incrementNumberOfVsiMessages(int signalStrength) {
        int incrementedNumberOfVsiMessages = vsiMessageCounter + 1;
        averageSignalStrength = computeAverageSignalStrength(signalStrength, incrementedNumberOfVsiMessages);
        vsiMessageCounter = incrementedNumberOfVsiMessages;
    }

    private int computeAverageSignalStrength(int signalStrength, int incrementedNumberOfVsiMessages) {
        return Math.floorDiv((vsiMessageCounter * averageSignalStrength) + signalStrength, incrementedNumberOfVsiMessages);
    }

    public void add(TimeSpan other) {
        this.setMessageCounterSat(this.getMessageCounterSat() + other.getMessageCounterSat());
        this.setMessageCounterTerrestrial(this.getMessageCounterTerrestrial() + other.getMessageCounterTerrestrial());
        this.addMessageCounterTerrestrialUnfiltered(other.getMessageCounterTerrestrialUnfiltered());
        this.setAverageSignalStrength(sumAverageSignalStrength(other));
        this.setVsiMessageCounter(this.getVsiMessageCounter() + other.getVsiMessageCounter());
        for (String s : other.distinctShipsSat.keySet()) {
            this.distinctShipsSat.put(s, true);
        }
        for (String s : other.distinctShipsTerrestrial.keySet()) {
            this.distinctShipsTerrestrial.put(s, true);
        }
    }

    private int sumAverageSignalStrength(TimeSpan other) {
        int thisAggregatedSignalStrength = this.getVsiMessageCounter() * this.getAverageSignalStrength();
        int otherAggregatedSignalStrength = other.getVsiMessageCounter() * other.getAverageSignalStrength();
        int summedVsiMessageCounters = this.getVsiMessageCounter() + other.getVsiMessageCounter();
        return Math.floorDiv(thisAggregatedSignalStrength + otherAggregatedSignalStrength, summedVsiMessageCounters);
    }

    public TimeSpan copy() {
        TimeSpan copy = new TimeSpan(this.getFirstMessage());
        copy.setLastMessage(this.getLastMessage());
        copy.setMessageCounterSat(this.getMessageCounterSat());
        copy.setMessageCounterTerrestrial(this.getMessageCounterTerrestrial());
        copy.setMessageCounterTerrestrialUnfiltered(this.messageCounterTerrestrialUnfiltered);
        copy.setVsiMessageCounter(this.getVsiMessageCounter());
        copy.setAverageSignalStrength(this.getAverageSignalStrength());
        for (String s : this.distinctShipsSat.keySet()) {
            copy.distinctShipsSat.put(s, true);
        }
        for (String s : this.distinctShipsTerrestrial.keySet()) {
            copy.distinctShipsTerrestrial.put(s, true);
        }
        return copy;
    }
}
