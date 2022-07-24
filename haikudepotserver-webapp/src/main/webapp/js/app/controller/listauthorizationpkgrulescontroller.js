/*
 * Copyright 2014-2022, Andrew Lindesay
 * Distributed under the terms of the MIT License.
 */

angular.module('haikudepotserver').controller(
    'ListAuthorizationPkgRulesController',
    [
        '$scope','$log','$location',
        'remoteProcedureCall','constants','errorHandling','userState','breadcrumbs','breadcrumbFactory',
        'messageSource',
        function(
            $scope,$log,$location,
            remoteProcedureCall,constants,errorHandling,userState,breadcrumbs,breadcrumbFactory,
            messageSource) {

            var PAGESIZE = 15;

            // used for the search.
            var KEY_OFFSET = 'o';
            var KEY_USERNICKNAME = 'un';
            var KEY_PKGNAME = 'pn';

            var amFetchingRules = false;

            // this gets set to the rule that should be deleted and a modal will appear
            // based off that.

            $scope.ruleToDelete = undefined;

            $scope.rules = {
                items: undefined,
                offset: $location.search()[KEY_OFFSET] ? parseInt($location.search()[KEY_OFFSET],10) : 0,
                max: PAGESIZE,
                total: undefined
            };

            $scope.criteria = {
                userNickname : $location.search()[KEY_USERNICKNAME] ? $location.search()[KEY_USERNICKNAME] : '',
                userNicknameNotFound : false,
                pkgName : $location.search()[KEY_PKGNAME] ? $location.search()[KEY_PKGNAME] : '',
                pkgNameNotFound : false
            };

            function resetNotFounds() {
                $scope.criteria.userNicknameNotFound = false;
                $scope.criteria.pkgNameNotFound = false;
            }

            $scope.userNicknameDidChange = function() {
                resetNotFounds();
            };

            $scope.pkgNameDidChange = function() {
                resetNotFounds();
            };

            $scope.shouldSpin = function() {
                return amFetchingRules;
            };

            function refetchRules() {

                amFetchingRules = true;

                // copy all of the search specifications into the search parameters of the route.

                $location.search(KEY_OFFSET, $scope.rules.offset ? ''+$scope.rules.offset : '0');
                $location.search(KEY_PKGNAME, $scope.criteria.pkgName ? $scope.criteria.pkgName : '');
                $location.search(KEY_USERNICKNAME, $scope.criteria.userNickname ? $scope.criteria.userNickname : '');
                breadcrumbs.peek().search = $location.search();

                remoteProcedureCall.call(
                    constants.ENDPOINT_API_V2_AUTHORIZATION,
                    "search-authorization-pkg-rules",
                    {
                        userNickname : $scope.criteria.userNickname,
                        pkgName : $scope.criteria.pkgName,
                        offset : $scope.rules.offset,
                        limit : $scope.rules.max
                    }
                ).then(
                    function (result) {

                        $scope.rules.items = _.map(
                            result.items,
                            function (item) {
                                return {
                                    user : { nickname : item.userNickname },
                                    permission : { code : item.permissionCode, title : '...' },
                                    pkg : item.pkgName ? { name : item.pkgName } : undefined
                                };
                            }
                        );

                        _.each(
                            $scope.rules.items,
                            function (item) {
                                messageSource.get(userState.naturalLanguageCode(),'permission.' + item.permission.code + '.title').then(
                                    function (value) {
                                        item.permission.title = value;
                                    },
                                    function () {
                                        result.title = '???';
                                    }
                                );
                            }
                        );

                        $scope.rules.total = result.total;
                        $log.info('found '+result.items.length+' rules');
                        amFetchingRules = false;

                    },
                    function (err) {

                        $scope.rules.items = [];
                        $scope.rules.total = 0;

                        switch (err.code) {

                            case remoteProcedureCall.errorCodes.OBJECTNOTFOUND:

                                switch (err.data.entityName) {

                                    case 'User':
                                        $scope.criteria.userNicknameNotFound = true;
                                        break;

                                    case 'Pkg':
                                        $scope.criteria.pkgNameNotFound = true;
                                        break;

                                    default:
                                        errorHandling.handleRemoteProcedureCallError(err);
                                        break;

                                }

                                break;

                            default:
                                errorHandling.handleRemoteProcedureCallError(err);
                                break;

                        }

                        amFetchingRules = false;

                    }
                );

            }

            $scope.goSearch = function () {
                $scope.rules.offset = 0;
                refetchRules();
            };

            $scope.goCancelDeleteRule = function () {
                $scope.ruleToDelete = undefined;
            };

            $scope.goConfirmDeleteRule = function () {

                remoteProcedureCall.call(
                    constants.ENDPOINT_API_V2_AUTHORIZATION,
                    "remove-authorization-pkg-rule",
                    {
                        userNickname : $scope.ruleToDelete.user.nickname,
                        pkgName : $scope.ruleToDelete.pkg ? $scope.ruleToDelete.pkg.name : null,
                        permissionCode : $scope.ruleToDelete.permission.code
                    }
                ).then(
                    function () {
                        $scope.ruleToDelete = undefined;
                        $log.info('did delete an authorization pkg rule');
                        $scope.goSearch();
                    },
                    function (err) {
                        errorHandling.handleRemoteProcedureCallError(err);
                    }
                );

            };

            /**
             * <p>Hit when somebody clicks on the cross at the end of the row relating to a rule.</p>
             */

            $scope.goDeleteRule = function (rule) {
                $scope.ruleToDelete = rule;
            };

            // --------------------
            // WATCHES + EVENTS

            $scope.$watch('rules.offset', function () {
                refetchRules();
            });

        }
    ]
);
