/*
 * Copyright 2015, Andrew Lindesay
 * Distributed under the terms of the MIT License.
 */

package org.haikuos.haikudepotserver.support.hvif;

import java.io.IOException;

/**
 * <p>This interface describes a service that is able to take HVIF vector icons and render them into
 * PNG bitmaps.</p>
 */

public interface HvifRenderingService {

    /**
     * <p>This method will render the supplied data into a PNG bitmap image.</p>
     */

    byte[] render(int size, byte[] input) throws IOException;

}
