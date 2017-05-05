/*
 Copyright (c) 2017, Stephen Gold
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Neither the name of the copyright holder nor the names of its contributors
 may be used to endorse or promote products derived from this software without
 specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package maud;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Edit history for Maud.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class History {
    // *************************************************************************
    // constants and loggers

    /**
     * message logger for this class
     */
    final public static Logger logger = Logger.getLogger(
            History.class.getName());
    // *************************************************************************
    // fields

    /**
     * index of the next slot to use
     */
    private static int nextIndex = 0;
    /**
     * list of checkpoint slots
     */
    final private static List<Checkpoint> checkpoints = new ArrayList<>(20);
    // *************************************************************************
    // new methods exposed

    /**
     * Create a checkpoint in the next slot, discarding any checkpoints beyond
     * that slot so that the new one is also the last.
     */
    static void add() {
        while (hasVulnerable()) {
            int lastIndex = checkpoints.size() - 1;
            checkpoints.remove(lastIndex);
            logger.log(Level.INFO, "discard [{0}]", nextIndex);
        }

        Checkpoint newbie = new Checkpoint();
        checkpoints.add(newbie);
        logger.log(Level.INFO, "add [{0}]", nextIndex);
        nextIndex++;

        assert checkpoints.size() == nextIndex;
    }

    /**
     * Discard all checkpoints.
     */
    static void clear() {
        checkpoints.clear();
        nextIndex = 0;
    }

    /**
     * Read the index of the next slot to use.
     *
     * @return index (&ge;0)
     */
    static int getNextIndex() {
        return nextIndex;
    }

    /**
     * Test whether any checkpoints would be discarded by {@link #add()}.
     *
     * @return true if some checkpoints are vulnerable, otherwise false
     */
    static boolean hasVulnerable() {
        int numVulnerable = checkpoints.size() - nextIndex;
        assert numVulnerable >= 0 : numVulnerable;
        if (numVulnerable > 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Count the available checkpoints.
     *
     * @return count (&ge;0)
     */
    static int length() {
        int count = checkpoints.size();
        return count;
    }

    /**
     * If the next slot has a checkpoint, restore the checkpoint and increment
     * the index.
     */
    static void redo() {
        if (hasVulnerable()) {
            Checkpoint next = checkpoints.get(nextIndex);
            next.restore();
            logger.log(Level.INFO, "redo to [{0}]", nextIndex);
            nextIndex++;
        } else {
            logger.log(Level.INFO, "nothing to redo", nextIndex);
        }
    }

    /**
     * If a previous slot exists, restore its checkpoint and decrement the
     * index. If there are no vulnerable checkpoints, add one.
     */
    static void undo() {
        if (nextIndex > 0) {
            if (!hasVulnerable()) {
                Checkpoint newbie = new Checkpoint();
                checkpoints.add(newbie);
                logger.log(Level.INFO, "add [{0}]", nextIndex);
            }
            nextIndex--;
            Checkpoint previous = checkpoints.get(nextIndex);
            previous.restore();
            logger.log(Level.INFO, "undo to [{0}]", nextIndex);
        } else {
            logger.log(Level.INFO, "nothing to undo", nextIndex);
        }
    }
}
