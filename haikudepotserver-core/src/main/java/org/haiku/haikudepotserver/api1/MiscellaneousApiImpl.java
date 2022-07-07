/*
 * Copyright 2018-2022, Andrew Lindesay
 * Distributed under the terms of the MIT License.
 */

package org.haiku.haikudepotserver.api1;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.commons.lang3.StringUtils;
import org.haiku.haikudepotserver.api1.model.miscellaneous.*;
import org.haiku.haikudepotserver.api1.support.ObjectNotFoundException;
import org.haiku.haikudepotserver.dataobjects.*;
import org.haiku.haikudepotserver.feed.model.FeedService;
import org.haiku.haikudepotserver.feed.model.FeedSpecification;
import org.haiku.haikudepotserver.naturallanguage.model.NaturalLanguageService;
import org.haiku.haikudepotserver.support.ContributorsService;
import org.haiku.haikudepotserver.support.RuntimeInformationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component("miscellaneousApiImplV1")
@AutoJsonRpcServiceImpl(additionalPaths = "/api/v1/miscellaneous") // TODO; legacy path - remove
public class MiscellaneousApiImpl extends AbstractApiImpl implements MiscellaneousApi {

    protected static Logger LOGGER = LoggerFactory.getLogger(MiscellaneousApiImpl.class);

    private final ServerRuntime serverRuntime;
    private final RuntimeInformationService runtimeInformationService;
    private final FeedService feedService;
    private final ContributorsService contributorsService;
    private final MessageSource messageSource;
    private final NaturalLanguageService naturalLanguageService;
    private final Boolean isProduction;
    private final String architectureDefaultCode;
    private final String repositoryDefaultCode;

    @Autowired
    public MiscellaneousApiImpl(
            ServerRuntime serverRuntime,
            RuntimeInformationService runtimeInformationService,
            FeedService feedService,
            ContributorsService contributorsService,
            MessageSource messageSource,
            NaturalLanguageService naturalLanguageService,
            @Value("${deployment.isproduction:false}") Boolean isProduction,
            @Value("${architecture.default.code}") String architectureDefaultCode,
            @Value("${repository.default.code}") String repositoryDefaultCode) {
        this.serverRuntime = Preconditions.checkNotNull(serverRuntime);
        this.runtimeInformationService = Preconditions.checkNotNull(runtimeInformationService);
        this.feedService = Preconditions.checkNotNull(feedService);
        this.contributorsService = Preconditions.checkNotNull(contributorsService);
        this.messageSource = Preconditions.checkNotNull(messageSource);
        this.naturalLanguageService = Preconditions.checkNotNull(naturalLanguageService);
        this.isProduction = Preconditions.checkNotNull(isProduction);
        this.architectureDefaultCode = Preconditions.checkNotNull(architectureDefaultCode);
        this.repositoryDefaultCode = Preconditions.checkNotNull(repositoryDefaultCode);
    }

    @Override
    public GetAllPkgCategoriesResult getAllPkgCategories(GetAllPkgCategoriesRequest getAllPkgCategoriesRequest) {
        Preconditions.checkNotNull(getAllPkgCategoriesRequest);
        final ObjectContext context = serverRuntime.newContext();

        final Optional<NaturalLanguage> naturalLanguageOptional = Optional
                .ofNullable(getAllPkgCategoriesRequest.naturalLanguageCode)
                .filter(StringUtils::isNotBlank)
                .flatMap(c -> NaturalLanguage.tryGetByCode(context, c));

        return new GetAllPkgCategoriesResult(
                PkgCategory.getAll(context).stream().map(pc -> {
                    if(naturalLanguageOptional.isPresent()) {
                        return new GetAllPkgCategoriesResult.PkgCategory(
                                pc.getCode(),
                                messageSource.getMessage(
                                        pc.getTitleKey(),
                                        null, // params
                                        naturalLanguageOptional.get().toLocale()));
                    }
                    else {
                        return new GetAllPkgCategoriesResult.PkgCategory(
                                pc.getCode(),
                                pc.getName());
                    }
                }).collect(Collectors.toList())
        );
    }

