/*
 * Copyright 2018-2025, Andrew Lindesay
 * Distributed under the terms of the MIT License.
 */

package org.haiku.haikudepotserver.captcha;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.query.MappedExec;
import org.haiku.haikudepotserver.captcha.model.CaptchaRepository;
import org.haiku.haikudepotserver.dataobjects.Response;
import org.haiku.haikudepotserver.dataobjects.auto._Captcha;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * <p>This object stores the captchas in a database for later retrieval.  It uses the Cayenne object-relational
 * system to access the database objects.</p>
 */

public class DatabaseCaptchaRepository implements CaptchaRepository {

    protected final static Logger LOGGER = LoggerFactory.getLogger(DatabaseCaptchaRepository.class);

    private final ServerRuntime serverRuntime;

    private final Long expirySeconds;

    public DatabaseCaptchaRepository(
            ServerRuntime serverRuntime,
            Long expirySeconds) {
        this.serverRuntime = Preconditions.checkNotNull(serverRuntime);
        this.expirySeconds = Preconditions.checkNotNull(expirySeconds);
    }

    public ServerRuntime getServerRuntime() {
        return serverRuntime;
    }

    public void init() {
        purgeExpired();
    }

    @Override
    public void purgeExpired() {
        Preconditions.checkNotNull(serverRuntime);
        ObjectContext context = serverRuntime.newContext();
        Timestamp expiryDate = new Timestamp(System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(expirySeconds));
        MappedExec query = MappedExec.query(_Captcha.DELETE_EXPIRED_RESPONSES_QUERYNAME).params(Map.of("expiryTimestamp", expiryDate));
        query.update(context);
    }

    @Override
    public boolean delete(String token) {
        Preconditions.checkState(!Strings.isNullOrEmpty(token));
        Preconditions.checkNotNull(serverRuntime);

        ObjectContext objectContext = serverRuntime.newContext();

        Optional<Response> responseOptional = Response.getByToken(objectContext, token);

        if(responseOptional.isPresent()) {
            objectContext.deleteObjects(responseOptional.get());
            objectContext.commitChanges();
            LOGGER.info("did delete captcha response with token; {}", token);
            return true;
        }

        return false;
    }

    @Override
    public String get(String token) {
        Preconditions.checkState(!Strings.isNullOrEmpty(token));
        Preconditions.checkNotNull(serverRuntime);

        ObjectContext objectContext = serverRuntime.newContext();

        Optional<Response> responseOptional = Response.getByToken(objectContext, token);

        if(responseOptional.isPresent()) {
            String result = responseOptional.get().getResponse();
            delete(token);
            return result;
        }

        return null;
    }

    @Override
    public void store(String token, String response) {
        Preconditions.checkState(!Strings.isNullOrEmpty(token));
        Preconditions.checkState(!Strings.isNullOrEmpty(response));
        Preconditions.checkNotNull(serverRuntime);

        ObjectContext objectContext = serverRuntime.newContext();

        Response r = objectContext.newObject(Response.class);

        r.setToken(token);
        r.setResponse(response);
        r.setCreateTimestamp(new Date());

        objectContext.commitChanges();

        LOGGER.info("stored captcha response with token {}", token);
    }

}
