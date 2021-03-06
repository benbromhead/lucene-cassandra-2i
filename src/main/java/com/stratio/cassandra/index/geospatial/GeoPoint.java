/*
 * Copyright 2015, Stratio.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stratio.cassandra.index.geospatial;

import com.google.common.base.Objects;
import com.spatial4j.core.context.SpatialContext;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Class representing a circle in geographical coordinates.
 *
 * @author Andres de la Pena <adelapena@stratio.com>
 */
public class GeoPoint extends GeoShape {

    private double longitude;
    private double latitude;

    /**
     * Builds a new {@link GeoPoint} defined by the specified longitude and latitude.
     *
     * @param longitude The longitude of the point to be built.
     * @param latitude  The latitude of the point to be built.
     */
    @JsonCreator
    public GeoPoint(@JsonProperty("longitude") double longitude, @JsonProperty("latitude") double latitude) {
        checkLongitude(longitude);
        checkLatitude(latitude);
        this.longitude = longitude;
        this.latitude = latitude;
    }

    /** {@inheritDoc} */
    @Override
    public com.spatial4j.core.shape.Shape toSpatial4j(SpatialContext spatialContext) {
        return spatialContext.makePoint(longitude, latitude);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Objects.toStringHelper(this).add("longitude", longitude).add("latitude", latitude).toString();
    }
}
