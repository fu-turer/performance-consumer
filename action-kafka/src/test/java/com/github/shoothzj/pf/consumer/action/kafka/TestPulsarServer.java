/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.github.shoothzj.pf.consumer.action.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.bookkeeper.conf.ServerConfiguration;
import org.apache.pulsar.PulsarStandalone;
import org.apache.pulsar.PulsarStandaloneBuilder;
import org.apache.pulsar.broker.ServiceConfiguration;
import org.apache.pulsar.zookeeper.LocalBookkeeperEnsemble;
import org.assertj.core.util.Files;

import java.io.File;
import java.util.Optional;

@Slf4j
public class TestPulsarServer {

    private final File zkDir;

    private final File bkDir;

    private final int zkPort;

    private final int bkPort;

    private final int webPort;

    private final int tcpPort;

    private final PulsarStandalone pulsarStandalone;

    public TestPulsarServer() {
        try {
            this.zkPort = TestUtil.getFreePort();
            this.bkPort = TestUtil.getFreePort();
            this.zkDir = Files.newTemporaryFolder();
            this.zkDir.deleteOnExit();
            this.bkDir = Files.newTemporaryFolder();
            this.bkDir.deleteOnExit();
            LocalBookkeeperEnsemble bkEnsemble = new LocalBookkeeperEnsemble(
                    1, zkPort, bkPort, zkDir.toString(),
                    bkDir.toString(), false, "127.0.0.1");
            ServerConfiguration bkConf = new ServerConfiguration();
            bkConf.setJournalRemovePagesFromCache(false);
            log.info("begin to start bookkeeper");
            bkEnsemble.startStandalone(bkConf, false);
            this.webPort = TestUtil.getFreePort();
            this.tcpPort = TestUtil.getFreePort();
            this.pulsarStandalone = PulsarStandaloneBuilder
                    .instance()
                    .withZkPort(zkPort)
                    .withNumOfBk(1)
                    .withOnlyBroker(true)
                    .build();
            ServiceConfiguration standaloneConfig = this.pulsarStandalone.getConfig();
            standaloneConfig.setWebServicePort(Optional.of(webPort));
            standaloneConfig.setBrokerServicePort(Optional.of(tcpPort));
            standaloneConfig.setManagedLedgerDefaultEnsembleSize(1);
            standaloneConfig.setManagedLedgerDefaultWriteQuorum(1);
            standaloneConfig.setManagedLedgerDefaultAckQuorum(1);
            this.pulsarStandalone.setConfig(standaloneConfig);
        } catch (Throwable e) {
            log.error("exception is ", e);
            throw new IllegalStateException("start pulsar standalone failed");
        }
    }

    public void start() throws Exception {
        this.pulsarStandalone.start();
    }

    public int getWebPort() {
        return webPort;
    }

    public int getTcpPort() {
        return tcpPort;
    }

    public void close() throws Exception {
        this.pulsarStandalone.close();
    }

}