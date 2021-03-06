/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.influxdb;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;
import org.influxdb.InfluxDB;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Producer for the InfluxDB components
 *
 */
public class InfluxDbProducer extends DefaultProducer {

    private static final Logger LOG = LoggerFactory.getLogger(InfluxDbProducer.class);

    InfluxDbEndpoint endpoint;
    InfluxDB connection;

    public InfluxDbProducer(InfluxDbEndpoint endpoint) {
        super(endpoint);
        if (endpoint == null) {
            throw new IllegalArgumentException("Can't create a producer when the endpoint is null");
        }

        if (endpoint.getInfluxDB() == null) {
            throw new IllegalArgumentException("Can't create a producer when the database connection is null");
        }

        this.connection = endpoint.getInfluxDB();
        this.endpoint = endpoint;
    }

    /**
     * Processes the message exchange
     *
     * @param exchange the message exchange
     * @throws Exception if an internal processing error has occurred.
     */
    @Override
    public void process(Exchange exchange) throws Exception {

        String dataBaseName = calculateDatabaseName(exchange);
        String retentionPolicy = calculateRetentionPolicy(exchange);
        if (!endpoint.isBatch()) {
            Point p = exchange.getIn().getMandatoryBody(Point.class);

            try {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Writing point {}", p.lineProtocol());
                }

                connection.write(dataBaseName, retentionPolicy, p);
            } catch (Exception ex) {
                exchange.setException(new CamelInfluxDbException(ex));
            }
        } else {
            BatchPoints batchPoints = exchange.getIn().getMandatoryBody(BatchPoints.class);

            try {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Writing BatchPoints {}", batchPoints.lineProtocol());
                }

                connection.write(batchPoints);
            } catch (Exception ex) {
                exchange.setException(new CamelInfluxDbException(ex));
            }
        }
    }

    private String calculateRetentionPolicy(Exchange exchange) {
        String retentionPolicy = exchange.getIn().getHeader(InfluxDbConstants.RETENTION_POLICY_HEADER, String.class);

        if (retentionPolicy != null) {
            return retentionPolicy;
        }

        return endpoint.getRetentionPolicy();
    }

    private String calculateDatabaseName(Exchange exchange) {
        String dbName = exchange.getIn().getHeader(InfluxDbConstants.DBNAME_HEADER, String.class);

        if (dbName != null) {
            return dbName;
        }

        return endpoint.getDatabaseName();
    }

}
