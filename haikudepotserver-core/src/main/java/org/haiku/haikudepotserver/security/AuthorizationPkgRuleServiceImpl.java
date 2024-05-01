/*
 * Copyright 2014-2023, Andrew Lindesay
 * Distributed under the terms of the MIT License.
 */

package org.haiku.haikudepotserver.security;

import com.google.common.base.Preconditions;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.query.ObjectSelect;
import org.haiku.haikudepotserver.dataobjects.Permission;
import org.haiku.haikudepotserver.dataobjects.PermissionUserPkg;
import org.haiku.haikudepotserver.dataobjects.Pkg;
import org.haiku.haikudepotserver.dataobjects.User;
import org.haiku.haikudepotserver.security.model.AuthorizationPkgRule;
import org.haiku.haikudepotserver.security.model.AuthorizationPkgRuleSearchSpecification;
import org.haiku.haikudepotserver.security.model.AuthorizationPkgRuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * <p>This service is designed to orchestrate the authorization rules in the system.</p>
 */

@Service
public class AuthorizationPkgRuleServiceImpl implements AuthorizationPkgRuleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationPkgRuleServiceImpl.class);

    @SuppressWarnings("UnusedParameters")
    private String prepareWhereClause(
            List<Object> parameterAccumulator,
            ObjectContext context,
            AuthorizationPkgRuleSearchSpecification specification) {

        List<String> clauses = new ArrayList<>();

        if (null != specification.getPermissions()) {

            StringBuilder query = new StringBuilder();
            query.append('(');

            for (int i = 0; i < specification.getPermissions().size(); i++) {
                if (0 != i) {
                    query.append(" OR ");
                }

                parameterAccumulator.add(specification.getPermissions().get(i));
                query.append("r.permission=?");
                query.append(Integer.toString(parameterAccumulator.size()));
            }

            query.append(')');

            clauses.add(query.toString());
        }

        if (null != specification.getUser()) {
            parameterAccumulator.add(specification.getUser());
            clauses.add("r.user=?" + parameterAccumulator.size());

            if (!specification.getIncludeInactive()) {
                clauses.add("r.user." + User.ACTIVE.getName() + "=true");
            }
        }

        if (null != specification.getPkg()) {
            parameterAccumulator.add(specification.getPkg());
            clauses.add("r.pkg=?" + parameterAccumulator.size());

            if (!specification.getIncludeInactive()) {
                clauses.add("r.pkg." + Pkg.ACTIVE.getName() + "=true");
            }
        }

        return String.join(" AND ", clauses);
    }

    @Override
    public List<AuthorizationPkgRule> search(
            ObjectContext context,
            AuthorizationPkgRuleSearchSpecification specification) {

        Preconditions.checkArgument(null != context, "the context must be provided");
        Preconditions.checkArgument(null != specification, "the specification must be provided");
        Preconditions.checkArgument(specification.getOffset() >= 0, "the offset must be >= 0");
        Preconditions.checkArgument(specification.getLimit() > 0, "the limit must be > 0");

        // special case.

        if (null != specification.getPermissions() && specification.getPermissions().isEmpty()) {
            return Collections.emptyList();
        }

        StringBuilder queryBuilder = new StringBuilder();
        List<Object> parameterAccumulator = new ArrayList<>();
        String whereClause = prepareWhereClause(parameterAccumulator, context, specification);

        queryBuilder.append("SELECT r FROM ");
        queryBuilder.append(PermissionUserPkg.class.getSimpleName());
        queryBuilder.append(" AS r");

        if (!whereClause.isEmpty()) {
            queryBuilder.append(" WHERE ");
            queryBuilder.append(whereClause);
        }

        queryBuilder.append(" ORDER BY r.createTimestamp DESC");

        EJBQLQuery query = new EJBQLQuery(queryBuilder.toString());

        for (int i = 0; i < parameterAccumulator.size(); i++) {
            query.setParameter(i + 1, parameterAccumulator.get(i));
        }

        query.setFetchLimit(specification.getLimit());
        query.setFetchOffset(specification.getOffset());

        //noinspection unchecked
        return (List<AuthorizationPkgRule>) context.performQuery(query);
    }

    @Override
    public long total(
            ObjectContext context,
            AuthorizationPkgRuleSearchSpecification specification) {

        Preconditions.checkArgument(null != context, "the context must be provided");
        Preconditions.checkArgument(null != specification, "the specification must be provided");

        if (null != specification.getPermissions() && specification.getPermissions().isEmpty()) {
            return 0;
        }

        StringBuilder queryBuilder = new StringBuilder();
        List<Object> parameterAccumulator = new ArrayList<>();
        String whereClause = prepareWhereClause(parameterAccumulator, context, specification);

        queryBuilder.append("SELECT COUNT(r) FROM ");
        queryBuilder.append(PermissionUserPkg.class.getSimpleName());
        queryBuilder.append(" AS r");

        if (!whereClause.isEmpty()) {
            queryBuilder.append(" WHERE ");
            queryBuilder.append(whereClause);
        }

        EJBQLQuery query = new EJBQLQuery(queryBuilder.toString());

        for (int i = 0; i < parameterAccumulator.size(); i++) {
            query.setParameter(i + 1, parameterAccumulator.get(i));
        }

        @SuppressWarnings("unchecked") List<Number> result = context.performQuery(query);

        if (result.size() == 1) {
            return result.getFirst().longValue();
        }
        throw new IllegalStateException("expected 1 row from count query, but got " + result.size());
    }

    @Override
    public boolean wouldConflict(
            ObjectContext context,
            User user,
            Permission permission,
            Pkg pkg) {

        Preconditions.checkArgument(null != context, "the context must be provided");
        Preconditions.checkArgument(null != permission, "the permission must be provided");
        Preconditions.checkArgument(null != user, "the user must be provided");

        Expression baseE = PermissionUserPkg.USER.eq(user).andExp(PermissionUserPkg.PERMISSION.eq(permission));

        if (ObjectSelect.query(PermissionUserPkg.class).where(baseE)
                .and(PermissionUserPkg.PKG.isNull())
                .count()
                .selectFirst(context) > 0) {
            return true;
        }

        return ObjectSelect.query(PermissionUserPkg.class).where(baseE)
                .and(PermissionUserPkg.PKG.eq(pkg))
                .count()
                .selectFirst(context) > 0;
    }

    @Override
    public AuthorizationPkgRule create(
            ObjectContext context,
            User user,
            Permission permission,
            Pkg pkg) {

        Preconditions.checkArgument(null != context, "the context must be provided");
        Preconditions.checkArgument(null != permission, "the permission must be provided");
        Preconditions.checkArgument(null != user, "the user must be provided");

        if (user.getIsRoot()) {
            throw new IllegalStateException("when creating an authorization rule, the rule is not able to be applied to a root user");
        }

        PermissionUserPkg rule = context.newObject(PermissionUserPkg.class);
        rule.setPermission(permission);
        user.addToManyTarget(User.PERMISSION_USER_PKGS.getName(), rule, true);
        rule.setPkg(pkg);
        LOGGER.info("did create permission user repository; {},{},{}", permission, user, pkg);

        return rule;
    }

    @Override
    public void remove(
            ObjectContext context,
            User user,
            Permission permission,
            Pkg pkg) {

        Preconditions.checkArgument(null != context, "the context must be provided");
        Preconditions.checkArgument(null != permission, "the permission must be provided");
        Preconditions.checkArgument(null != user, "the user must be provided");

        Optional<PermissionUserPkg> permissionUserPkgOptional = PermissionUserPkg.getByPermissionUserAndPkg(context, permission, user, pkg);

        if (permissionUserPkgOptional.isPresent()) {
            context.deleteObjects(permissionUserPkgOptional.get());
            LOGGER.info("did remove permission user package; {},{},{}", permission, user, pkg);
        } else {
            LOGGER.info("no permission user package already existed to remove; {},{},{}", permission, user, pkg);
        }

    }

}