    @Override
    public GetAllNaturalLanguagesResult getAllNaturalLanguages(GetAllNaturalLanguagesRequest getAllNaturalLanguagesRequest) {
        Preconditions.checkNotNull(getAllNaturalLanguagesRequest);
        final ObjectContext context = serverRuntime.newContext();

        final Optional<NaturalLanguage> naturalLanguageOptional = Optional
                .ofNullable(getAllNaturalLanguagesRequest.naturalLanguageCode)
                .filter(StringUtils::isNotBlank)
                .flatMap(c -> NaturalLanguage.tryGetByCode(context, c));

        return new GetAllNaturalLanguagesResult(
                NaturalLanguage.getAll(context).stream().map(nl -> {
                            if(naturalLanguageOptional.isPresent()) {
                                return new GetAllNaturalLanguagesResult.NaturalLanguage(
                                        nl.getCode(),
                                        messageSource.getMessage(
                                                nl.getTitleKey(),
                                                null, // params
                                                naturalLanguageOptional.get().toLocale()),
                                        nl.getIsPopular(),
                                        naturalLanguageService.hasData(nl.getCode()),
                                        naturalLanguageService.hasLocalizationMessages(nl.getCode()));
                            }
                            else {
                                return new GetAllNaturalLanguagesResult.NaturalLanguage(
                                        nl.getCode(),
                                        nl.getName(),
                                        nl.getIsPopular(),
                                        naturalLanguageService.hasData(nl.getCode()),
                                        naturalLanguageService.hasLocalizationMessages(nl.getCode()));
                            }
                        }
                ).collect(Collectors.toList())
        );

    }

    @Override
    public RaiseExceptionResult raiseException(RaiseExceptionRequest raiseExceptionRequest) {

        final ObjectContext context = serverRuntime.newContext();
        Optional<User> authUserOptional = tryObtainAuthenticatedUser(context);

        if(authUserOptional.isPresent() && authUserOptional.get().getIsRoot()) {
            throw new RuntimeException("test exception");
        }

        LOGGER.warn("attempt to raise a test exception without being authenticated as root");

        return new RaiseExceptionResult();
    }

    @Override
    public GetRuntimeInformationResult getRuntimeInformation(GetRuntimeInformationRequest getRuntimeInformationRequest) {

        final ObjectContext context = serverRuntime.newContext();
        Optional<User> authUserOptional = tryObtainAuthenticatedUser(context);

        GetRuntimeInformationResult result = new GetRuntimeInformationResult();
        result.projectVersion = runtimeInformationService.getProjectVersion();
        result.isProduction = isProduction;

        if(authUserOptional.isPresent() && authUserOptional.get().getIsRoot()) {
            result.javaVersion = runtimeInformationService.getJavaVersion();
            result.startTimestamp = runtimeInformationService.getStartTimestamp();
        }

        GetRuntimeInformationResult.Defaults defaults = new GetRuntimeInformationResult.Defaults();
        defaults.architectureCode = architectureDefaultCode;
        defaults.repositoryCode = repositoryDefaultCode;
        result.defaults = defaults;

        return result;
    }

    @Override
    public GetAllArchitecturesResult getAllArchitectures(GetAllArchitecturesRequest getAllArchitecturesRequest) {
        Preconditions.checkNotNull(getAllArchitecturesRequest);
        GetAllArchitecturesResult result = new GetAllArchitecturesResult();
        result.architectures =
                Architecture.getAll(serverRuntime.newContext())
                        .stream()
                        .filter(a -> !a.getCode().equals(Architecture.CODE_SOURCE) && !a.getCode().equals(Architecture.CODE_ANY))
                        .map(a -> new GetAllArchitecturesResult.Architecture(a.getCode()))
                        .collect(Collectors.toList());

        return result;
    }

    @Override
    public GetAllMessagesResult getAllMessages(GetAllMessagesRequest getAllMessagesRequest) {
        Preconditions.checkNotNull(getAllMessagesRequest);
        Preconditions.checkNotNull(getAllMessagesRequest.naturalLanguageCode);

        ObjectContext context = serverRuntime.newContext();

        NaturalLanguage naturalLanguage = Optional
                .ofNullable(getAllMessagesRequest.naturalLanguageCode)
                .filter(StringUtils::isNotBlank)
                .flatMap(c -> NaturalLanguage.tryGetByCode(context, c))
                .orElseThrow(() -> new ObjectNotFoundException(
                        NaturalLanguage.class.getSimpleName(),
                        getAllMessagesRequest.naturalLanguageCode));

        Properties allLocalizationMessages = naturalLanguageService.getAllLocalizationMessages(naturalLanguage.getCode());

        GetAllMessagesResult getAllMessagesResult = new GetAllMessagesResult();
        getAllMessagesResult.messages = new HashMap<>();

        for(Object key : allLocalizationMessages.keySet()) {
            getAllMessagesResult.messages.put(key.toString(), allLocalizationMessages.get(key).toString());
        }

        return getAllMessagesResult;
    }

