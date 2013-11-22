/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.catalog.util.impl;

import java.util.Comparator;

import org.apache.log4j.Logger;
import org.opengis.filter.sort.SortOrder;

import ddf.catalog.data.Result;
import ddf.catalog.util.DistanceResultComparator;

/**
 * Comparator for the distance (in meters) of 2 {@link Result} objects.
 * 
 * @author ddf.isgs@lmco.com
 */
public class DistanceResultComparator implements Comparator<Result> {

    private static Logger logger = Logger.getLogger(DistanceResultComparator.class);

    private SortOrder distanceOrder;

    /**
     * Constructs the comparator with the specified sort order, either distance ascending or
     * distance descending.
     * 
     * @param distanceOrder
     *            the distance sort order
     */
    public DistanceResultComparator(SortOrder distanceOrder) {
        this.distanceOrder = distanceOrder;
    }

    /**
     * Compares the distance (in meters) between the two results.
     * 
     * @return 1 if A is null and B is non-null -1 if A is non-null and B is null 0 if both A and B
     *         are null 1 if ascending sort order and A > B; -1 if ascending sort order and B > A -1
     *         if descending sort order and A > B; 1 if descending sort order and B > A
     */
    @Override
    public int compare(Result contentA, Result contentB) {
        if (contentA != null && contentB != null) {

            Double distanceA = contentA.getDistanceInMeters();
            Double distanceB = contentB.getDistanceInMeters();

            if (distanceA == null && distanceB != null) {
                logger.debug("distanceA is null and distanceB is not null: " + distanceB);
                return 1;
            } else if (distanceA != null && distanceB == null) {
                logger.debug("distanceA is not null: " + distanceA + " and distanceB is null");
                return -1;
            } else if (distanceA == null && distanceB == null) {
                logger.debug("both are null");
                return 0;
            } else if (SortOrder.ASCENDING.equals(distanceOrder)) {
                logger.debug("Ascending sort");
                return distanceA.compareTo(distanceB);
            } else if (SortOrder.DESCENDING.equals(distanceOrder)) {
                logger.debug("Descending sort");
                return distanceB.compareTo(distanceA);
            } else {
                logger.warn("Unknown order type. Returning 0.");
                return 0;
            }

        } else {
            logger.warn("Error comparing results, at least one was null.  Returning -1: ");
            return -1;
        }
    }

}
