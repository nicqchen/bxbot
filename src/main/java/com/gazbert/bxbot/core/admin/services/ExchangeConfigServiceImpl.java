/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Gareth Jon Lynch
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.gazbert.bxbot.core.admin.services;

import com.gazbert.bxbot.core.config.ConfigurationManager;
import com.gazbert.bxbot.core.config.exchange.ExchangeConfig;
import com.gazbert.bxbot.core.config.exchange.NetworkConfig;
import com.gazbert.bxbot.core.config.exchange.OtherConfig;
import com.gazbert.bxbot.core.config.exchange.generated.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.gazbert.bxbot.core.config.exchange.ExchangeConfig.EXCHANGE_CONFIG_XML_FILENAME;
import static com.gazbert.bxbot.core.config.exchange.ExchangeConfig.EXCHANGE_CONFIG_XSD_FILENAME;

/**
 * Implementation of the Exchange config service.
 *
 * @author gazbert
 * @since 20/07/2016
 */
@Service("exchangeConfigService")
@Transactional
public class ExchangeConfigServiceImpl implements ExchangeConfigService {

    private static final Logger LOG = LogManager.getLogger();

    @Override
    public ExchangeConfig getConfig() {
        final ExchangeType internalEngineConfig = ConfigurationManager.loadConfig(ExchangeType.class,
                EXCHANGE_CONFIG_XML_FILENAME, EXCHANGE_CONFIG_XSD_FILENAME);
        return adaptInternalToExternalConfig(internalEngineConfig);
    }

    @Override
    public void updateConfig(ExchangeConfig config) {

        LOG.info(() -> "About to update: " + config);

        final ExchangeType internalExchangeConfig = adaptExternalToInternalConfig(config);
        ConfigurationManager.saveConfig(ExchangeType.class, internalExchangeConfig, EXCHANGE_CONFIG_XML_FILENAME);
    }

    // ------------------------------------------------------------------------------------------------
    // Adapter methods
    // ------------------------------------------------------------------------------------------------

    private static ExchangeConfig adaptInternalToExternalConfig(ExchangeType internalExchangeConfig) {

        // We don't expose AuthenticationConfig in the service - security risk

        final NetworkConfig networkConfig = new NetworkConfig();
        networkConfig.setConnectionTimeout(internalExchangeConfig.getNetworkConfig().getConnectionTimeout());
        networkConfig.setNonFatalErrorCodes(internalExchangeConfig.getNetworkConfig().getNonFatalErrorCodes().getCodes());
        networkConfig.setNonFatalErrorMessages(internalExchangeConfig.getNetworkConfig().getNonFatalErrorMessages().getMessages());

        final OtherConfig otherConfig = new OtherConfig();
        internalExchangeConfig.getOtherConfig().getConfigItems()
                .forEach(item -> otherConfig.addItem(item.getName(), item.getValue()));

        final ExchangeConfig exchangeConfig = new ExchangeConfig();
        exchangeConfig.setExchangeName(internalExchangeConfig.getName());
        exchangeConfig.setExchangeAdapter(internalExchangeConfig.getAdapter());
        exchangeConfig.setNetworkConfig(networkConfig);
        exchangeConfig.setOtherConfig(otherConfig);
        return exchangeConfig;
    }

    private static ExchangeType adaptExternalToInternalConfig(ExchangeConfig externalExchangeConfig) {

        final NonFatalErrorCodesType nonFatalErrorCodes = new NonFatalErrorCodesType();
        nonFatalErrorCodes.getCodes().addAll(externalExchangeConfig.getNetworkConfig().getNonFatalErrorCodes());
        final NonFatalErrorMessagesType nonFatalErrorMessages = new NonFatalErrorMessagesType();
        nonFatalErrorMessages.getMessages().addAll(externalExchangeConfig.getNetworkConfig().getNonFatalErrorMessages());
        final NetworkConfigType networkConfig = new NetworkConfigType();
        networkConfig.setConnectionTimeout(externalExchangeConfig.getNetworkConfig().getConnectionTimeout());
        networkConfig.setNonFatalErrorCodes(nonFatalErrorCodes);
        networkConfig.setNonFatalErrorMessages(nonFatalErrorMessages);

        final OtherConfigType otherConfig = new OtherConfigType();
        externalExchangeConfig.getOtherConfig().getItems().entrySet()
                .forEach(item -> {
                    final ConfigItemType configItem = new ConfigItemType();
                    configItem.setName(item.getKey());
                    configItem.setValue(item.getValue());
                    otherConfig.getConfigItems().add(configItem);
                });

        final ExchangeType exchangeConfig = new ExchangeType();
        exchangeConfig.setName(externalExchangeConfig.getExchangeName());
        exchangeConfig.setAdapter(externalExchangeConfig.getExchangeAdapter());
        exchangeConfig.setNetworkConfig(networkConfig);
        exchangeConfig.setOtherConfig(otherConfig);

        // We don't accept AuthenticationConfig in the service - security risk
        // We load the existing auth config and merge it in with the updated stuff...
        final ExchangeType existingExchangeConfig = ConfigurationManager.loadConfig(ExchangeType.class,
                EXCHANGE_CONFIG_XML_FILENAME, EXCHANGE_CONFIG_XSD_FILENAME);
        exchangeConfig.setAuthenticationConfig(existingExchangeConfig.getAuthenticationConfig());

        return exchangeConfig;
    }
}
