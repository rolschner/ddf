/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.catalog.source;

/**
 * Describes a source that is created by a factory, and allows the source
 *  to be aware of its configuration pid for reliable matching between 
 *  configuration and source.
 */
public interface ConfiguredSource {

    /**
     * Sets the PID of this source's corresponding configuration
     * 
     * @param pid The unique PID of the configuration that corresponds to this
     *  source
     */
    public void setConfigurationPid(String pid);

    /**
     * Returns the PID of this source's corresponding configuration
     * 
     * @return The unique PID of the configuration that corresponds to this
     *  source
     */
    public String getConfigurationPid();

}
