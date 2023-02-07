/*
 * Copyright 2018-2023, Andrew Lindesay
 * Distributed under the terms of the MIT License.
 */

package org.haiku.haikudepotserver.graphics.hvif;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;

/**
 * <p>Produces an HVIF rendering service depending on the configuration of the application server.</p>
 */

public class HvifRenderingServiceFactory implements FactoryBean<HvifRenderingService> {

    protected static Logger LOGGER = LoggerFactory.getLogger(HvifRenderingServiceFactory.class);

    private final String hvif2pngPath;

    public HvifRenderingServiceFactory(
            @Value("${hds.hvif2png.path:}") String hvif2pngPath
    ) {
        this.hvif2pngPath = hvif2pngPath;
    }

    @Override
    public HvifRenderingService getObject() throws Exception {
        if(!Strings.isNullOrEmpty(hvif2pngPath)) {
            LOGGER.info("will use hvif2png rendering; {}", hvif2pngPath);
            return new Hvif2PngHvifRenderingServiceImpl(hvif2pngPath);
        }

        LOGGER.info("will fallback hvif rendering that produces generic images");
        return new FallbackHvifRenderingServiceImpl();
    }

    @Override
    public Class<?> getObjectType() {
        return HvifRenderingService.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
