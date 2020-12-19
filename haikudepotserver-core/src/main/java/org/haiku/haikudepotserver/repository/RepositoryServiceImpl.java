/*
 * Copyright 2018-2020, Andrew Lindesay
 * Distributed under the terms of the MIT License.
 */

package org.haiku.haikudepotserver.repository;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.commons.lang3.StringUtils;
import org.haiku.haikudepotserver.dataobjects.HaikuDepot;
import org.haiku.haikudepotserver.dataobjects.Pkg;
import org.haiku.haikudepotserver.dataobjects.PkgVersion;
import org.haiku.haikudepotserver.dataobjects.Repository;
import org.haiku.haikudepotserver.repository.model.RepositorySearchSpecification;
import org.haiku.haikudepotserver.repository.model.RepositoryService;
import org.haiku.haikudepotserver.support.DateTimeHelper;
import org.haiku.haikudepotserver.support.LikeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * <p>This service provides non-trivial operations and processes around repositories.</p>
 */

@Service
public class RepositoryServiceImpl implements RepositoryService {

    protected static Logger LOGGER = LoggerFactory.getLogger(RepositoryServiceImpl.class);

    private final PasswordEncoder passwordEncoder;

    public RepositoryServiceImpl(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = Preconditions.checkNotNull(passwordEncoder);
    }

    // ------------------------------
    // HELPERS

    @Override
    public Date getLastRepositoryModifyTimestampSecondAccuracy(ObjectContext context) {
        EJBQLQuery query = new EJBQLQuery(String.join(" ",
                "SELECT",
                "MAX(r." + Repository.MODIFY_TIMESTAMP.getName() + ")",
                "FROM",
                Repository.class.getSimpleName(),
                "r WHERE r.active = true"));

        query.setCacheGroup(HaikuDepot.CacheGroup.REPOSITORY.name());

        List<Object> result = context.performQuery(query);

        switch (result.size()) {
            case 0: return new Date(0);
            case 1: return DateTimeHelper.secondAccuracyDate((Date) result.get(0));
            default: throw new IllegalStateException("more than one row returned for a max aggregate.");
        }
    }

    @Override
    public List<Repository> getRepositoriesForPkg(
            ObjectContext context,
            Pkg pkg) {
        Preconditions.checkArgument(null!=context);
        Preconditions.checkArgument(null!=pkg);

        StringBuilder ejbql = new StringBuilder();

        ejbql.append("SELECT DISTINCT r FROM\n");
        ejbql.append(Repository.class.getSimpleName());
        ejbql.append(" r WHERE r.active = true AND EXISTS (SELECT pv FROM \n");
        ejbql.append(PkgVersion.class.getSimpleName());
        ejbql.append(" pv WHERE pv.repositorySource.repository=r");
        ejbql.append(" AND pv.pkg=:pkg");
        ejbql.append(")");

        EJBQLQuery query = new EJBQLQuery(ejbql.toString());
        query.setParameter("pkg", pkg);

        return (List<Repository>) context.performQuery(query);

    }

    /**
     * <p>If we are searching for "http" URLs then we may as well search for "https" URLs as well.</p>
     */