    @Override
    public GetAllUserRatingStabilitiesResult getAllUserRatingStabilities(GetAllUserRatingStabilitiesRequest getAllUserRatingStabilitiesRequest) {
        Preconditions.checkNotNull(getAllUserRatingStabilitiesRequest);
        final ObjectContext context = serverRuntime.newContext();

        final Optional<NaturalLanguage> naturalLanguageOptional = Optional
                .ofNullable(getAllUserRatingStabilitiesRequest.naturalLanguageCode)
                .filter(StringUtils::isNotBlank)
                .flatMap(c -> NaturalLanguage.tryGetByCode(context, c));

        return new GetAllUserRatingStabilitiesResult(
                UserRatingStability.getAll(context)
                        .stream()
                        .map(urs -> {
                            if(naturalLanguageOptional.isPresent()) {
                                return new GetAllUserRatingStabilitiesResult.UserRatingStability(
                                        urs.getCode(),
                                        messageSource.getMessage(
                                                urs.getTitleKey(),
                                                null, // params
                                                naturalLanguageOptional.get().toLocale()),
                                        urs.getOrdering());
                            }

                            return new GetAllUserRatingStabilitiesResult.UserRatingStability(
                                    urs.getCode(),
                                    urs.getName(),
                                    urs.getOrdering());
                        })
                        .collect(Collectors.toList())
        );
    }

    @Override
    public GetAllProminencesResult getAllProminences(GetAllProminencesRequest request) {
        Preconditions.checkNotNull(request);
        final ObjectContext context = serverRuntime.newContext();

        return new GetAllProminencesResult(
                Prominence.getAll(context)
                .stream()
                .map(p -> new GetAllProminencesResult.Prominence(p.getOrdering(),p.getName()))
                .collect(Collectors.toList())
        );
    }

    @Override
    public GetAllCountriesResult getAllCountries(GetAllCountriesRequest request) {
        Preconditions.checkNotNull(request);
        final ObjectContext context = serverRuntime.newContext();

        return new GetAllCountriesResult(
                Country.getAll(context)
                .stream()
                .map(c -> new GetAllCountriesResult.Country(c.getCode(), c.getName()))
                .collect(Collectors.toList()));
    }

    @Override
    public GenerateFeedUrlResult generateFeedUrl(final GenerateFeedUrlRequest request) {
        Preconditions.checkNotNull(request);

        final ObjectContext context = serverRuntime.newContext();
        FeedSpecification specification = new FeedSpecification();
        specification.setFeedType(FeedSpecification.FeedType.ATOM);
        specification.setLimit(request.limit);

        if(null!=request.supplierTypes) {
            specification.setSupplierTypes(
                    request.supplierTypes
                    .stream()
                    .map(st -> FeedSpecification.SupplierType.valueOf(st.name()))
                    .collect(Collectors.toList())
            );
        }

        if(null!=request.naturalLanguageCode) {
            specification.setNaturalLanguageCode(getNaturalLanguage(context, request.naturalLanguageCode).getCode());
        }

        if(null!=request.pkgNames) {
            List<String> checkedPkgNames = new ArrayList<>();

            for (String pkgName : request.pkgNames) {
                Optional<Pkg> pkgOptional = Pkg.tryGetByName(context, pkgName);

                if (pkgOptional.isEmpty()) {
                    throw new ObjectNotFoundException(Pkg.class.getSimpleName(), pkgName);
                }

                checkedPkgNames.add(pkgOptional.get().getName());
            }

            specification.setPkgNames(checkedPkgNames);
        }

        GenerateFeedUrlResult result = new GenerateFeedUrlResult();
        result.url = feedService.generateUrl(specification);
        return result;
    }

    @Override
    public GetAllContributorsResult getAllContributors(GetAllContributorsRequest request) {
        Preconditions.checkArgument(null != request);
        return new GetAllContributorsResult(
                contributorsService.getConstributors().stream()
                .map(c -> new GetAllContributorsResult.Contributor(
                        GetAllContributorsResult.Contributor.Type.valueOf(c.getType().name()),
                        c.getName(),
                        c.getNaturalLanguageCode()))
                .collect(Collectors.toUnmodifiableList()));
    }

}
