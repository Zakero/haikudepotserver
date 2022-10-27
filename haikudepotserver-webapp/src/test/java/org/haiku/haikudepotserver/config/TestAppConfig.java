/*
 * Copyright 2018, Andrew Lindesay
 * Distributed under the terms of the MIT License.
 */

package org.haiku.haikudepotserver.config;

import org.springframework.context.annotation.Import;

@Import(value = { TestConfig.class })
public class TestAppConfig {
}
