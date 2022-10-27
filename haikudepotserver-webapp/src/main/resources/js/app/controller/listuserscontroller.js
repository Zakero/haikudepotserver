/*
 * Copyright 2014-2022, Andrew Lindesay
 * Distributed under the terms of the MIT License.
 */

angular.module('haikudepotserver').controller(
    'ListUsersController',
    [
        '$scope','$log','$location',
        'remoteProcedureCall','constants',
        'breadcrumbs','breadcrumbFactory','errorHandling',
        function(
            $scope,$log,$location,
            remoteProcedureCall,constants,
            breadcrumbs,breadcrumbFactory,errorHandling) {

            breadcrumbs.mergeCompleteStack([
                breadcrumbFactory.createHome(),
                breadcrumbFactory.applyCurrentLocation(breadcrumbFactory.createListUsers())
            ]);

            var PAGESIZE = 15;

            $scope.users = {
                items: undefined,
                offset: 0,
                max: PAGESIZE,
                total: undefined
            };

            function clearUsers() {
                $scope.users.items = undefined;
                $scope.users.total = undefined;
                $scope.users.offset = 0;
            }

            $scope.amShowingInactive = false;
            var amFetchingUsers = false;

            refetchUsersAtFirstPage();

            $scope.shouldSpin = function () {
                return amFetchingUsers;
            };

            $scope.goShowInactive = function () {
                $scope.amShowingInactive = true;
                refetchUsersAtFirstPage();
            };

            // ---- LIST MANAGEMENT

            $scope.goSearch = function () {
                clearUsers();
                refetchUsersAtFirstPage();
            };

            function refetchUsersAtFirstPage() {
                $scope.users.offset = 0;
                refetchUsers();
            }

            function refetchUsers() {

                amFetchingUsers = true;

                remoteProcedureCall.call(
                    constants.ENDPOINT_API_V2_USER,
                    "search-users",
                    {
                        expression : $scope.searchExpression,
                        expressionType : 'CONTAINS',
                        includeInactive : $scope.amShowingInactive,
                        offset : $scope.users.offset,
                        limit : $scope.users.max
                    }
                ).then(
                    function (result) {
                        $scope.users.items = result.items;
                        $scope.users.total = result.total;
                        $log.info('found '+result.items.length+' users');
                        amFetchingUsers = false;
                    },
                    function (err) {
                        errorHandling.handleRemoteProcedureCallError(err);
                    }
                );

            }

            // ---- EVENTS

            $scope.$watch('users.offset', function() {
                refetchUsers();
            });

        }
    ]
);