    private List<String> toRepositorySourceUrlVariants(String url) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(url), "the url must be supplied");

        if (url.startsWith("http:")) {
            return ImmutableList.of(url, "https:" + url.substring(5));
        }

        if (url.startsWith("https:")) {
            return ImmutableList.of(url, "http:" + url.substring(6));
        }

        return Collections.singletonList(url);
    }

    // ------------------------------
    // SEARCH

    private String prepareWhereClause(
            List<Object> parameterAccumulator,
            ObjectContext context,
            RepositorySearchSpecification search) {

        Preconditions.checkNotNull(search);
        Preconditions.checkNotNull(context);

        List<String> whereExpressions = new ArrayList<>();

        if (null != search.getExpression()) {
            switch (search.getExpressionType()) {

                case CONTAINS:
                    parameterAccumulator.add("%" + LikeHelper.ESCAPER.escape(search.getExpression()) + "%");
                    whereExpressions.add("LOWER(r." + Repository.CODE.getName() + ") LIKE ?" + parameterAccumulator.size() + " ESCAPE '|'");
                    break;

                default:
                    throw new IllegalStateException("unsupported expression type; "+search.getExpressionType());

            }
        }

        if (!search.getIncludeInactive()) {
            whereExpressions.add("r." + Repository.ACTIVE.getName() + " = true");
        }

        return String.join(" AND ", whereExpressions);
    }

    @Override
    public List<Repository> search(ObjectContext context, RepositorySearchSpecification search) {
        Preconditions.checkNotNull(search);
        Preconditions.checkNotNull(context);
        Preconditions.checkState(search.getOffset() >= 0);
        Preconditions.checkState(search.getLimit() > 0);

        List<Object> parameterAccumulator = new ArrayList<>();

        StringBuilder ejbql = new StringBuilder();
        ejbql.append("SELECT r FROM ");
        ejbql.append(Repository.class.getSimpleName());
        ejbql.append(" r ");

        String whereClause = prepareWhereClause(parameterAccumulator, context, search);

        if (!Strings.isNullOrEmpty(whereClause)) {
            ejbql.append(" WHERE ");
            ejbql.append(whereClause);
        }

        ejbql.append(" ORDER BY r.");
        ejbql.append(Repository.CODE.getName());
        ejbql.append(" ASC");

        EJBQLQuery ejbqlQuery = new EJBQLQuery(ejbql.toString());

        for (int i = 0; i < parameterAccumulator.size(); i++) {
            ejbqlQuery.setParameter(i+1, parameterAccumulator.get(i));
        }

        ejbqlQuery.setFetchLimit(search.getLimit());
        ejbqlQuery.setFetchOffset(search.getOffset());

        //noinspection unchecked
        return (List<Repository>) context.performQuery(ejbqlQuery);
    }

    @Override
    public long total(ObjectContext context, RepositorySearchSpecification search) {
        Preconditions.checkNotNull(search);
        Preconditions.checkNotNull(context);

        List<Object> parameters = new ArrayList<>();

        StringBuilder ejbql = new StringBuilder();
        ejbql.append("SELECT COUNT(r) FROM Repository AS r");

        String whereClause = prepareWhereClause(parameters, context, search);

        if (!Strings.isNullOrEmpty(whereClause)) {
            ejbql.append(" WHERE ");
            ejbql.append(whereClause);
        }

        EJBQLQuery ejbQuery = new EJBQLQuery(ejbql.toString());

        for (int i = 0; i < parameters.size(); i++) {
            ejbQuery.setParameter(i + 1, parameters.get(i));
        }

        @SuppressWarnings("unchecked") List<Number> result = context.performQuery(ejbQuery);

        switch (result.size()) {
            case 1:
                return result.get(0).longValue();

            default:
                throw new IllegalStateException("expected 1 row from count query, but got "+result.size());
        }
    }

    public void setPassword(Repository repository, String passwordClear) {
        Preconditions.checkArgument(null != repository, "the repository is required");
        if (StringUtils.isBlank(passwordClear)) {
            repository.setPasswordSalt(null);
            repository.setPasswordHash(null);
        }
        else {
            List<String> parts = Splitter.on(".").splitToList(passwordEncoder.encode(passwordClear));
            if (2 != parts.size()) {
                throw new IllegalStateException("expecting a salt and hash separated by a period symbol");
            }
            repository.setPasswordSalt(parts.get(0));
            repository.setPasswordHash(parts.get(1));
        }
    }

    public boolean matchPassword(Repository repository, String passwordClear) {
        return StringUtils.isNotBlank(repository.getPasswordSalt())
                && StringUtils.isNotBlank(repository.getPasswordHash())
                && passwordEncoder.matches(
                passwordClear,
                repository.getPasswordSalt() + "." + repository.getPasswordHash());
    }

}
